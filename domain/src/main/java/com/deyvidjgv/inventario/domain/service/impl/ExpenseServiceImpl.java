package com.deyvidjgv.inventario.domain.service.impl;

import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;
import com.deyvidjgv.inventario.domain.model.Expense;
import com.deyvidjgv.inventario.domain.port.ExpenseRepository;
import com.deyvidjgv.inventario.domain.port.JornadaRepository;
import com.deyvidjgv.inventario.domain.service.ExpenseService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class ExpenseServiceImpl implements ExpenseService {
    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    private final ExpenseRepository expenseRepository;
    private final JornadaRepository jornadaRepository;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository, JornadaRepository jornadaRepository) {
        this.expenseRepository = expenseRepository;
        this.jornadaRepository = jornadaRepository;
    }

    @Override
    public Expense add(String concept, long amount, Instant at, Long jornadaId) {
        if (jornadaId != null && !jornadaRepository.findById(jornadaId).isPresent()) {
            throw new DomainException(ErrorCode.JORNADA_NOT_FOUND, "La jornada especificada para el gasto no existe: " + jornadaId);
        }
        Instant createdAt = at != null ? at : Instant.now();
        Expense expense = new Expense(concept, amount, createdAt, jornadaId);
        return expenseRepository.save(expense);
    }

    @Override
    public void delete(long expenseId) {
        if (!expenseRepository.findById(expenseId).isPresent()) {
            throw new DomainException(ErrorCode.EXPENSE_NOT_FOUND, "Gasto no encontrado: " + expenseId);
        }
        expenseRepository.delete(expenseId);
    }

    @Override
    public List<Expense> list(LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(BOGOTA_ZONE).toInstant();
        return expenseRepository.findByPeriod(start, end);
    }
}
