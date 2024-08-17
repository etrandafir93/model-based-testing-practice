package com.etr.demo.employees;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "employeeNo"))
class Employee {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String employeeNo;
	private String name;

	Employee(String employeeNo, String name) {
		this.employeeNo = employeeNo;
		this.name = name;
	}
}
