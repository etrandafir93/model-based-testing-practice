package com.etr.demo.employees;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "employee_no", unique = true)
	private String employeeNo;

	private String name;

	Employee(String employeeNo, String name) {
		this.employeeNo = employeeNo;
		this.name = name;
	}
}
