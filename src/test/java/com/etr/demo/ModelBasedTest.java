package com.etr.demo;

import com.etr.demo.ModelBasedTestingActions.SubjectVsModel;
import com.etr.demo.utils.TestHttpClient;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.stateful.ActionSequence;
import net.jqwik.testcontainers.Container;
import net.jqwik.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ModelBasedTest {
	@Container
	private static GenericContainer<?> CONTAINER_MODEL = new GenericContainer(DockerImageName.parse("mbt-demo:v1"))
			.withExposedPorts(8080);

	@Container
	private static GenericContainer<?> CONTAINER_SUBJECT = new GenericContainer(DockerImageName.parse("mbt-demo:v1"))
			.withExposedPorts(8080);


	@Property(tries = 110)
	void simpleTest(@ForAll("simpleActions") ActionSequence<TestHttpClient> actions) {
		actions.run(testClient("v1", CONTAINER_MODEL));
	}

	@Property(tries = 110)
	void mbtTest(@ForAll("mbtActions") ActionSequence<SubjectVsModel> actions) {
		actions.run(
				new SubjectVsModel(
						testClient("v1", CONTAINER_MODEL),
						testClient("v2", CONTAINER_SUBJECT)
				));
	}

	@Provide
	Arbitrary<ActionSequence<SubjectVsModel>> mbtActions() {
		return ModelBasedTestingActions.allActions();
	}

	@Provide
	Arbitrary<ActionSequence<TestHttpClient>> simpleActions() {
		return SimpleActions.allActions();
	}


	private TestHttpClient testClient(String clientName, GenericContainer<?> container) {
		String url = "http://localhost:%s/api/employees".formatted(
				container.getMappedPort(8080));
		return new TestHttpClient(clientName, url);
	}

}

