package com.deyvidjgv.inventario.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.deyvidjgv.inventario.InventarioApplication;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.di.AppContainer;
import com.deyvidjgv.inventario.domain.dto.ProductStock;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.model.Product;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class InventoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private MaterialButton btnReceiveStock;
    private MaterialButton btnAddProduct;
    private AppContainer container;
    private InventoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inventory, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        recyclerView = view.findViewById(R.id.recycler_inventory);
        btnReceiveStock = view.findViewById(R.id.btn_receive_stock);
        btnAddProduct = view.findViewById(R.id.btn_add_product);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InventoryAdapter(new ArrayList<>(), this::archiveProduct);
        recyclerView.setAdapter(adapter);

        btnReceiveStock.setOnClickListener(v -> showReceiveStockDialog());
        btnAddProduct.setOnClickListener(v -> showAddProductDialog());

        loadInventory();
        return view;
    }

    private void loadInventory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ProductStock> list = container.getInventoryService().listStock();
                requireActivity().runOnUiThread(() -> adapter.updateData(list));
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private void showAddProductDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        EditText inputName = new EditText(getContext());
        inputName.setHint("Nombre del producto");
        layout.addView(inputName);

        EditText inputPrice = new EditText(getContext());
        inputPrice.setHint("Precio de venta (COP)");
        inputPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputPrice);

        CheckBox checkStock = new CheckBox(getContext());
        checkStock.setText("Lleva inventario físico");
        checkStock.setChecked(true);
        layout.addView(checkStock);

        new AlertDialog.Builder(getContext())
                .setTitle("Nuevo Producto")
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    String name = inputName.getText().toString().trim();
                    String priceStr = inputPrice.getText().toString().trim();
                    if (!name.isEmpty() && !priceStr.isEmpty()) {
                        long price = Long.parseLong(priceStr);
                        boolean tracks = checkStock.isChecked();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                container.getInventoryService().createProduct(name, 1L, price, tracks);
                                requireActivity().runOnUiThread(this::loadInventory);
                            } catch (DomainException e) {
                                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showReceiveStockDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ProductStock> stocks = container.getInventoryService().listStock();
            requireActivity().runOnUiThread(() -> {
                if (stocks.isEmpty()) {
                    Toast.makeText(getContext(), "Crea un producto primero", Toast.LENGTH_SHORT).show();
                    return;
                }

                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(32, 16, 32, 16);

                Spinner spinner = new Spinner(getContext());
                List<String> names = new ArrayList<>();
                for (ProductStock s : stocks) names.add(s.getProduct().getName());
                ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, names);
                spinner.setAdapter(spinAdapter);
                layout.addView(spinner);

                EditText inputQty = new EditText(getContext());
                inputQty.setHint("Cantidad comprada (unidades)");
                inputQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                layout.addView(inputQty);

                EditText inputCost = new EditText(getContext());
                inputCost.setHint("Precio de compra unitario (COP)");
                inputCost.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                layout.addView(inputCost);

                new AlertDialog.Builder(getContext())
                        .setTitle("Registrar Entrada de Mercancía")
                        .setView(layout)
                        .setPositiveButton("Registrar Entrada", (d, w) -> {
                            int selectedIdx = spinner.getSelectedItemPosition();
                            Product selectedProduct = stocks.get(selectedIdx).getProduct();
                            String qtyStr = inputQty.getText().toString().trim();
                            String costStr = inputCost.getText().toString().trim();

                            if (!qtyStr.isEmpty() && !costStr.isEmpty()) {
                                int qty = Integer.parseInt(qtyStr);
                                long cost = Long.parseLong(costStr);
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    try {
                                        container.getInventoryService().receiveStock(selectedProduct.getId(), qty, cost, Instant.now(), "Compra");
                                        requireActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "Entrada registrada exitosamente", Toast.LENGTH_SHORT).show();
                                            loadInventory();
                                        });
                                    } catch (DomainException e) {
                                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        });
    }

    private void archiveProduct(Product p) {
        new AlertDialog.Builder(getContext())
                .setTitle("Archivar Producto")
                .setMessage("¿Deseas archivar " + p.getName() + "? (No se borrarán sus ventas históricas)")
                .setPositiveButton("Archivar", (d, w) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        container.getInventoryService().archiveProduct(p.getId());
                        requireActivity().runOnUiThread(this::loadInventory);
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
        private final List<ProductStock> items;
        private final java.util.function.Consumer<Product> onArchive;

        InventoryAdapter(List<ProductStock> items, java.util.function.Consumer<Product> onArchive) {
            this.items = items;
            this.onArchive = onArchive;
        }

        void updateData(List<ProductStock> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout l = new LinearLayout(parent.getContext());
            l.setOrientation(LinearLayout.HORIZONTAL);
            l.setPadding(16, 16, 16, 16);
            l.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(l);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductStock item = items.get(position);
            Product p = item.getProduct();
            LinearLayout layout = (LinearLayout) holder.itemView;
            layout.removeAllViews();

            TextView tv = new TextView(layout.getContext());
            tv.setText(p.getName() + "\nPrecio: " + CurrencyFormatter.formatCOP(p.getSalePrice()) +
                    (p.isTracksStock() ? " | Stock: " + item.getCurrentStock() : " | (Servicio/Pool)"));
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(16f);
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            layout.addView(tv, p1);

            Button btn = new Button(layout.getContext());
            btn.setText("Archivar");
            btn.setTextSize(12f);
            btn.setOnClickListener(v -> onArchive.accept(p));
            layout.addView(btn);
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
