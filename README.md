# Model-based Testing with Testcontainers and Jqwick

In this demo, we'll explore the Model-based testing technique to perform regression testing on a simple REST
API.

We'll use the [Jqwik](https://jqwik.net) test engine on JUnit5 to run Property and Model-based tests. Additionally, we'll use
[Testcontainers](https://testcontainers.com/getting-started/) to spin up Docker containers with different versions of our application.

## Model Based Testing

Model-based testing (MBT) is a method for testing stateful software by creating a "model" that represents the expected behavior of the system. Instead of manually writing test cases, we'll use a testing tool that:

- Takes a list of possible actions supported by the application,
- Automatically generates test sequences from these actions, targeting potential edge cases,
- Executes these tests on the software and the model, comparing the results.

In our case, the "actions" are simply the endpoints exposed by the application's API. For the demo's code examples, we'll use a basic service with a CRUD REST API that allows us to:

- Find an employee by its unique "Employee Number"
- Update an employee's name
- Get a list of all employees
- Register a new employee

![img.png](images/swagger.png)


## Docker Compose

Let’s assume we need to switch the database from Postgres to MySQL and want to ensure the service’s behavior remains consistent. To test this, we can run both versions of the application, send identical requests to each, and compare the responses. 

We can set up the environment using a Docker Compose that will run two versions of the app: 
- the **model** (`mbt-demo:postgres`): the current _live_ version and our source of truth,
- the **tested** version (`mbt-demo:mysql`): the new _feature branch_ that is under test.


```yaml
services:
  ## MODEL
  app-model:
      image: mbt-demo:postgres
      # ...
      depends_on:
          - postgres
  postgres:
      image: postgres:16-alpine
      # ...
      
  ## TESTED
  app-tested:
    image: mbt-demo:mysql
    # ...
    depends_on:
      - mysql
  mysql:
    image: mysql:8.0
    # ...
```

## Testcontainers

At this point, we could start the application and databases manually for testing, but this would be tedious. Instead, let's use Testcontainers' [`DockerComposeContainer`](https://java.testcontainers.org/modules/docker_compose/) to automate this with our Docker Compose file during the testing phase.

In this example, we'll use Jqwik as our JUnit 5 test runner. Firstly, let's add the Jqwik and Testcontainers dependencies to your _pom.xml_:

```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.9.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik-testcontainers</artifactId>
    <version>0.5.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.16.2</version>
    <scope>test</scope>
</dependency>
```

As a result, we can now instantiate a _DockerComposeContainer_ and pass our test _docker-compose_ file as argument:

```java
@Testcontainers
class ModelBasedTest {

    @Container
    static DockerComposeContainer<?> ENV = new DockerComposeContainer<>(new File("src/test/resources/docker-compose-test.yml"))
       .withExposedService("app-tested", 8080, Wait.forHttp("/api/employees").forStatusCode(200))
       .withExposedService("app-model", 8080, Wait.forHttp("/api/employees").forStatusCode(200));

    // tests
}
```

## Test Http Client

Now let's create a small test utility that will help us execute the HTTP requests against our services

```java
class TestHttpClient {
  ApiResponse<EmployeeDto> get(String employeeNo) { /* ... */ }
  
  ApiResponse<Void> put(String employeeNo, String newName) { /* ... */ }
  
  ApiResponse<List<EmployeeDto>> getByDepartment(String department) { /* ... */ }
  
  ApiResponse<EmployeeDto> post(String employeeNo, String name) { /* ... */ }

    
  record ApiResponse<T>(int statusCode, @Nullable T body) { }
    
  record EmployeeDto(String employeeNo, String name) { }
}
```

Additionally, in the test class we can declare another method that helps us create _TestHttpClient_s for the two services started by the _DockerComposeContainer_:

```java
static TestHttpClient testClient(String service) {
  int port = ENV.getServicePort(service, 8080);
  String url = "http://localhost:%s/api/employees".formatted(port);
  return new TestHttpClient(service, url);
}
```

This will make it easily to create two test clients in our test, one for each version of the service:

```java
TestHttpClient model = testClient("app-model");
TestHttpClient tested = testClient("app-tested");
```

## Jqwik

Jqwik is a property-based testing framework for Java that integrates with JUnit5, automatically generating test cases to validate properties of code across diverse input data. It enhances test coverage and uncovers edge cases by using generators to create varied and random test inputs.

If you're new to Jqwik, you can explore their API in detail by reviewing the [official user guide](https://jqwik.net/docs/current/user-guide.html). While this tutorial won't cover all the specifics of  the API, it's important to know that Jqwik allows us to define a set of actions we want to test.

To begin with, we’ll use Jqwik’s _@Property_ annotation to define a test - instead of the traditional _@Test_:
```java
@Property
void regressionTest() {
  TestHttpClient model = testClient("app-model");
  TestHttpClient tested = testClient("app-tested");
  // ...
}

```

Next, we’ll define the "actions," which are the HTTP calls to our APIs and can also include assertions. 

For instance, the _GetOneEmployeeAction_ will try to fetch a specific employee from both services and compare the responses:

```java
record ModelVsTested(TestHttpClient model, TestHttpClient tested) {}

record GetOneEmployeeAction(String empNo) implements Action<ModelVsTested> {
  @Override
  public ModelVsTested run(ModelVsTested apps) {
    ApiResponse<EmployeeDto> actual = apps.tested.get(empNo);
    ApiResponse<EmployeeDto> expected = apps.model.get(empNo);

    assertThat(actual)
      .satisfies(hasStatusCode(expected.statusCode()))
      .satisfies(hasBody(expected.body()));
    return apps;
  }
}
```

Additionally, we'll need to wrap these *Action*s within _Arbitrary_ objects. We can think of Arbitraries as of objects implementing the factory design pattern, that can generate a wide variety of instances of a type, based on a set of configured rules. 

For instance, the _Arbitrary<String>_ returned by _employeeNos()_ can generate employee numbers by choosing a random department from the configured list and concatenating a number between 0 and 200:
```java
static Arbitrary<String> employeeNos() {
  Arbitrary<String> departments = Arbitraries.of("Frontend", "Backend", "HR", "Creative", "DevOps");
  Arbitrary<Long> ids = Arbitraries.longs().between(1, 200);
  return Combinators.combine(departments, ids).as("%s-%s"::formatted);
}

static Arbitrary<GetOneEmployeeAction> getOneEmployeeAction() {
  return employeeNos().map(GetOneEmployeeAction::new);
}
```
After declaring all our actions, we'll create an _ActionSequence_. We'll also wrap these actions in _Arbitrary_ objects. In simple terms, _Arbitrary_ objects follow the _Factory_ design pattern, allowing us to generate objects and actions with randomized parameters:

```java
@Provide
Arbitrary<ActionSequence<ModelVsTested>> mbtJqwikActions() {
  return Arbitraries.sequences(
    Arbitraries.oneOf(
      MbtJqwikActions.getOneEmployeeAction(),
      MbtJqwikActions.getEmployeesByDepartmentAction(),
      MbtJqwikActions.createEmployeeAction(),
      MbtJqwikActions.updateEmployeeNameAction()
  ));
}

static Arbitrary<Action<ModelVsTested>> getOneEmployeeAction() { /* ... */ }
static Arbitrary<Action<ModelVsTested>> getEmployeesByDepartmentAction() { /* ... */ }
// same for the other actions
```
Now we can write our test and leverage Jqwik to test various sequences of with provided actions. Let's create the _ModelVsTested_ tuple, and use it to execute the sequence of actions against it:

```java
@Property
void regressionTest(@ForAll("mbtJqwikActions") ActionSequence<ModelVsTested> actions) {
  ModelVsTested testVsModel = new ModelVsTested(
    testClient("app-model"),
    testClient("app-tested")
  );
  actions.run(testVsModel);
}
```
 
Let's finally tun the test! This will generate a sequence of thousands of requests trying to find inconsistencies between the model and the tested service:

```
10:31:54.492 [main] INFO -- [tested] POST /api/employees { name=LY, empNo=DevOps-7 }
10:31:54.532 [main] INFO -- [model] POST /api/employees { name=LY, empNo=DevOps-7 }
10:31:54.593 [main] INFO -- [model] GET /api/employees/DevOps-7
10:31:54.604 [main] INFO -- [tested] GET /api/employees/DevOps-7
10:31:54.617 [main] INFO -- [model] GET /api/employees/HR-160
10:31:54.626 [main] INFO -- [model] PUT /api/employeesHR-160?name=TEjDSCeODLK
10:31:54.636 [main] INFO -- [tested] PUT /api/employeesHR-160?name=TEjDSCeODLK
10:31:54.647 [main] INFO -- [model] GET /api/employees/DevOps-9
10:31:54.655 [main] INFO -- [tested] POST /api/employees { name=fcQqVOqZQcK, empNo=DevOps-9 }
10:31:54.677 [main] INFO -- [model] POST /api/employees { name=fcQqVOqZQcK, empNo=DevOps-9 }
10:31:54.690 [main] INFO -- [model] GET /api/employees/DevOps-9
10:31:54.702 [main] INFO -- [tested] GET /api/employees/DevOps-9
10:31:54.712 [main] INFO -- [model] GET /api/employees/HR-101
10:31:54.721 [main] INFO -- [model] PUT /api/employeesHR-101?name=gHv
10:31:54.731 [main] INFO -- [tested] PUT /api/employeesHR-101?name=gHv
10:31:54.740 [main] INFO -- [tested] GET /api/employees
10:31:54.752 [main] INFO -- [model] GET /api/employees
10:31:54.763 [main] INFO -- [tested] GET /api/employees/Frontend-65
10:31:54.771 [main] INFO -- [model] GET /api/employees/Frontend-65
10:31:54.778 [main] INFO -- [tested] GET /api/employees
10:31:54.786 [main] INFO -- [model] GET /api/employees
        ...
```

## Catching Errors
