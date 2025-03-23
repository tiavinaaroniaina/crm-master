package site.easy.to.build.crm.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "alerte_rate")
public class AlerteRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alerte_rate_id")
    private Integer alerteRateId;

    @Column(name = "percentage", nullable = false)
    private BigDecimal percentage;

    @Column(name = "alerte_rate_date", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime alerteRateDate;

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