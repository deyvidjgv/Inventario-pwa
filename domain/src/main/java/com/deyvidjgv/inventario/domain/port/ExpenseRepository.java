package com.deyvidjgv.inventario.domain.port;

import com.deyvidjgv.inventario.domain.model.Expense;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {
    Expense save(Expense expense);
    void delete(long id);
    Optional<Expense> findById(long id);
    List<Expense> findByJornadaId(long jornadaId);
    List<Expense> findByPeriod(Instant from, Instant to);
    List<Expense> findAll();
    void deleteAll();
}
