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

class MbtJqwikActions {

	record TestedVsModel(TestHttpClient tested, TestHttpClient model) {
	}

	static Arbitrary<ActionSequence<TestedVsModel>> allActions() {
		return Arbitraries.sequences(
				Arbitraries.oneOf(
						getOneEmployeeAction(),
						getAllEmployeesAction(),
						createEmployeeAction(),
						updateEmployeeNameAction()
				));
	}

	static Arbitrary<Action<TestedVsModel>> getAllEmployeesAction() {
		return Arbitraries.nothing().map(__ -> new GetAllEmployeesAction());
	}

	static Arbitrary<GetOneEmployeeAction> getOneEmployeeAction() {
		return employeeNos().map(GetOneEmployeeAction::new);
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
		public TestedVsModel run(TestedVsModel apps) {
			if (apps.model.get(employeeNo).isEmpty()) {
				apps.tested.create(employeeNo, name);
				apps.model.create(employeeNo, name);
				assertEquals(
						apps.model.get(employeeNo).orElseThrow(),
						apps.tested.get(employeeNo).orElseThrow());
			} else {
				try {
					apps.model.create(employeeNo, name);
				} catch (Exception e) {
					assertThatThrownBy(() -> apps.tested.create(employeeNo, name))
							.isInstanceOf(e.getClass());
				}
			}
			return apps;
		}
	}

	@ToString
	@RequiredArgsConstructor
	static class UpdateEmployeeAction implements Action<TestedVsModel> {
		private final String employeeNo;
		private final String newName;

		@Override
		public TestedVsModel run(TestedVsModel apps) {
			if (apps.model.get(employeeNo).isPresent()) {
				apps.tested.update(employeeNo, newName);
				apps.model.update(employeeNo, newName);
				assertEquals(
						apps.model.get(employeeNo).orElseThrow(),
						apps.tested.get(employeeNo).orElseThrow());
			} else {
				try {
					apps.model.update(employeeNo, newName);
				} catch (Exception e) {
					assertThatThrownBy(() -> apps.tested.update(employeeNo, newName))
							.isInstanceOf(e.getClass())
							.hasMessage(e.getMessage());
				}
			}
			return apps;
		}
	}

	@ToString
	@RequiredArgsConstructor
	static class GetOneEmployeeAction implements Action<TestedVsModel> {
		private final String empNo;

		@Override
		public TestedVsModel run(TestedVsModel apps) {
			var actual = apps.tested.get(empNo);
			var expected = apps.model.get(empNo);

			actual.ifPresentOrElse(
					it -> assertThat(expected).hasValue(it),
					() -> assertThat(expected).isEmpty()
			);
			return apps;
		}
	}

	@ToString
	static class GetAllEmployeesAction implements Action<TestedVsModel> {
		@Override
		public TestedVsModel run(TestedVsModel apps) {
			var actual = apps.tested.getAll();
			var expected = apps.model.getAll();
			assertThat(actual)
					.containsExactlyInAnyOrderElementsOf(expected);
			return apps;
		}
	}
}
