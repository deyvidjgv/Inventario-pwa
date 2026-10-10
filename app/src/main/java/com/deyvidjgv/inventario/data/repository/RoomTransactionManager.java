package com.deyvidjgv.inventario.data.repository;

import androidx.room.RoomDatabase;
import com.deyvidjgv.inventario.domain.port.TransactionManager;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class RoomTransactionManager implements TransactionManager {
    private final RoomDatabase database;

    public RoomTransactionManager(RoomDatabase database) {
        this.database = database;
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> action) {
        return database.runInTransaction((Callable<T>) action::get);
    }

    @Override
    public void executeInTransaction(Runnable action) {
        database.runInTransaction(action);
    }
}
