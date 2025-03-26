package site.easy.to.build.crm.util.number;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberFormatUtil {
    
    public static BigDecimal roundToDecimals(BigDecimal value, int decimalPlaces) {
        if (decimalPlaces < 0) 
        { throw new IllegalArgumentException("Decimal places must be non-negative"); }
        return value.setScale(decimalPlaces, RoundingMode.HALF_UP);
    }
}