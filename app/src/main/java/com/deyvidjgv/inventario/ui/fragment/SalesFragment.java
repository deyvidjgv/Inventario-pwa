package com.deyvidjgv.inventario.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
        executeSale(product, 1);
    }

    private void onProductLongClicked(Product product) {
        EditText input = new EditText(getContext());
        input.setHint("Cantidad a vender");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText("1");
        input.selectAll();

        new AlertDialog.Builder(getContext())
                .setTitle("Vender " + product.getName())
                .setMessage("Ingresa la cantidad:")
                .setView(input)
                .setPositiveButton("Vender", (dialog, which) -> {
                    String str = input.getText().toString().trim();
                    if (!str.isEmpty()) {
                        int qty = Integer.parseInt(str);
                        if (qty > 0) {
                            executeSale(product, qty);
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
                lastSaleId = sale.getId();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "+" + quantity + " " + product.getName() + " (" + CurrencyFormatter.formatCOP(sale.getTotal()) + ")", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void voidLastSale() {
        if (lastSaleId == null) {
            Toast.makeText(getContext(), "No hay una venta reciente para anular en esta sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Anular Venta")
                .setMessage("¿Deseas anular la última venta? Las unidades regresarán inmediatamente a sus lotes originales.")
                .setPositiveButton("Anular", (d, w) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            container.getSalesService().voidSale(lastSaleId);
                            lastSaleId = null;
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Venta anulada. El stock volvió a su lote.", Toast.LENGTH_LONG).show();
                                loadData();
                            });
                        } catch (DomainException e) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
