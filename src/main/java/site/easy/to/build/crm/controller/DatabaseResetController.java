package site.easy.to.build.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import site.easy.to.build.crm.service.db.DatabaseResetService;

@Controller
public class DatabaseResetController {

    @Autowired
    private DatabaseResetService databaseResetService;
    
    @GetMapping("/database/reset")
    public String reset(RedirectAttributes attributes) {
        databaseResetService.resetDatabase();
        attributes.addFlashAttribute("successMessage", "Database has been reset successfully !");
        return "redirect:/";
    }
}