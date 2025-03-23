package site.easy.to.build.crm.service.employee;

import site.easy.to.build.crm.entity.Employee;

import java.util.List;

public interface EmployeeService {

    public Employee findByEmployeeId(int employeeId);

    public Employee findByEmail(String email);

    public List<Employee> findAll();

    public Employee save(Employee employee);

    public void delete(Employee employee);

}