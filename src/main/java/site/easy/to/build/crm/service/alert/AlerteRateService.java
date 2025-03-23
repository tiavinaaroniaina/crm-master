package site.easy.to.build.crm.service.alert;

import java.math.BigDecimal;

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

    public boolean isAlerteRateReached(BigDecimal expenseAmount, BigDecimal budget) {
        BigDecimal alertPercentage = getLatestAlerteRatePercentage();
        BigDecimal alertThreshold = budget.multiply(alertPercentage).divide(BigDecimal.valueOf(100));
        return expenseAmount.compareTo(alertThreshold) >= 0;
    }

    public boolean isBudgetExceeded(BigDecimal expenseAmount, BigDecimal budget) {
        System.out.println("Expense amount: " + expenseAmount.toString());
        System.out.println("Budget amount: " + budget.toString());

        return expenseAmount.compareTo(budget) > 0;
    }
}