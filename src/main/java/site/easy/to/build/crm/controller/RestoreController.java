package site.easy.to.build.crm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RestoreController {  

    @GetMapping("/restore/restore")
    public String showRestorePage(Model model) {
        model.addAttribute("message", "Restauration en cours, veuillez patienter...");
        return "restore/restore";
    }
}