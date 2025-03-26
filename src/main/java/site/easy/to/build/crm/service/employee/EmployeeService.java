package site.easy.to.build.crm.service.employee;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import site.easy.to.build.crm.entity.Employee;
import site.easy.to.build.crm.repository.EmployeeRepository;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private Validator validator; 

    @Transactional(rollbackFor = Exception.class)
    public void importCSV(MultipartFile file) throws Exception {
        List<Employee> employees = new ArrayList<>();
        Set<String> usernames = new HashSet<>(); // Track usernames for uniqueness
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                
                String[] data = line.split(",");
                if (data.length < 5) {
                    throw new IllegalArgumentException("Invalid CSV row: " + line + " - insufficient columns");
                }
                
                Employee employee = new Employee();
                employee.setUsername(data[0].trim());
                employee.setFirstName(data[1].trim());
                employee.setLastName(data[2].trim());
                employee.setEmail(data[3].trim());
                employee.setPassword(data[4].trim());
                employee.setProvider(data.length > 5 ? data[5].trim() : null);
                
                // Manual validation
                Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException("Validation failed for row: " + line, violations);
                }
                
                // Check for duplicate username in this batch
                if (!usernames.add(employee.getUsername())) {
                    throw new IllegalArgumentException("Duplicate username found in CSV: " + employee.getUsername());
                }
                
                employees.add(employee);
            }
            
            if (employees.isEmpty()) {
                throw new IllegalArgumentException("No valid employee data found in CSV file");
            }
            
            employeeRepository.saveAll(employees);
            employeeRepository.flush(); // Force flush to catch DB constraints immediately
        }
    }
}