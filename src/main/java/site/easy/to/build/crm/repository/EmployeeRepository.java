package site.easy.to.build.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import site.easy.to.build.crm.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
}