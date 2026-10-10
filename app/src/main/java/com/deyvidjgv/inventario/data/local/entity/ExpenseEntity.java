package com.deyvidjgv.inventario.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.deyvidjgv.inventario.domain.model.Expense;

import java.time.Instant;

@Entity(
    tableName = "expenses",
    foreignKeys = @ForeignKey(
        entity = JornadaEntity.class,
        parentColumns = "id",
        childColumns = "jornadaId",
        onDelete = ForeignKey.SET_NULL
    ),
    indices = {@Index("jornadaId"), @Index("createdAt")}
)
public class ExpenseEntity {
    @PrimaryKey(autoGenerate = true)
    public Long id;
    public String concept;
    public long amount;
    public Instant createdAt;
    public Long jornadaId;

    public ExpenseEntity() {}

    @Ignore
    public ExpenseEntity(Long id, String concept, long amount, Instant createdAt, Long jornadaId) {
        this.id = id;
        this.concept = concept;
        this.amount = amount;
        this.createdAt = createdAt;
        this.jornadaId = jornadaId;
    }

    public static ExpenseEntity fromDomain(Expense expense) {
        return new ExpenseEntity(
            expense.getId(),
            expense.getConcept(),
            expense.getAmount(),
            expense.getCreatedAt(),
            expense.getJornadaId()
        );
    }

    public Expense toDomain() {
        return new Expense(id, concept, amount, createdAt, jornadaId);
    }
}
