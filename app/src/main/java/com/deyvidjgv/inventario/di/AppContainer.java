package com.deyvidjgv.inventario.di;

import android.content.Context;
import com.deyvidjgv.inventario.data.local.database.AppDatabase;
import com.deyvidjgv.inventario.data.repository.*;
import com.deyvidjgv.inventario.domain.port.*;
import com.deyvidjgv.inventario.domain.service.*;
import com.deyvidjgv.inventario.domain.service.impl.*;

public class AppContainer {
    private final InventoryService inventoryService;
    private final SalesService salesService;
    private final ReportService reportService;
    private final ExpenseService expenseService;
    private final BackupService backupService;

    public AppContainer(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        CategoryRepository categoryRepo = new RoomCategoryRepository(db.categoryDao());
        ProductRepository productRepo = new RoomProductRepository(db.productDao());
        StockLotRepository stockLotRepo = new RoomStockLotRepository(db.stockLotDao());
        JornadaRepository jornadaRepo = new RoomJornadaRepository(db.jornadaDao());
        SaleRepository saleRepo = new RoomSaleRepository(db.saleDao());
        StockAdjustmentRepository adjustmentRepo = new RoomStockAdjustmentRepository(db.stockAdjustmentDao());
        ExpenseRepository expenseRepo = new RoomExpenseRepository(db.expenseDao());
        AuditLogRepository auditRepo = new RoomAuditLogRepository(db.auditLogDao());
        TransactionManager transactionManager = new RoomTransactionManager(db);

        this.inventoryService = new InventoryServiceImpl(productRepo, stockLotRepo, categoryRepo);
        this.salesService = new SalesServiceImpl(
                jornadaRepo,
                productRepo,
                stockLotRepo,
                saleRepo,
                adjustmentRepo,
                expenseRepo,
                auditRepo,
                transactionManager
        );
        this.reportService = new ReportServiceImpl(
                productRepo,
                stockLotRepo,
                saleRepo,
                jornadaRepo,
                expenseRepo,
                adjustmentRepo
        );
        this.expenseService = new ExpenseServiceImpl(expenseRepo, jornadaRepo);
        this.backupService = new BackupServiceImpl(
                categoryRepo,
                productRepo,
                stockLotRepo,
                jornadaRepo,
                saleRepo,
                adjustmentRepo,
                expenseRepo,
                auditRepo,
                transactionManager
        );

        // Pre-sembrar categorías por defecto en segundo plano si la BD es nueva
        new Thread(() -> {
            try {
                if (categoryRepo.findAll().isEmpty()) {
                    categoryRepo.save(new com.deyvidjgv.inventario.domain.model.Category("Cervezas"));
                    categoryRepo.save(new com.deyvidjgv.inventario.domain.model.Category("Snacks"));
                    categoryRepo.save(new com.deyvidjgv.inventario.domain.model.Category("Cigarrillos"));
                    categoryRepo.save(new com.deyvidjgv.inventario.domain.model.Category("Licores"));
                    categoryRepo.save(new com.deyvidjgv.inventario.domain.model.Category("Juegos / Pool"));
                    categoryRepo.save(new com.deyvidjgv.inventario.domain.model.Category("General"));
                }
            } catch (Exception ignored) {}
        }).start();
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public SalesService getSalesService() {
        return salesService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public ExpenseService getExpenseService() {
        return expenseService;
    }

    public BackupService getBackupService() {
        return backupService;
    }
}
