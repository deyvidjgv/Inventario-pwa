package com.deyvidjgv.inventario.domain.model;

import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;

import java.time.Instant;
import java.util.Objects;

public class Jornada {
    private final Long id;
    private final Instant openedAt;
    private final Instant closedAt;

    public Jornada(Long id, Instant openedAt, Instant closedAt) {
        if (openedAt == null) {
            throw new IllegalArgumentException("La fecha de apertura de la jornada no puede ser nula");
        }
        if (closedAt != null && closedAt.isBefore(openedAt)) {
            throw new IllegalArgumentException("La fecha de cierre no puede ser anterior a la de apertura");
        }
        this.id = id;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
    }

    public Jornada(Instant openedAt) {
        this(null, openedAt, null);
    }

    public Long getId() {
        return id;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public boolean isOpen() {
        return closedAt == null;
    }

    public Jornada withId(Long newId) {
        return new Jornada(newId, this.openedAt, this.closedAt);
    }

    public Jornada close(Instant at) {
        if (!isOpen()) {
            throw new DomainException(ErrorCode.JORNADA_NOT_FOUND, "La jornada ya está cerrada");
        }
        return new Jornada(this.id, this.openedAt, at != null ? at : Instant.now());
    }

    public Jornada reopen() {
        return new Jornada(this.id, this.openedAt, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Jornada jornada = (Jornada) o;
        return Objects.equals(id, jornada.id) &&
                Objects.equals(openedAt, jornada.openedAt) &&
                Objects.equals(closedAt, jornada.closedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, openedAt, closedAt);
    }

    @Override
    public String toString() {
        return "Jornada{id=" + id + ", openedAt=" + openedAt + ", closedAt=" + closedAt + ", isOpen=" + isOpen() + "}";
    }
}
