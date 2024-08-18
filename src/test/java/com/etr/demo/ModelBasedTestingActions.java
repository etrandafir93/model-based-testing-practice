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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelBasedTestingActions {

	record TestedVsModel(TestHttpClient tested, TestHttpClient model) {
	}

	static Arbitrary<ActionSequence<TestedVsModel>> allActions() {
		return Arbitraries.sequences(
				Arbitraries.oneOf(
						createEmployeeAction(),
						getAllAction(),
						updateEmployeeNameAction()
				));
	}

	static Arbitrary<Action<TestedVsModel>> getAllAction() {
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
	static class CreateEmployeeAction implements Action<TestedVsModel> {
		private final String employeeNo;
		private final String name;

		@Override
		public TestedVsModel run(TestedVsModel clients) {
			if (clients.model.get(employeeNo).isEmpty()) {
				clients.tested.create(employeeNo, name);
				clients.model.create(employeeNo, name);
				assertEquals(
						clients.model.get(employeeNo).orElseThrow().name(),
						clients.tested.get(employeeNo).orElseThrow().name());
			} else {
				try {
					clients.model.create(employeeNo, name);
				} catch (Exception e) {
					assertThatThrownBy(() -> clients.tested.create(employeeNo, name))
							.isInstanceOf(e.getClass());
				}
			}
			return clients;
		}
	}

	@ToString
	@RequiredArgsConstructor
	static class UpdateEmployeeAction implements Action<TestedVsModel> {
		private final String employeeNo;
		private final String newName;

		@Override
		public TestedVsModel run(TestedVsModel clients) {
			if (clients.model.get(employeeNo).isPresent()) {
				clients.tested.update(employeeNo, newName);
				clients.model.update(employeeNo, newName);
				assertEquals(
						clients.model.get(employeeNo).orElseThrow().name(),
						clients.tested.get(employeeNo).orElseThrow().name());
			} else {
				try {
					clients.model.update(employeeNo, newName);
				} catch (Exception e) {
					assertThatThrownBy(() -> clients.tested.update(employeeNo, newName))
							.isInstanceOf(e.getClass())
							.hasMessage(e.getMessage());
				}
			}
			return clients;
		}
	}

	@ToString
	static class GetAllAction implements Action<TestedVsModel> {
		@Override
		public TestedVsModel run(TestedVsModel clients) {
			var actual = clients.tested.getAll();
			var expected = clients.model.getAll();
			assertThat(actual)
					.containsExactlyInAnyOrderElementsOf(expected);
			return clients;
		}
	}
}
