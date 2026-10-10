package com.deyvidjgv.inventario.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.deyvidjgv.inventario.InventarioApplication;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.di.AppContainer;
import com.deyvidjgv.inventario.domain.dto.JornadaSummary;
import com.deyvidjgv.inventario.domain.dto.ProductStock;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.model.Jornada;
import com.deyvidjgv.inventario.domain.model.Product;
import com.deyvidjgv.inventario.domain.model.Sale;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

public class SalesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView textTotal;
    private MaterialButton btnVoidLast;
    private AppContainer container;
    private ProductSalesAdapter adapter;

    private Long lastSaleId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        recyclerView = view.findViewById(R.id.recycler_products_sales);
        textTotal = view.findViewById(R.id.text_bottom_total);
        btnVoidLast = view.findViewById(R.id.btn_void_last_sale);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ProductSalesAdapter(new ArrayList<>(), this::onProductClicked, this::onProductLongClicked);
        recyclerView.setAdapter(adapter);

        btnVoidLast.setOnClickListener(v -> voidLastSale());

        loadData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ProductStock> stocks = container.getInventoryService().listStock();
                Optional<Jornada> openOpt = container.getSalesService().getOpenJornada();

                long nightTotal = 0;
                if (openOpt.isPresent()) {
                    JornadaSummary summary = container.getReportService().summary(openOpt.get().getId());
                    nightTotal = summary.getTotalSales();
                }
                final long finalTotal = nightTotal;

                requireActivity().runOnUiThread(() -> {
                    adapter.updateData(stocks);
                    textTotal.setText(CurrencyFormatter.formatCOP(finalTotal));
                });
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private void onProductClicked(Product product) {
        showQuantityDialog(product);
    }

    private void onProductLongClicked(Product product) {
        showQuantityDialog(product);
    }

    private void showQuantityDialog(Product product) {
        Context ctx = getContext();
        if (ctx == null) return;

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextView tvInfo = new TextView(ctx);
        tvInfo.setText("Precio: " + CurrencyFormatter.formatCOP(product.getSalePrice()) + " c/u");
        tvInfo.setTextColor(0xFFB0B0B0);
        tvInfo.setTextSize(15f);
        layout.addView(tvInfo);

        // Fila de contador [-] [input] [+]
        LinearLayout counterRow = new LinearLayout(ctx);
        counterRow.setOrientation(LinearLayout.HORIZONTAL);
        counterRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        counterRow.setPadding(0, 24, 0, 16);

        MaterialButton btnMinus = new MaterialButton(ctx);
        btnMinus.setText("-");
        btnMinus.setTextSize(22f);
        btnMinus.setLayoutParams(new LinearLayout.LayoutParams(140, 140));

        EditText inputQty = new EditText(ctx);
        inputQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputQty.setText("1");
        inputQty.setTextSize(24f);
        inputQty.setTextColor(0xFFFFFFFF);
        inputQty.setGravity(android.view.Gravity.CENTER);
        inputQty.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        inputQty.selectAll();

        MaterialButton btnPlus = new MaterialButton(ctx);
        btnPlus.setText("+");
        btnPlus.setTextSize(22f);
        btnPlus.setLayoutParams(new LinearLayout.LayoutParams(140, 140));

        counterRow.addView(btnMinus);
        counterRow.addView(inputQty);
        counterRow.addView(btnPlus);
        layout.addView(counterRow);

        // Fila de botones de acceso rápido (+1, +2, +5, +6, +10)
        LinearLayout quickRow = new LinearLayout(ctx);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setPadding(0, 0, 0, 16);

        int[] quickAdd = {1, 2, 5, 6, 10};
        for (int add : quickAdd) {
            Button btnQuick = new Button(ctx, null, android.R.attr.borderlessButtonStyle);
            btnQuick.setText("+" + add);
            btnQuick.setTextSize(13f);
            btnQuick.setTextColor(0xFF00E676);
            btnQuick.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            btnQuick.setOnClickListener(v -> {
                int current = 0;
                try {
                    current = Integer.parseInt(inputQty.getText().toString().trim());
                } catch (Exception ignored) {}
                inputQty.setText(String.valueOf(current + add));
            });
            quickRow.addView(btnQuick);
        }
        layout.addView(quickRow);

        // Texto Total Calculado
        TextView tvTotal = new TextView(ctx);
        tvTotal.setText("Total a cobrar: " + CurrencyFormatter.formatCOP(product.getSalePrice()));
        tvTotal.setTextColor(0xFF00E676);
        tvTotal.setTextSize(18f);
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotal.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        tvTotal.setPadding(0, 8, 0, 8);
        layout.addView(tvTotal);

        // Listener para actualizar total en tiempo real
        inputQty.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int q = 0;
                try {
                    q = Integer.parseInt(s.toString().trim());
                } catch (Exception ignored) {}
                tvTotal.setText("Total a cobrar: " + CurrencyFormatter.formatCOP(q * product.getSalePrice()));
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnMinus.setOnClickListener(v -> {
            int q = 1;
            try {
                q = Integer.parseInt(inputQty.getText().toString().trim());
            } catch (Exception ignored) {}
            if (q > 1) {
                inputQty.setText(String.valueOf(q - 1));
            }
        });

        btnPlus.setOnClickListener(v -> {
            int q = 1;
            try {
                q = Integer.parseInt(inputQty.getText().toString().trim());
            } catch (Exception ignored) {}
            inputQty.setText(String.valueOf(q + 1));
        });

        new AlertDialog.Builder(ctx)
                .setTitle("Vender " + product.getName())
                .setView(layout)
                .setPositiveButton("Vender", (dialog, which) -> {
                    String str = inputQty.getText().toString().trim();
                    if (!str.isEmpty()) {
                        try {
                            int qty = Integer.parseInt(str);
                            if (qty > 0) {
                                executeSale(product, qty);
                            } else {
                                Toast.makeText(getContext(), "La cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Por favor ingresa un número válido", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executeSale(Product product, int quantity) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Sale sale = container.getSalesService().sell(product.getId(), quantity);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "✓ +" + quantity + " " + product.getName() + " (" + CurrencyFormatter.formatCOP(sale.getTotal()) + ")", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void voidLastSale() {
        Executors.newSingleThreadExecutor().execute(() -> {
            Optional<Jornada> openOpt = container.getSalesService().getOpenJornada();
            if (!openOpt.isPresent()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "No hay una jornada abierta actualmente", Toast.LENGTH_SHORT).show());
                return;
            }

            long jornadaId = openOpt.get().getId();
            Optional<Sale> lastSaleOpt = container.getSalesService().getLastNonVoidedSale(jornadaId);

            if (!lastSaleOpt.isPresent()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "No hay ventas activas para anular en esta sesión", Toast.LENGTH_SHORT).show());
                return;
            }

            Sale lastSale = lastSaleOpt.get();
            Product product = container.getInventoryService().getProduct(lastSale.getProductId()).orElse(null);
            String prodName = product != null ? product.getName() : "Producto #" + lastSale.getProductId();

            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
                    .withZone(java.time.ZoneId.of("America/Bogota"));
            String timeStr = timeFmt.format(lastSale.getCreatedAt());

            requireActivity().runOnUiThread(() -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Anular Última Venta")
                        .setMessage("¿Deseas anular la última venta de esta sesión?\n\n" +
                                "• Producto: " + prodName + "\n" +
                                "• Cantidad: " + lastSale.getQuantity() + " unidades\n" +
                                "• Total: " + CurrencyFormatter.formatCOP(lastSale.getTotal()) + "\n" +
                                "• Hora: " + timeStr + "\n\n" +
                                "Las unidades regresarán inmediatamente al inventario en sus lotes originales.")
                        .setPositiveButton("Sí, Anular Venta", (d, w) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                try {
                                    container.getSalesService().voidSale(lastSale.getId());
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Venta anulada. +" + lastSale.getQuantity() + " " + prodName + " restauradas al stock.", Toast.LENGTH_LONG).show();
                                        loadData();
                                    });
                                } catch (DomainException e) {
                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                                }
                            });
                        })
                        .setNeutralButton("Ver todas de la sesión", (d, w) -> showSessionSalesDialog(jornadaId))
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        });
    }

    private void showSessionSalesDialog(long jornadaId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Sale> allSales = container.getSalesService().getSalesForJornada(jornadaId);
            List<Sale> activeSales = new ArrayList<>();
            for (Sale s : allSales) {
                if (!s.isVoided()) {
                    activeSales.add(s);
                }
            }

            activeSales.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            if (activeSales.isEmpty()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "No hay ventas activas en esta sesión", Toast.LENGTH_SHORT).show());
                return;
            }

            List<ProductStock> stocks = container.getInventoryService().listStock();
            java.util.Map<Long, String> namesById = new java.util.HashMap<>();
            for (ProductStock ps : stocks) {
                namesById.put(ps.getProduct().getId(), ps.getProduct().getName());
            }

            java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
                    .withZone(java.time.ZoneId.of("America/Bogota"));

            CharSequence[] items = new CharSequence[activeSales.size()];
            for (int i = 0; i < activeSales.size(); i++) {
                Sale s = activeSales.get(i);
                String pName = namesById.getOrDefault(s.getProductId(), "Producto #" + s.getProductId());
                items[i] = pName + " x" + s.getQuantity() + " — " + CurrencyFormatter.formatCOP(s.getTotal()) + " (" + timeFmt.format(s.getCreatedAt()) + ")";
            }

            requireActivity().runOnUiThread(() -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Selecciona la venta a anular")
                        .setItems(items, (dialog, which) -> {
                            Sale selected = activeSales.get(which);
                            String pName = namesById.getOrDefault(selected.getProductId(), "Producto #" + selected.getProductId());
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Confirmar Anulación")
                                    .setMessage("¿Anular la venta de " + selected.getQuantity() + " " + pName + " por " + CurrencyFormatter.formatCOP(selected.getTotal()) + "?")
                                    .setPositiveButton("Sí, Anular", (d, w) -> {
                                        Executors.newSingleThreadExecutor().execute(() -> {
                                            try {
                                                container.getSalesService().voidSale(selected.getId());
                                                requireActivity().runOnUiThread(() -> {
                                                    Toast.makeText(getContext(), "Venta de " + pName + " anulada con éxito.", Toast.LENGTH_SHORT).show();
                                                    loadData();
                                                });
                                            } catch (DomainException e) {
                                                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                                            }
                                        });
                                    })
                                    .setNegativeButton("Cancelar", null)
                                    .show();
                        })
                        .setNegativeButton("Cerrar", null)
                        .show();
            });
        });
    }

    private static class ProductSalesAdapter extends RecyclerView.Adapter<ProductSalesAdapter.ViewHolder> {
        private final List<ProductStock> items;
        private final java.util.function.Consumer<Product> onClick;
        private final java.util.function.Consumer<Product> onLongClick;

        ProductSalesAdapter(List<ProductStock> items,
                            java.util.function.Consumer<Product> onClick,
                            java.util.function.Consumer<Product> onLongClick) {
            this.items = items;
            this.onClick = onClick;
            this.onLongClick = onLongClick;
        }

        void updateData(List<ProductStock> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MaterialButton btn = new MaterialButton(parent.getContext());
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 170);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);
            btn.setTextSize(15f);
            btn.setCornerRadius(16);
            return new ViewHolder(btn);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductStock item = items.get(position);
            Product p = item.getProduct();
            MaterialButton btn = (MaterialButton) holder.itemView;

            String stockLabel = p.isTracksStock() ? " (Stock: " + item.getCurrentStock() + ")" : " (Pool/Juego)";
            btn.setText(p.getName() + "\n" + CurrencyFormatter.formatCOP(p.getSalePrice()) + stockLabel);

            if (p.isTracksStock() && item.getCurrentStock() <= 0) {
                btn.setBackgroundColor(0xFF333333);
                btn.setTextColor(0xFF888888);
            } else {
                btn.setBackgroundColor(0xFF1E1E1E);
                btn.setTextColor(0xFFFFFFFF);
            }

            btn.setOnClickListener(v -> onClick.accept(p));
            btn.setOnLongClickListener(v -> {
                onLongClick.accept(p);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
