package com.deyvidjgv.inventario.domain.model;

import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;

import java.time.Instant;
import java.util.Objects;

public class Expense {
    private final Long id;
    private final String concept;
    private final long amount;
    private final Instant createdAt;
    private final Long jornadaId;

    public Expense(Long id, String concept, long amount, Instant createdAt, Long jornadaId) {
        if (concept == null || concept.trim().isEmpty()) {
            throw new IllegalArgumentException("El concepto del gasto no puede estar vacío");
        }
        if (amount <= 0) {
            throw new DomainException(ErrorCode.INVALID_AMOUNT, "El monto del gasto debe ser mayor a 0");
        }
        this.id = id;
        this.concept = concept.trim();
        this.amount = amount;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.jornadaId = jornadaId;
    }

    public Expense(String concept, long amount, Instant createdAt, Long jornadaId) {
        this(null, concept, amount, createdAt, jornadaId);
    }

    public Long getId() {
        return id;
    }

    public String getConcept() {
        return concept;
    }

    public long getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getJornadaId() {
        return jornadaId;
    }

    public Expense withId(Long newId) {
        return new Expense(newId, concept, amount, createdAt, jornadaId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return amount == expense.amount &&
                Objects.equals(id, expense.id) &&
                Objects.equals(concept, expense.concept) &&
                Objects.equals(createdAt, expense.createdAt) &&
                Objects.equals(jornadaId, expense.jornadaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, concept, amount, createdAt, jornadaId);
    }

    @Override
    public String toString() {
        return "Expense{id=" + id + ", concept='" + concept + "', amount=" + amount +
                ", createdAt=" + createdAt + ", jornadaId=" + jornadaId + "}";
    }
}
