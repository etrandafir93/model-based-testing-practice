package com.etr.demo.employees;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface EmployeesRepository extends JpaRepository<Employee, Long> {
	Optional<Employee> findByEmployeeNo(String employeeNo);
}
