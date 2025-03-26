package site.easy.to.build.crm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import site.easy.to.build.crm.service.data.DataGeneratorService;
import site.easy.to.build.crm.service.ticket.TicketService;

@Controller
@RequestMapping("/generator")
public class DataGeneratorController {

    @Autowired
    private DataGeneratorService dataGeneratorService;

    @Autowired
    private TicketService ticketService;
    
    @GetMapping("/data")
    public String generate(RedirectAttributes redirectAttributes) {
        try {
            dataGeneratorService.generateDataForTable("customer", 20);
            redirectAttributes.addFlashAttribute("successMessage", "Data generated successfully");

            return "redirect:/";   
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occured, please fix it.");
            e.printStackTrace();
            return "redirect:/";   
        }
    }

    @GetMapping("/data/tickets")
    public String generateTickets(RedirectAttributes redirectAttributes) {
        try {
            ticketService.generateRandomTicket(25);
            redirectAttributes.addFlashAttribute("successMessage", "Tickets generated successfully");
            return "redirect:/";   
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An error occured, please fix it.");
            e.printStackTrace();
            return "redirect:/";   
        }
    }
}
