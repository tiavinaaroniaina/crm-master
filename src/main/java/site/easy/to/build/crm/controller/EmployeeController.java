package site.easy.to.build.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import site.easy.to.build.crm.service.employee.EmployeeService;

@Controller
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/import-employees")
    public String showImportForm(Model model) {
        return "employee/import-employees";
    }

    @PostMapping("/import-employees")
    public String importEmployees(@RequestParam("file") MultipartFile file, 
                                RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
                return "redirect:/import-employees";
            }
            
            employeeService.importCSV(file);
            redirectAttributes.addFlashAttribute("message", 
                "Employees imported successfully from " + file.getOriginalFilename());
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", 
                "Error importing file: " + e.getMessage());
        }
        
        return "redirect:/import-employees";
    }
}