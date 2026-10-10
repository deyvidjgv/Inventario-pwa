package com.deyvidjgv.inventario.domain.service;

import com.deyvidjgv.inventario.domain.dto.JornadaSummary;
import com.deyvidjgv.inventario.domain.dto.PeriodSummary;
import com.deyvidjgv.inventario.domain.dto.ProductMarginReport;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    ProductMarginReport margins(long productId);
    List<ProductMarginReport> allMargins();
    JornadaSummary summary(long jornadaId);
    PeriodSummary summary(LocalDate from, LocalDate to);
}
