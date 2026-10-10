package com.deyvidjgv.inventario.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.deyvidjgv.inventario.domain.dto.ProductStock;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.model.Product;
import com.deyvidjgv.inventario.domain.model.Sale;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SalesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView textTotal;
    private MaterialButton btnVoidLast;
    private AppContainer container;
    private ProductSalesAdapter adapter;

    private Long lastSaleId = null;
    private long totalNightSales = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sales, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        recyclerView = view.findViewById(R.id.recycler_products_sales);
        textTotal = view.findViewById(R.id.text_bottom_total);
        btnVoidLast = view.findViewById(R.id.btn_void_last_sale);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ProductSalesAdapter(new ArrayList<>(), this::onProductClicked);
        recyclerView.setAdapter(adapter);

        btnVoidLast.setOnClickListener(v -> voidLastSale());

        loadProducts();
        return view;
    }

    private void loadProducts() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ProductStock> stocks = container.getInventoryService().listStock();
                requireActivity().runOnUiThread(() -> adapter.updateData(stocks));
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private void onProductClicked(Product product) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Sale sale = container.getSalesService().sell(product.getId(), 1);
                lastSaleId = sale.getId();
                totalNightSales += sale.getTotal();
                requireActivity().runOnUiThread(() -> {
                    textTotal.setText(CurrencyFormatter.formatCOP(totalNightSales));
                    Toast.makeText(getContext(), "+1 " + product.getName(), Toast.LENGTH_SHORT).show();
                    loadProducts(); // refrescar stock
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void voidLastSale() {
        if (lastSaleId == null) {
            Toast.makeText(getContext(), "No hay ventas recientes para anular", Toast.LENGTH_SHORT).show();
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                container.getSalesService().voidSale(lastSaleId);
                lastSaleId = null;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Venta anulada correctamente. Stock devuelto a lotes.", Toast.LENGTH_LONG).show();
                    loadProducts();
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private static class ProductSalesAdapter extends RecyclerView.Adapter<ProductSalesAdapter.ViewHolder> {
        private final List<ProductStock> items;
        private final java.util.function.Consumer<Product> onClick;

        ProductSalesAdapter(List<ProductStock> items, java.util.function.Consumer<Product> onClick) {
            this.items = items;
            this.onClick = onClick;
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
                    ViewGroup.LayoutParams.MATCH_PARENT, 160);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);
            btn.setTextSize(16f);
            return new ViewHolder(btn);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductStock item = items.get(position);
            Product p = item.getProduct();
            String stockLabel = p.isTracksStock() ? " (" + item.getCurrentStock() + ")" : " (Pool)";
            ((MaterialButton) holder.itemView).setText(p.getName() + "\n" + CurrencyFormatter.formatCOP(p.getSalePrice()) + stockLabel);
            holder.itemView.setOnClickListener(v -> onClick.accept(p));
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
