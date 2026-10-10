package com.deyvidjgv.inventario.data.repository;

import com.deyvidjgv.inventario.data.local.dao.ExpenseDao;
import com.deyvidjgv.inventario.data.local.entity.ExpenseEntity;
import com.deyvidjgv.inventario.domain.model.Expense;
import com.deyvidjgv.inventario.domain.port.ExpenseRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomExpenseRepository implements ExpenseRepository {
    private final ExpenseDao expenseDao;

    public RoomExpenseRepository(ExpenseDao expenseDao) {
        this.expenseDao = expenseDao;
    }

    @Override
    public Expense save(Expense expense) {
        ExpenseEntity entity = ExpenseEntity.fromDomain(expense);
        long id = expenseDao.insert(entity);
        return expense.withId(id);
    }

    @Override
    public void delete(long id) {
        expenseDao.delete(id);
    }

    @Override
    public Optional<Expense> findById(long id) {
        ExpenseEntity entity = expenseDao.findById(id);
        return entity != null ? Optional.of(entity.toDomain()) : Optional.empty();
    }

    @Override
    public List<Expense> findByJornadaId(long jornadaId) {
        List<ExpenseEntity> entities = expenseDao.findByJornadaId(jornadaId);
        List<Expense> list = new ArrayList<>();
        for (ExpenseEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<Expense> findByPeriod(Instant from, Instant to) {
        List<ExpenseEntity> entities = expenseDao.findByPeriod(from, to);
        List<Expense> list = new ArrayList<>();
        for (ExpenseEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public List<Expense> findAll() {
        List<ExpenseEntity> entities = expenseDao.findAll();
        List<Expense> list = new ArrayList<>();
        for (ExpenseEntity entity : entities) {
            list.add(entity.toDomain());
        }
        return list;
    }

    @Override
    public void deleteAll() {
        expenseDao.deleteAll();
    }
}
