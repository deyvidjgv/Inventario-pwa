package com.deyvidjgv.inventario.domain.service;

import com.deyvidjgv.inventario.domain.model.Expense;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {
    Expense add(String concept, long amount, Instant at, Long jornadaId);
    void delete(long expenseId);
    List<Expense> list(LocalDate from, LocalDate to);
}
