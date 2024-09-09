package com.etr.demo.employees;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/employees")
@RequiredArgsConstructor
@Tag(name = "Manage Employees")
class EmployeesController {

	private final EmployeesRepository employees;

	record CreateEmployeeRequest(String employeeNo, String name) {
	}

	@GetMapping
	@Operation(summary = "Find Employee from a given department.")
	List<Employee> getAll(@RequestParam(required = false) String department) {
		if (department == null) {
			return employees.findAll();
		} else {
			return employees.findByDepartment(department);
		}
	}

	@GetMapping("/{employeeNo}")
	@Operation(summary = "Find an Employee based on his {employeeNo}.")
	ResponseEntity<?> get(@PathVariable String employeeNo) {
		try {
			return employees.findByEmployeeNo(employeeNo)
					.map(it -> new ResponseEntity<>(it, HttpStatus.OK))
					.orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(e.getMessage());
		}
	}

	@PostMapping
	@Operation(summary = "Register an Employee.")
	ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRequest req) {
		try {
			var employee = employees.save(new Employee(req.employeeNo, req.name));
			return new ResponseEntity<>(employee, HttpStatus.CREATED);
		} catch (DataIntegrityViolationException e) {
			return ResponseEntity.badRequest()
					.body("an employee with employeeNo=%s already exists".formatted(req.employeeNo));
		}
	}


	@PutMapping("/{employeeNo}")
	@Operation(summary = "Update an Employee's name.")
	ResponseEntity<Employee> update(@PathVariable String employeeNo, @RequestParam String name) {
		var empOpt = employees.findByEmployeeNo(employeeNo);
		if (empOpt.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		var employee = empOpt.get();
		employee.setName(name);
		var updated = employees.save(employee);
		return new ResponseEntity<>(updated, HttpStatus.OK);
	}

}
