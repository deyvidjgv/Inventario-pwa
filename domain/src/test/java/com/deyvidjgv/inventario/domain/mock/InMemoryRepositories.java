package com.deyvidjgv.inventario.domain.mock;

import com.deyvidjgv.inventario.domain.model.*;
import com.deyvidjgv.inventario.domain.port.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InMemoryRepositories {

    public static class TransactionManagerImpl implements TransactionManager {
        @Override
        public <T> T executeInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public void executeInTransaction(Runnable action) {
            action.run();
        }
    }

    public static class CategoryRepositoryImpl implements CategoryRepository {
        private final Map<Long, Category> storage = new LinkedHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public Category save(Category category) {
            Long id = category.getId() != null ? category.getId() : idGenerator.getAndIncrement();
            Category saved = category.withId(id);
            storage.put(id, saved);
            return saved;
        }

        @Override
        public Optional<Category> findById(long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<Category> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }
    }

    public static class ProductRepositoryImpl implements ProductRepository {
        private final Map<Long, Product> storage = new LinkedHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public Product save(Product product) {
            Long id = product.getId() != null ? product.getId() : idGenerator.getAndIncrement();
            Product saved = product.withId(id);
            storage.put(id, saved);
            return saved;
        }

        @Override
        public Optional<Product> findById(long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<Product> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public List<Product> findActive() {
            return storage.values().stream().filter(Product::isActive).collect(Collectors.toList());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }
    }

    public static class StockLotRepositoryImpl implements StockLotRepository {
        private final Map<Long, StockLot> storage = new LinkedHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public StockLot save(StockLot lot) {
            Long id = lot.getId() != null ? lot.getId() : idGenerator.getAndIncrement();
            StockLot saved = lot.withId(id);
            storage.put(id, saved);
            return saved;
        }

        @Override
        public List<StockLot> saveAll(List<StockLot> lots) {
            List<StockLot> savedList = new ArrayList<>();
            for (StockLot lot : lots) {
                savedList.add(save(lot));
            }
            return savedList;
        }

        @Override
        public Optional<StockLot> findById(long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<StockLot> findActiveByProductId(long productId) {
            return storage.values().stream()
                    .filter(l -> l.getProductId() == productId && l.getQuantityRemaining() > 0)
                    .sorted((l1, l2) -> {
                        int comp = l1.getReceivedAt().compareTo(l2.getReceivedAt());
                        return comp != 0 ? comp : Long.compare(l1.getId(), l2.getId());
                    })
                    .collect(Collectors.toList());
        }

        @Override
        public List<StockLot> findAllByProductId(long productId) {
            return storage.values().stream()
                    .filter(l -> l.getProductId() == productId)
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<StockLot> findNewestByProductId(long productId) {
            return storage.values().stream()
                    .filter(l -> l.getProductId() == productId)
                    .max((l1, l2) -> {
                        int comp = l1.getReceivedAt().compareTo(l2.getReceivedAt());
                        return comp != 0 ? comp : Long.compare(l1.getId(), l2.getId());
                    });
        }

        @Override
        public List<StockLot> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }
    }

    public static class JornadaRepositoryImpl implements JornadaRepository {
        private final Map<Long, Jornada> storage = new LinkedHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public Jornada save(Jornada jornada) {
            Long id = jornada.getId() != null ? jornada.getId() : idGenerator.getAndIncrement();
            Jornada saved = jornada.withId(id);
            storage.put(id, saved);
            return saved;
        }

        @Override
        public Optional<Jornada> findById(long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public Optional<Jornada> findOpen() {
            return storage.values().stream().filter(Jornada::isOpen).findFirst();
        }

        @Override
        public Optional<Jornada> findLastClosed() {
            return storage.values().stream()
                    .filter(j -> !j.isOpen())
                    .max(Comparator.comparing(Jornada::getClosedAt));
        }

        @Override
        public List<Jornada> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }
    }

    public static class SaleRepositoryImpl implements SaleRepository {
        private final Map<Long, Sale> salesStorage = new LinkedHashMap<>();
        private final List<SaleLotAllocation> allocationsStorage = new ArrayList<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public Sale save(Sale sale, List<SaleLotAllocation> allocations) {
            Long id = sale.getId() != null ? sale.getId() : idGenerator.getAndIncrement();
            Sale savedSale = sale.withId(id);
            salesStorage.put(id, savedSale);
            for (SaleLotAllocation alloc : allocations) {
                allocationsStorage.add(new SaleLotAllocation(id, alloc.getLotId(), alloc.getQuantity(), alloc.getUnitCost()));
            }
            return savedSale;
        }

        @Override
        public void update(Sale sale) {
            salesStorage.put(sale.getId(), sale);
        }

        @Override
        public Optional<Sale> findById(long id) {
            return Optional.ofNullable(salesStorage.get(id));
        }

        @Override
        public List<Sale> findByJornadaId(long jornadaId) {
            return salesStorage.values().stream()
                    .filter(s -> s.getJornadaId() == jornadaId)
                    .collect(Collectors.toList());
        }

        @Override
        public List<Sale> findByPeriod(Instant from, Instant to) {
            return salesStorage.values().stream()
                    .filter(s -> !s.getCreatedAt().isBefore(from) && s.getCreatedAt().isBefore(to))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Sale> findByProductId(long productId) {
            return salesStorage.values().stream()
                    .filter(s -> s.getProductId() == productId)
                    .collect(Collectors.toList());
        }

        @Override
        public List<SaleLotAllocation> findAllocationsBySaleId(long saleId) {
            return allocationsStorage.stream()
                    .filter(a -> a.getSaleId() == saleId)
                    .collect(Collectors.toList());
        }

        @Override
        public List<SaleLotAllocation> findAllocationsByProductId(long productId) {
            Set<Long> saleIds = findByProductId(productId).stream()
                    .map(Sale::getId)
                    .collect(Collectors.toSet());
            return allocationsStorage.stream()
                    .filter(a -> saleIds.contains(a.getSaleId()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Sale> findAll() {
            return new ArrayList<>(salesStorage.values());
        }

        @Override
        public List<SaleLotAllocation> findAllAllocations() {
            return new ArrayList<>(allocationsStorage);
        }

        @Override
        public void deleteAll() {
            salesStorage.clear();
            allocationsStorage.clear();
        }
    }

    public static class StockAdjustmentRepositoryImpl implements StockAdjustmentRepository {
        private final Map<Long, StockAdjustment> storage = new LinkedHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public StockAdjustment save(StockAdjustment adjustment) {
            Long id = adjustment.getId() != null ? adjustment.getId() : idGenerator.getAndIncrement();
            StockAdjustment saved = adjustment.withId(id);
            storage.put(id, saved);
            return saved;
        }

        @Override
        public List<StockAdjustment> findByJornadaId(long jornadaId) {
            return storage.values().stream()
                    .filter(a -> a.getJornadaId() == jornadaId)
                    .collect(Collectors.toList());
        }

        @Override
        public List<StockAdjustment> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }
    }

    public static class ExpenseRepositoryImpl implements ExpenseRepository {
        private final Map<Long, Expense> storage = new LinkedHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public Expense save(Expense expense) {
            Long id = expense.getId() != null ? expense.getId() : idGenerator.getAndIncrement();
            Expense saved = expense.withId(id);
            storage.put(id, saved);
            return saved;
        }

        @Override
        public void delete(long id) {
            storage.remove(id);
        }

        @Override
        public Optional<Expense> findById(long id) {
            return Optional.ofNullable(storage.get(id));
        }

        @Override
        public List<Expense> findByPeriod(Instant from, Instant to) {
            return storage.values().stream()
                    .filter(e -> !e.getCreatedAt().isBefore(from) && e.getCreatedAt().isBefore(to))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Expense> findByJornadaId(long jornadaId) {
            return storage.values().stream()
                    .filter(e -> e.getJornadaId() != null && e.getJornadaId() == jornadaId)
                    .collect(Collectors.toList());
        }

        @Override
        public List<Expense> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }
    }

    public static class AuditLogRepositoryImpl implements AuditLogRepository {
        private final Map<Long, AuditLog> storage = new LinkedHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);

        @Override
        public AuditLog save(AuditLog log) {
            Long id = log.getId() != null ? log.getId() : idGenerator.getAndIncrement();
            AuditLog saved = log.withId(id);
            storage.put(id, saved);
            return saved;
        }

        @Override
        public List<AuditLog> findByEntity(String entity, long entityId) {
            return storage.values().stream()
                    .filter(l -> l.getEntity().equals(entity) && l.getEntityId() == entityId)
                    .collect(Collectors.toList());
        }

        @Override
        public List<AuditLog> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void deleteAll() {
            storage.clear();
        }
    }
}
