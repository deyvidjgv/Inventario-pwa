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
import com.deyvidjgv.inventario.domain.model.Category;
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
        adapter = new InventoryAdapter(new ArrayList<>(), this::archiveProduct, this::showDirectReceiveDialog);
        recyclerView.setAdapter(adapter);

        btnReceiveStock.setOnClickListener(v -> showReceiveStockDialog());
        btnAddProduct.setOnClickListener(v -> showAddProductDialog());

        loadInventory();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadInventory();
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
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Category> categories = container.getInventoryService().listCategories();
            if (categories.isEmpty()) {
                Category cat = container.getInventoryService().createCategory("General");
                categories = new ArrayList<>();
                categories.add(cat);
            }
            final List<Category> finalCategories = categories;

            requireActivity().runOnUiThread(() -> {
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(32, 16, 32, 16);

                EditText inputName = new EditText(getContext());
                inputName.setHint("Nombre (ej. Cerveza Águila, Chitos)");
                layout.addView(inputName);

                TextView catLabel = new TextView(getContext());
                catLabel.setText("Categoría:");
                catLabel.setTextColor(0xFFB0B0B0);
                catLabel.setPadding(0, 12, 0, 4);
                layout.addView(catLabel);

                Spinner spinnerCategory = new Spinner(getContext());
                List<String> catNames = new ArrayList<>();
                for (Category c : finalCategories) catNames.add(c.getName());
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, catNames);
                spinnerCategory.setAdapter(catAdapter);
                layout.addView(spinnerCategory);

                EditText inputPrice = new EditText(getContext());
                inputPrice.setHint("Precio de venta al público (COP)");
                inputPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                layout.addView(inputPrice);

                CheckBox checkStock = new CheckBox(getContext());
                checkStock.setText("Lleva control de inventario (desmarcar si es servicio/juego)");
                checkStock.setChecked(true);
                checkStock.setTextColor(0xFFFFFFFF);
                checkStock.setPadding(0, 8, 0, 8);
                layout.addView(checkStock);

                new AlertDialog.Builder(getContext())
                        .setTitle("Crear Nuevo Producto")
                        .setView(layout)
                        .setPositiveButton("Crear Producto", (d, w) -> {
                            String name = inputName.getText().toString().trim();
                            String priceStr = inputPrice.getText().toString().trim();
                            if (name.isEmpty() || priceStr.isEmpty()) {
                                Toast.makeText(getContext(), "Por favor ingresa nombre y precio", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            long price = Long.parseLong(priceStr);
                            boolean tracks = checkStock.isChecked();
                            int catIdx = spinnerCategory.getSelectedItemPosition();
                            long catId = finalCategories.get(catIdx).getId();

                            Executors.newSingleThreadExecutor().execute(() -> {
                                try {
                                    container.getInventoryService().createProduct(name, catId, price, tracks);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "Producto creado: " + name, Toast.LENGTH_SHORT).show();
                                        loadInventory();
                                    });
                                } catch (DomainException e) {
                                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                                }
                            });
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        });
    }

    private void showDirectReceiveDialog(Product product) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        TextView pTitle = new TextView(getContext());
        pTitle.setText("Producto: " + product.getName() + "\nPrecio venta actual: " + CurrencyFormatter.formatCOP(product.getSalePrice()));
        pTitle.setTextColor(0xFF00E676);
        pTitle.setTextSize(16f);
        pTitle.setPadding(0, 0, 0, 12);
        layout.addView(pTitle);

        EditText inputQty = new EditText(getContext());
        inputQty.setHint("Cantidad comprada (ej. 24)");
        inputQty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputQty);

        EditText inputCost = new EditText(getContext());
        inputCost.setHint("Precio de compra unitario en COP (ej. 4300)");
        inputCost.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputCost);

        new AlertDialog.Builder(getContext())
                .setTitle("Registrar Entrada de Lote")
                .setView(layout)
                .setPositiveButton("Registrar Entrada", (d, w) -> {
                    String qtyStr = inputQty.getText().toString().trim();
                    String costStr = inputCost.getText().toString().trim();
                    if (!qtyStr.isEmpty() && !costStr.isEmpty()) {
                        int qty = Integer.parseInt(qtyStr);
                        long cost = Long.parseLong(costStr);
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                container.getInventoryService().receiveStock(product.getId(), qty, cost, Instant.now(), "Compra");
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Lote de " + qty + " u. ingresado a costo " + CurrencyFormatter.formatCOP(cost), Toast.LENGTH_LONG).show();
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
    }

    private void showReceiveStockDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ProductStock> stocks = container.getInventoryService().listStock();
            requireActivity().runOnUiThread(() -> {
                if (stocks.isEmpty()) {
                    Toast.makeText(getContext(), "Primero debes crear al menos un producto con el botón '+ Nuevo Producto'", Toast.LENGTH_LONG).show();
                    return;
                }

                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(32, 16, 32, 16);

                Spinner spinner = new Spinner(getContext());
                List<String> names = new ArrayList<>();
                for (ProductStock s : stocks) {
                    names.add(s.getProduct().getName() + " (Stock: " + s.getCurrentStock() + ")");
                }
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
                        .setTitle("Entrada de Mercancía")
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
        private final java.util.function.Consumer<Product> onAddStock;

        InventoryAdapter(List<ProductStock> items,
                         java.util.function.Consumer<Product> onArchive,
                         java.util.function.Consumer<Product> onAddStock) {
            this.items = items;
            this.onArchive = onArchive;
            this.onAddStock = onAddStock;
        }

        void updateData(List<ProductStock> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(parent.getContext());
            card.setCardBackgroundColor(0xFF1E1E1E);
            card.setRadius(16f);
            card.setCardElevation(4f);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);

            LinearLayout l = new LinearLayout(parent.getContext());
            l.setOrientation(LinearLayout.VERTICAL);
            l.setPadding(24, 20, 24, 20);
            card.addView(l);

            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductStock item = items.get(position);
            Product p = item.getProduct();
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) holder.itemView;
            LinearLayout layout = (LinearLayout) card.getChildAt(0);
            layout.removeAllViews();

            // Cabecera: Nombre y Categoría
            LinearLayout header = new LinearLayout(card.getContext());
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvName = new TextView(card.getContext());
            tvName.setText(p.getName());
            tvName.setTextColor(0xFFFFFFFF);
            tvName.setTextSize(18f);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams pName = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            header.addView(tvName, pName);

            TextView tvCategory = new TextView(card.getContext());
            tvCategory.setText(item.getCategoryName());
            tvCategory.setTextColor(0xFF00E676);
            tvCategory.setTextSize(13f);
            header.addView(tvCategory);

            layout.addView(header);

            // Detalles de stock y precio
            TextView tvDetails = new TextView(card.getContext());
            String stockText = p.isTracksStock() ?
                    "Stock disponible: " + item.getCurrentStock() + " unidades" :
                    "Producto de servicio (Sin inventario físico)";
            int stockColor = !p.isTracksStock() ? 0xFF00B0FF : (item.getCurrentStock() > 0 ? 0xFF00E676 : 0xFFFF5252);

            tvDetails.setText("Precio venta: " + CurrencyFormatter.formatCOP(p.getSalePrice()) + "\n" + stockText);
            tvDetails.setTextColor(stockColor);
            tvDetails.setTextSize(15f);
            tvDetails.setPadding(0, 8, 0, 12);
            layout.addView(tvDetails);

            // Botones de acción
            LinearLayout actions = new LinearLayout(card.getContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);

            if (p.isTracksStock()) {
                com.google.android.material.button.MaterialButton btnAddStock = new com.google.android.material.button.MaterialButton(card.getContext());
                btnAddStock.setText("+ Entrar Stock");
                btnAddStock.setTextSize(13f);
                btnAddStock.setBackgroundColor(0xFF00E676);
                btnAddStock.setTextColor(0xFF000000);
                btnAddStock.setOnClickListener(v -> onAddStock.accept(p));
                LinearLayout.LayoutParams pBtn = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                pBtn.setMarginEnd(12);
                actions.addView(btnAddStock, pBtn);
            }

            com.google.android.material.button.MaterialButton btnArchive = new com.google.android.material.button.MaterialButton(card.getContext());
            btnArchive.setText("Archivar");
            btnArchive.setTextSize(13f);
            btnArchive.setBackgroundColor(0xFF2C2C2C);
            btnArchive.setTextColor(0xFFB0B0B0);
            btnArchive.setOnClickListener(v -> onArchive.accept(p));
            actions.addView(btnArchive);

            layout.addView(actions);
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
