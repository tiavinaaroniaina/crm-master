package site.easy.to.build.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import site.easy.to.build.crm.service.csv.CsvService;
import site.easy.to.build.crm.dto.EmployeeDto;
import site.easy.to.build.crm.service.employee.EmployeeService;
import site.easy.to.build.crm.service.database.DatabaseService;
import site.easy.to.build.crm.entity.Employee;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Controller
public class RestoreDatabaseController {
    private final DatabaseService databaseService;
    private final CsvService csvService;
    private final EmployeeService employeeService;

    @Autowired
    public RestoreDatabaseController(DatabaseService databaseService,
                                   CsvService csvService,
                                   EmployeeService employeeService) {
        this.databaseService = databaseService;
        this.csvService = csvService;
        this.employeeService = employeeService;
    }

    @PostMapping("/restore")
    public ResponseEntity<String> restoreDatabase() {
        try {
            databaseService.restoreDatabase();
            return ResponseEntity.ok("Base de données restaurée avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de la restauration : " + e.getMessage());
        }
    }

    @PostMapping("/CsvImport")
    public ResponseEntity<?> importEmployee(@RequestParam("file") MultipartFile file) {
        System.out.println("manaona e");
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier est vide");
            }
    
            Path tempFile = Files.createTempFile("csv_upload_", file.getOriginalFilename());
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
    
            List<EmployeeDto> importedData = csvService.importCsv(tempFile.toString(), EmployeeDto.class);
            
            List<Employee> savedEmployees = new ArrayList<>();
            for (EmployeeDto employeeDto : importedData) {
                
                Employee newEmployee = new Employee(
                    employeeDto.getId().longValue(),
                    employeeDto.getUsername(),
                    employeeDto.getFirstName(),
                    employeeDto.getLastName(),
                    employeeDto.getEmail(),
                    employeeDto.getPassword(),
                    employeeDto.getProvider()
                );
                savedEmployees.add(employeeService.save(newEmployee));
            }
    
            Files.deleteIfExists(tempFile);
            return ResponseEntity.ok(savedEmployees);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erreur lors de l'import : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur inattendue : " + e.getMessage());
        }

        
    }
   
}