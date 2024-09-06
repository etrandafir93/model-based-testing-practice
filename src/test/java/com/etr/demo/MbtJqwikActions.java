package com.etr.demo;

import com.etr.demo.utils.TestHttpClient;
import com.etr.demo.utils.TestHttpClient.ApiResponse;
import com.etr.demo.utils.TestHttpClient.EmployeeDto;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.arbitraries.StringArbitrary;
import net.jqwik.api.stateful.Action;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class MbtJqwikActions {

	record TestedVsModel(TestHttpClient tested, TestHttpClient model) {
	}

	static Arbitrary<GetOneEmployeeAction> getOneEmployeeAction() {
		return employeeNos().map(GetOneEmployeeAction::new);
	}

	static Arbitrary<GetEmployeesByDepartmentAction> getEmployeesByDepartmentAction() {
		Arbitrary<String> invalidDepartments = Arbitraries.strings().ofMinLength(1);
		Arbitrary<String> validDepartments = Arbitraries.of(
				"frontend", "backend", "hr", "creative", "devops",
				"FRONTEND", "BACKEND", "HR", "CREATIVE", "DEVOPS",
				"fROntEND", "backEND", "Hr", "crEATive", "devOPS"
		);

		return Arbitraries.oneOf(validDepartments, invalidDepartments)
				.map(GetEmployeesByDepartmentAction::new);
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

	record CreateEmployeeAction(String empNo, String name) implements Action<TestedVsModel> {
		@Override
		public TestedVsModel run(TestedVsModel apps) {
			ApiResponse<EmployeeDto> actual = apps.tested.post(empNo, name);
			ApiResponse<EmployeeDto> expected = apps.model.post(empNo, name);

			assertThat(actual)
					.satisfies(statusCodeEquals(expected.statusCode()))
					.satisfies(bodyEquals(expected.body()));
			return apps;
		}
	}

	record UpdateEmployeeAction(String empNo, String newName) implements Action<TestedVsModel> {
		@Override
		public TestedVsModel run(TestedVsModel apps) {
			ApiResponse<Void> actual = apps.tested.put(empNo, newName);
			ApiResponse<Void> expected = apps.model.put(empNo, newName);

			assertThat(actual)
					.satisfies(statusCodeEquals(expected.statusCode()));

			ApiResponse<EmployeeDto> actualAfterUpdate = apps.tested.get(empNo);
			ApiResponse<EmployeeDto> expectedAfterUpdate = apps.model.get(empNo);

			assertThat(actualAfterUpdate)
					.satisfies(statusCodeEquals(expectedAfterUpdate.statusCode()))
					.satisfies(bodyEquals(expectedAfterUpdate.body()));
			return apps;
		}
	}

	record GetOneEmployeeAction(String empNo) implements Action<TestedVsModel> {
		@Override
		public TestedVsModel run(TestedVsModel apps) {
			ApiResponse<EmployeeDto> actual = apps.tested.get(empNo);
			ApiResponse<EmployeeDto> expected = apps.model.get(empNo);

			assertThat(actual)
					.satisfies(statusCodeEquals(expected.statusCode()))
					.satisfies(bodyEquals(expected.body()));
			return apps;
		}
	}

	record GetEmployeesByDepartmentAction(String department) implements Action<TestedVsModel> {
		@Override
		public TestedVsModel run(TestedVsModel apps) {
			ApiResponse<List<EmployeeDto>> actual = apps.tested.getByDepartment(department);
			ApiResponse<List<EmployeeDto>> expected = apps.model.getByDepartment(department);

			assertThat(actual)
					.satisfies(statusCodeEquals(expected.statusCode()))
					.satisfies(bodyEquals(expected.body()));
			return apps;
		}

	}

	private static <T> Consumer<ApiResponse<T>> statusCodeEquals(int expectedStatusCode) {
		return actual -> assertThat(actual.statusCode()).isEqualTo(expectedStatusCode);
	}

	private static <T> Consumer<ApiResponse<T>> bodyEquals(T expectedBody) {
		return actual -> assertThat(actual.body()).isEqualTo(expectedBody);
	}
}
