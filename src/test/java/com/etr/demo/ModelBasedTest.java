package com.etr.demo;

import com.etr.demo.MbtJqwikActions.TestedVsModel;
import com.etr.demo.utils.TestHttpClient;
import net.jqwik.api.*;
import net.jqwik.api.stateful.ActionSequence;
import net.jqwik.testcontainers.Container;
import net.jqwik.testcontainers.Testcontainers;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

@Testcontainers
class ModelBasedTest {

	@Container
	static DockerComposeContainer<?> ENV = new DockerComposeContainer<>(
			new File("src/test/resources/docker-compose-test.yml"))
			.withExposedService("app-tested", 8080,
					Wait.forHttp("/api/employees").forStatusCode(200))
			.withExposedService("app-model", 8080,
					Wait.forHttp("/api/employees").forStatusCode(200))
			.withExposedService("postgres", 5432);

	@Property(tries = 110)
	void mbtTest(@ForAll("mbtActions") ActionSequence<TestedVsModel> actions) {
		TestedVsModel testVsModel = new TestedVsModel(
				testClient("app-tested"),
				testClient("app-model"));

		actions.run(testVsModel);
	}

	private TestHttpClient testClient(String service) {
		int port = ENV.getServicePort(service, 8080);
		return new TestHttpClient(service, "http://localhost:%s/api/employees".formatted(port));
	}

	@Provide
	Arbitrary<ActionSequence<TestedVsModel>> mbtActions() {
		return Arbitraries.sequences(
				Arbitraries.oneOf(
						MbtJqwikActions.getOneEmployeeAction(),
						MbtJqwikActions.getEmployeesByDepartmentAction(),
						MbtJqwikActions.createEmployeeAction(),
						MbtJqwikActions.updateEmployeeNameAction()
				));
	}

}

