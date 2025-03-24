package site.easy.to.build.crm.controller.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.easy.to.build.crm.dto.AlerteRateDTO;
import site.easy.to.build.crm.entity.AlerteRate;
import site.easy.to.build.crm.repository.AlerteRateRepository;

@RestController
@RequestMapping("/api/alerte-rates")
public class AlerteRateApiController {

    private final AlerteRateRepository alerteRateRepository;

    @Autowired
    public AlerteRateApiController(AlerteRateRepository alerteRateRepository) {
        this.alerteRateRepository = alerteRateRepository;
    }

    // GET: Retrieve all AlerteRate records
    @GetMapping
    public ResponseEntity<?> getAllAlerteRates() {
        try {
            List<AlerteRate> alerteRates = alerteRateRepository.findAll();
            if (alerteRates.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No AlerteRate records found");
            }

            // Convert to DTO
            List<AlerteRateDTO> alerteRateDTOs = alerteRates.stream()
                .map(ar -> new AlerteRateDTO(
                    ar.getAlerteRateId(),
                    ar.getPercentage(),
                    ar.getAlerteRateDate()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(alerteRateDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving AlerteRate records: " + e.getMessage());
        }
    }

    // GET: Retrieve a specific AlerteRate by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getAlerteRateById(@PathVariable Integer id) {
        try {
            AlerteRate alerteRate = alerteRateRepository.findByAlerteRateId(id);
            if (alerteRate == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("AlerteRate not found with ID: " + id);
            }

            // Convert to DTO
            AlerteRateDTO alerteRateDTO = new AlerteRateDTO(
                alerteRate.getAlerteRateId(),
                alerteRate.getPercentage(),
                alerteRate.getAlerteRateDate()
            );

            return ResponseEntity.ok(alerteRateDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving AlerteRate with ID " + id + ": " + e.getMessage());
        }
    }

    // PUT: Update an existing AlerteRate
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAlerteRate(@PathVariable Integer id, @RequestBody AlerteRateDTO updatedAlerteRateDTO) {
        try {
            AlerteRate existingAlerteRate = alerteRateRepository.findByAlerteRateId(id);
            if (existingAlerteRate == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("AlerteRate not found with ID: " + id);
            }

            // Update fields
            existingAlerteRate.setPercentage(updatedAlerteRateDTO.getPercentage());
            if (updatedAlerteRateDTO.getAlerteRateDate() != null) {
                existingAlerteRate.setAlerteRateDate(updatedAlerteRateDTO.getAlerteRateDate());
            }

            // Save updated entity
            AlerteRate updatedAlerteRate = alerteRateRepository.save(existingAlerteRate);

            // Convert to DTO for response
            AlerteRateDTO responseDTO = new AlerteRateDTO(
                updatedAlerteRate.getAlerteRateId(),
                updatedAlerteRate.getPercentage(),
                updatedAlerteRate.getAlerteRateDate()
            );

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating AlerteRate with ID " + id + ": " + e.getMessage());
        }
    }
}