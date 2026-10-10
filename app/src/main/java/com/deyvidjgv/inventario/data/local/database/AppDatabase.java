package com.deyvidjgv.inventario.data.local.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.deyvidjgv.inventario.data.local.converter.Converters;
import com.deyvidjgv.inventario.data.local.dao.*;
import com.deyvidjgv.inventario.data.local.entity.*;

@Database(
    entities = {
        CategoryEntity.class,
        ProductEntity.class,
        StockLotEntity.class,
        JornadaEntity.class,
        SaleEntity.class,
        SaleLotAllocationEntity.class,
        StockAdjustmentEntity.class,
        ExpenseEntity.class,
        AuditLogEntity.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract StockLotDao stockLotDao();
    public abstract JornadaDao jornadaDao();
    public abstract SaleDao saleDao();
    public abstract StockAdjustmentDao stockAdjustmentDao();
    public abstract ExpenseDao expenseDao();
    public abstract AuditLogDao auditLogDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "inventario_pool.db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
