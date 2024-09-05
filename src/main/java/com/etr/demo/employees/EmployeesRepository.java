package com.etr.demo.employees;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface EmployeesRepository extends JpaRepository<Employee, Long> {
	Optional<Employee> findByEmployeeNo(String employeeNo);

	@Query(value = """
			SELECT * 
			FROM employee 
			WHERE employee_no ILIKE :department || '-%'
			""", nativeQuery = true)
	List<Employee> findByDepartment(@Param("department") String department);
}
