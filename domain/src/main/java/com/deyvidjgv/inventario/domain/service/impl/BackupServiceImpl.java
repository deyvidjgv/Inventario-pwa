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
                        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                            in.nextNull();
                            return null;
                        }
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

        try {
            transactionManager.executeInTransaction(() -> {
                // Limpiar datos existentes en orden inverso a dependencias FK (hijos primero)
                saleRepository.deleteAll(); // Borra allocations y luego sales
                stockAdjustmentRepository.deleteAll();
                expenseRepository.deleteAll();
                stockLotRepository.deleteAll();
                productRepository.deleteAll();
                categoryRepository.deleteAll();
                jornadaRepository.deleteAll();
                auditLogRepository.deleteAll();

                // Restaurar entidades con validación contra listas nulas
                if (payload.getCategories() != null) {
                    for (Category c : payload.getCategories()) {
                        if (c != null) categoryRepository.save(c);
                    }
                }
                if (payload.getProducts() != null) {
                    for (Product p : payload.getProducts()) {
                        if (p != null) productRepository.save(p);
                    }
                }
                if (payload.getLots() != null) {
                    for (StockLot l : payload.getLots()) {
                        if (l != null) stockLotRepository.save(l);
                    }
                }
                if (payload.getJornadas() != null) {
                    for (Jornada j : payload.getJornadas()) {
                        if (j != null) jornadaRepository.save(j);
                    }
                }
                List<SaleLotAllocation> allAllocations = payload.getAllocations() != null ? payload.getAllocations() : Collections.emptyList();
                if (payload.getSales() != null) {
                    for (Sale s : payload.getSales()) {
                        if (s != null) {
                            List<SaleLotAllocation> allocs = allAllocations.stream()
                                    .filter(a -> a != null && s.getId() != null && a.getSaleId() == s.getId())
                                    .collect(Collectors.toList());
                            saleRepository.save(s, allocs);
                        }
                    }
                }
                if (payload.getAdjustments() != null) {
                    for (StockAdjustment adj : payload.getAdjustments()) {
                        if (adj != null) stockAdjustmentRepository.save(adj);
                    }
                }
                if (payload.getExpenses() != null) {
                    for (Expense exp : payload.getExpenses()) {
                        if (exp != null) expenseRepository.save(exp);
                    }
                }
                if (payload.getAuditLogs() != null) {
                    for (AuditLog log : payload.getAuditLogs()) {
                        if (log != null) auditLogRepository.save(log);
                    }
                }
            });
        } catch (DomainException de) {
            throw de;
        } catch (Exception e) {
            throw new DomainException(ErrorCode.BACKUP_CORRUPTED, "Error al procesar los datos del archivo de respaldo: " + e.getMessage(), e);
        }
    }
}
