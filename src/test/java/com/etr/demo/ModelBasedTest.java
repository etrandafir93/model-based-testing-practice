package com.etr.demo;

import com.etr.demo.MbtJqwikActions.ModelVsTested;
import com.etr.demo.utils.TestHttpClient;
import net.jqwik.api.*;
import net.jqwik.api.stateful.ActionSequence;
import net.jqwik.testcontainers.Container;
import net.jqwik.testcontainers.Testcontainers;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

@Testcontainers
class ModelBasedTest {

	@Container
	static ComposeContainer ENV = new ComposeContainer(
				new File("src/test/resources/docker-compose-test.yml"))
			.withExposedService("app-tested", 8080,
					Wait.forHttp("/api/employees").forStatusCode(200))
			.withExposedService("app-model", 8080,
					Wait.forHttp("/api/employees").forStatusCode(200));

	@Property(tries = 110)
	void regressionTest(@ForAll("mbtJqwikActions") ActionSequence<ModelVsTested> actions) {
		ModelVsTested testVsModel = new ModelVsTested(
				testClient("app-model"),
				testClient("app-tested"));

		actions.run(testVsModel);
	}

	static TestHttpClient testClient(String service) {
		int port = ENV.getServicePort(service, 8080);
		String url = "http://localhost:%s/api/employees".formatted(port);
		return new TestHttpClient(service, url);
	}

	@Provide
	Arbitrary<ActionSequence<ModelVsTested>> mbtJqwikActions() {
		return Arbitraries.sequences(
				Arbitraries.oneOf(
						MbtJqwikActions.getOneEmployeeAction(),
						MbtJqwikActions.getEmployeesByDepartmentAction(),
						MbtJqwikActions.createEmployeeAction(),
						MbtJqwikActions.updateEmployeeNameAction()));
	}

}

