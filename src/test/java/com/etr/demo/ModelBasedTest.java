package com.etr.demo;

import com.etr.demo.ModelBasedTestingActions.TestedVsModel;
import com.etr.demo.utils.TestHttpClient;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
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
	void simpleTest(@ForAll("simpleActions") ActionSequence<TestHttpClient> actions) {
		actions.run(testClient("tested",  ENV.getServicePort("app-tested", 8080)));
	}

	@Property(tries = 110)
	void mbtTest(@ForAll("mbtActions") ActionSequence<TestedVsModel> actions) {
		TestedVsModel testVsModel = new TestedVsModel(
				testClient("tested", ENV.getServicePort("app-tested", 8080)),
				testClient("model", ENV.getServicePort("app-model", 8080))
		);
		actions.run(testVsModel);
	}

	private TestHttpClient testClient(String clientName, int port) {
		String url = "http://localhost:%s/api/employees".formatted(port);
		return new TestHttpClient(clientName, url);
	}

	@Provide
	Arbitrary<ActionSequence<TestedVsModel>> mbtActions() {
		return ModelBasedTestingActions.allActions();
	}

	@Provide
	Arbitrary<ActionSequence<TestHttpClient>> simpleActions() {
		return SimpleActions.allActions();
	}

}

