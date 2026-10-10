package com.deyvidjgv.inventario.domain.service.impl;

import com.deyvidjgv.inventario.domain.dto.BackupPayload;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.exception.ErrorCode;
import com.deyvidjgv.inventario.domain.model.*;
import com.deyvidjgv.inventario.domain.port.*;
import com.deyvidjgv.inventario.domain.service.BackupService;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BackupServiceImpl implements BackupService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StockLotRepository stockLotRepository;
    private final JornadaRepository jornadaRepository;
    private final SaleRepository saleRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ExpenseRepository expenseRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionManager transactionManager;
    private final Gson gson;

    public BackupServiceImpl(CategoryRepository categoryRepository,
                             ProductRepository productRepository,
                             StockLotRepository stockLotRepository,
                             JornadaRepository jornadaRepository,
                             SaleRepository saleRepository,
                             StockAdjustmentRepository stockAdjustmentRepository,
                             ExpenseRepository expenseRepository,
                             AuditLogRepository auditLogRepository,
                             TransactionManager transactionManager) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.stockLotRepository = stockLotRepository;
        this.jornadaRepository = jornadaRepository;
        this.saleRepository = saleRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.expenseRepository = expenseRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionManager = transactionManager;

        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new TypeAdapter<Instant>() {
                    @Override
                    public void write(JsonWriter out, Instant value) throws IOException {
                        if (value == null) {
                            out.nullValue();
                        } else {
                            out.value(value.toString());
                        }
                    }

                    @Override
                    public Instant read(JsonReader in) throws IOException {
                        String str = in.nextString();
                        return str != null ? Instant.parse(str) : null;
                    }
                })
                .setPrettyPrinting()
                .create();
    }

    @Override
    public String exportJson() {
        BackupPayload payload = new BackupPayload();
        payload.setVersion(1);
        payload.setExportedAt(Instant.now().toString());
        payload.setCategories(categoryRepository.findAll());
        payload.setProducts(productRepository.findAll());
        payload.setLots(stockLotRepository.findAll());
        payload.setJornadas(jornadaRepository.findAll());
        payload.setSales(saleRepository.findAll());
        payload.setAllocations(saleRepository.findAllAllocations());
        payload.setAdjustments(stockAdjustmentRepository.findAll());
        payload.setExpenses(expenseRepository.findAll());
        payload.setAuditLogs(auditLogRepository.findAll());

        return gson.toJson(payload);
    }

    @Override
    public void importJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new DomainException(ErrorCode.BACKUP_CORRUPTED, "El archivo de respaldo está vacío");
        }

        BackupPayload payload;
        try {
            payload = gson.fromJson(json, BackupPayload.class);
        } catch (Exception e) {
            throw new DomainException(ErrorCode.BACKUP_CORRUPTED, "Formato JSON inválido o corrupto", e);
        }

        if (payload == null || payload.getVersion() != 1) {
            throw new DomainException(ErrorCode.BACKUP_CORRUPTED, "Versión de respaldo incompatible o inválida");
        }

        transactionManager.executeInTransaction(() -> {
            // Limpiar datos existentes
            categoryRepository.deleteAll();
            productRepository.deleteAll();
            stockLotRepository.deleteAll();
            jornadaRepository.deleteAll();
            saleRepository.deleteAll();
            stockAdjustmentRepository.deleteAll();
            expenseRepository.deleteAll();
            auditLogRepository.deleteAll();

            // Restaurar entidades
            for (Category c : payload.getCategories()) {
                categoryRepository.save(c);
            }
            for (Product p : payload.getProducts()) {
                productRepository.save(p);
            }
            for (StockLot l : payload.getLots()) {
                stockLotRepository.save(l);
            }
            for (Jornada j : payload.getJornadas()) {
                jornadaRepository.save(j);
            }
            List<SaleLotAllocation> allAllocations = payload.getAllocations() != null ? payload.getAllocations() : Collections.emptyList();
            for (Sale s : payload.getSales()) {
                List<SaleLotAllocation> allocs = allAllocations.stream()
                        .filter(a -> a.getSaleId() == s.getId())
                        .collect(Collectors.toList());
                saleRepository.save(s, allocs);
            }
            for (StockAdjustment adj : payload.getAdjustments()) {
                stockAdjustmentRepository.save(adj);
            }
            for (Expense exp : payload.getExpenses()) {
                expenseRepository.save(exp);
            }
            for (AuditLog log : payload.getAuditLogs()) {
                auditLogRepository.save(log);
            }
        });
    }
}
