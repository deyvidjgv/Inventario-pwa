package com.deyvidjgv.inventario.ui.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter {
    private static final DecimalFormat FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "CO"));
        symbols.setGroupingSeparator('.');
        symbols.setCurrencySymbol("$");
        FORMATTER = new DecimalFormat("$#,##0", symbols);
    }

    public static String formatCOP(long amount) {
        return FORMATTER.format(amount);
    }

    public static String formatPercent(double percent) {
        return String.format(Locale.US, "%.1f %%", percent);
    }
}
