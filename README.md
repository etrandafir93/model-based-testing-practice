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

## The Supported "Actions"

For the code examples in this demo, we'll use a simple SpringBoot application with a REST API that allows us to:

- Find an employee by its unique "Employee Number",
- Update an employee's name,
- Register a new employee,
- Get a list of all employees.

![img.png](images/swagger.png)


In this example, we'll use a Docker Compose file to spin up different versions of our application. One version will serve as the "model" or source of truth, while the other will be the version under test. Testcontainers and the _DockerComposeContainer_ class will help us manage these container.

We'll also use Jqwik and a custom HTTP client to define actions supported by the API. Jqwik will then generate various action sequences with different parameters to test our service running in the containers. Regardless of whether the HTTP response is a success or failure, we'll compare the tested service's response with the expected response from the model.

When we run the final test, thousands of requests will be fired at our APIs, helping us uncover corner cases and inconsistencies:
![img.png](images/logs.png)

## Implementation
Even though we won't dive deep into the implementation details, let's discuss the main components of our test, and see how they interact with each-other. 

### Test Http Client
Now that we know the contract, let's create a small test utility that will help us execute the http requests:

```java
class TestHttpClient {
    private final String baseUrl;
    private final RestTemplate restTemplate;

    // constructor

    public EmployeeResponse create(String empNo, String name) { /* ... */ }

    public Optional<EmployeeDto> get(String empNo) { /* ... */ }

    public void update(String empNo, String newName) { /* ... */ }

    public List<EmployeeDto> getAll() { /* ... */ }
}
```
### Jqwik's Actions and Arbitraries

If you're new to Jqwik, you can explore their API in detail by reviewing the [official user guide](https://jqwik.net/docs/current/user-guide.html). While this tutorial won't cover all the specifics of  the API, it's important to know that Jqwik allows us to define a set of actions we want to test.

Let's add the Jqwik dependency to our pom.xml:
```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.9.0</version>
    <scope>test</scope>
</dependency>
```

These actions can also include assertions. For example, the _GetAllEmployeesAction_ can request all entries from both services and compare their responses:

```java
record TestedVsModel(TestHttpClient tested, TestHttpClient model) {}

class GetAllEmployeesAction implements Action<TestedVsModel> {
	@Override
	public TestedVsModel run(TestedVsModel apps) {
		var actual = apps.tested.getAll();
		var expected = apps.model.getAll();
		assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
		return clients;
	}
}
```
After declaring all our actions, we'll create an _ActionSequence_. We'll also wrap these actions in _Arbitrary_ objects. In simple terms, _Arbitrary_ objects follow the _Factory_ design pattern, allowing us to generate objects and actions with randomized parameters:

```java
@Provide
Arbitrary<ActionSequence<TestedVsModel>> mbtActions() {
    return Arbitraries.sequences(Arbitraries.oneOf(
        getOneEmployeeAction(),
        getAllEmplyeesAction(),
        createEmployeeAction(),
        updateEmployeeNameAction()
    ));
}

static Arbitrary<Action<TestedVsModel>> getAllEmplyeesAction() { /* ... */ }

// same for the other actions
```
Finally, we can write our test and leverage Jqwik to test various sequences of with provided actions. However, in order to create the _TestedVsModel_ tuple, we'll need to make sure the two versions of the service are up and running, and acquire their URLs: 

```java
@Property
void mbtTest(@ForAll("mbtActions") ActionSequence<TestedVsModel> actions) {
	TestedVsModel testVsModel = new TestedVsModel( ??? ); // <-- what will we put here?
	actions.run(testVsModel);
}
```

### Docker Compose
The first version of our application uses an in-memory H2 database. This is the version from the "main" branch and, in the scope of this tutorial, we'll consider it the source of truth.  In MBT terms, this _"v1"_ version will be the "model" for testing our application. 

Assuming we have built it as a Docker image with the name _mbt-demo:v1_, let's create a _docker-compose_ file for the test package, and add it as a service:

```yml
version: '3.8'
services:
  app-model:
    image: mbt-demo:v1
```
Now, let's imagine we have implemented a new version that uses a Postgresql database. However, we want to make sure the core functionality hasn't changed. Since the version _"v2"_ will be the component under test, let's also add it to the docker-compose file, together with the _Postgres_ database: 

```yml
version: '3.8'
services:
  app-model:
    image: mbt-demo:v1

  app-tested:
    image: mbt-demo:v2
    depends_on:
      - postgres
    # ...

  postgres:
    image: postgres:16-alpine
    ports:
      - "5432:5432"
    # ...
```

### Testcontainers
Now we simply need to spin up the three containers and obtain the URLs for both the "tested" and "model" versions.

To do this, we'll employ the Testcontainers library and Jqwik's support for Testcontainers:
```xml
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

As a result, we can now instantiate a [`DockerComposeContainer`](https://java.testcontainers.org/modules/docker_compose/) and pass our test _docker-compose_ file as argument:

```java
@Testcontainers
class ModelBasedTest {

	@Container
	static DockerComposeContainer<?> ENV = new DockerComposeContainer<>(new File("src/test/resources/docker-compose-test.yml"))
        .withExposedService("app-tested", 8080, Wait.forHttp("/api/employees").forStatusCode(200))
        .withExposedService("app-model", 8080, Wait.forHttp("/api/employees").forStatusCode(200))
        .withExposedService("postgres", 5432);

	// tests
}
```
Finally, we can create the test http clients and execute the test:

```java
@Property
void mbtTest(@ForAll("mbtActions") ActionSequence<TestedVsModel> actions) {
	TestedVsModel testVsModel = new TestedVsModel(
	    testClient(ENV.getServicePort("app-tested", 8080)),
	    testClient(ENV.getServicePort("app-model", 8080))
	);
	actions.run(testVsModel);
}

private TestHttpClient testClient(int port) {
	String url = "http://localhost:%s/api/employees".formatted(port);
	return new TestHttpClient(url);
}
```

If we run the _@Property_ test now, we'll get a sequence of thousands of requests, trying to find inconsistencies between the model and the tested service:
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
