package site.easy.to.build.crm.service.alert;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import site.easy.to.build.crm.entity.AlerteRate;
import site.easy.to.build.crm.repository.AlerteRateRepository;

@Service
public class AlerteRateService {

    private final AlerteRateRepository alerteRateRepository;

    @Autowired
    public AlerteRateService(AlerteRateRepository alerteRateRepository) {
        this.alerteRateRepository = alerteRateRepository;
    }

    public BigDecimal getLatestAlerteRatePercentage() {
        AlerteRate alerteRate = alerteRateRepository.findFirstByOrderByAlerteRateDateDesc();
        if (alerteRate != null) {
            return alerteRate.getPercentage();
        }
        return BigDecimal.ZERO; // Default to 0% if no alert rate is set
    }

    public boolean wouldExceedRemainingBudget(BigDecimal newExpenseAmount, BigDecimal remainingBudget) {
        return newExpenseAmount.compareTo(remainingBudget) > 0;
    }

    public boolean isAlerteRateReached(BigDecimal totalExpensesAfterNew, BigDecimal totalBudget) {
        BigDecimal alertPercentage = getLatestAlerteRatePercentage();
        BigDecimal alertThreshold = totalBudget.multiply(alertPercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return totalExpensesAfterNew.compareTo(alertThreshold) >= 0;
    }
}