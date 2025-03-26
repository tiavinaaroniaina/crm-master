package site.easy.to.build.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AlerteRateDTO {
    private Integer alerteRateId;
    private BigDecimal percentage;
    private LocalDateTime alerteRateDate;

    // Constructors
    public AlerteRateDTO() {}

    public AlerteRateDTO(Integer alerteRateId, BigDecimal percentage, LocalDateTime alerteRateDate) {
        this.alerteRateId = alerteRateId;
        this.percentage = percentage;
        this.alerteRateDate = alerteRateDate;
    }

    // Getters and Setters
    public Integer getAlerteRateId() {
        return alerteRateId;
    }

    public void setAlerteRateId(Integer alerteRateId) {
        this.alerteRateId = alerteRateId;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public LocalDateTime getAlerteRateDate() {
        return alerteRateDate;
    }

    public void setAlerteRateDate(LocalDateTime alerteRateDate) {
        this.alerteRateDate = alerteRateDate;
    }
}