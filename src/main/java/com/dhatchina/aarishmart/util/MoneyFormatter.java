package com.dhatchina.aarishmart.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class MoneyFormatter {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");

    private MoneyFormatter() {
    }

    public static String format(BigDecimal amount) {
        return "₹ " + FORMAT.format(amount);
    }
}
