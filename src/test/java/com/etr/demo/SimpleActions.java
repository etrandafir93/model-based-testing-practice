package com.etr.demo;

import com.etr.demo.utils.TestHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.arbitraries.StringArbitrary;
import net.jqwik.api.stateful.Action;
import net.jqwik.api.stateful.ActionSequence;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleActions {

	static Arbitrary<ActionSequence<TestHttpClient>> allActions() {
		return Arbitraries.sequences(
				Arbitraries.oneOf(
						createEmployeeAction(),
						getAllAction(),
						updateEmployeeNameAction()
				));
	}

	static Arbitrary<Action<TestHttpClient>> getAllAction() {
		return Arbitraries.nothing().map(__ -> new GetAllAction());
	}

	static Arbitrary<CreateEmployeeAction> createEmployeeAction() {
		StringArbitrary names = Arbitraries.strings()
				.alpha()
				.ofMinLength(1);
		return Combinators.combine(employeeNos(), names)
				.as(CreateEmployeeAction::new);
	}

	static Arbitrary<UpdateEmployeeAction> updateEmployeeNameAction() {
		Arbitrary<String> newNames = Arbitraries.strings()
				.alpha()
				.ofMinLength(1);
		return Combinators.combine(employeeNos(), newNames)
				.as(UpdateEmployeeAction::new);
	}

	static Arbitrary<String> employeeNos() {
		Arbitrary<String> departments = Arbitraries.of("Frontend", "Backend", "HR", "Creative", "DevOps");
		Arbitrary<Long> ids = Arbitraries.longs().between(1, 200);
		return Combinators.combine(departments, ids)
				.as("%s-%s"::formatted);
	}

	@ToString
	@RequiredArgsConstructor
	static class CreateEmployeeAction implements Action<TestHttpClient> {
		private final String employeeNo;
		private final String name;

		@Override
		public TestHttpClient run(TestHttpClient client) {
			if (client.get(employeeNo).isEmpty()) {
				client.create(employeeNo, name);
				assertEquals(name, client.get(employeeNo).orElseThrow().name());
			} else {
				assertThatThrownBy(() -> client.create(employeeNo, name))
						.isInstanceOf(HttpClientErrorException.BadRequest.class)
						.hasMessageContaining("an employee with employeeNo=%s already exists"
								.formatted(employeeNo));
			}
			return client;
		}
	}

	@ToString
	@RequiredArgsConstructor
	static class UpdateEmployeeAction implements Action<TestHttpClient> {
		private final String employeeNo;
		private final String newName;

		@Override
		public TestHttpClient run(TestHttpClient client) {
			if (client.get(employeeNo).isPresent()) {
				client.update(employeeNo, newName);
				assertEquals(newName, client.get(employeeNo).orElseThrow().name());
			} else {
				assertThatThrownBy(() -> client.update(employeeNo, newName))
						.isInstanceOf(HttpClientErrorException.NotFound.class);
			}
			return client;
		}
	}

	@ToString
	static class GetAllAction implements Action<TestHttpClient> {
		@Override
		public TestHttpClient run(TestHttpClient client) {
			var all = client.getAll();
			return client;
		}
	}
}
