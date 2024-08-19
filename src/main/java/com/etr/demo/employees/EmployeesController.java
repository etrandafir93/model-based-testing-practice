package com.etr.demo.employees;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/employees")
@RequiredArgsConstructor
class EmployeesController {

	private final EmployeesRepository employees;

	record CreateEmployeeRequest(String employeeNo, String name) {
	}

	@GetMapping
	List<Employee> getAll() {
		return employees.findAll();
	}

	@GetMapping("/{employeeNo}")
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
