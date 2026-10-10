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
import com.deyvidjgv.inventario.domain.model.Expense;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ExpensesFragment extends Fragment {

    private RecyclerView recyclerView;
    private MaterialButton btnAddExpense;
    private AppContainer container;
    private ExpenseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        recyclerView = view.findViewById(R.id.recycler_expenses);
        btnAddExpense = view.findViewById(R.id.btn_add_expense);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExpenseAdapter(new ArrayList<>(), this::deleteExpense);
        recyclerView.setAdapter(adapter);

        btnAddExpense.setOnClickListener(v -> showAddExpenseDialog());

        loadExpenses();
        return view;
    }

    private void loadExpenses() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                LocalDate today = LocalDate.now();
                List<Expense> list = container.getExpenseService().list(today.minusDays(30), today);
                requireActivity().runOnUiThread(() -> adapter.updateData(list));
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private void showAddExpenseDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        EditText inputConcept = new EditText(getContext());
        inputConcept.setHint("Concepto (e.g. Hielo, Bolsas, Luz)");
        layout.addView(inputConcept);

        EditText inputAmount = new EditText(getContext());
        inputAmount.setHint("Monto (COP)");
        inputAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputAmount);

        new AlertDialog.Builder(getContext())
                .setTitle("Registrar Gasto Libre")
                .setView(layout)
                .setPositiveButton("Registrar", (d, w) -> {
                    String concept = inputConcept.getText().toString().trim();
                    String amtStr = inputAmount.getText().toString().trim();
                    if (!concept.isEmpty() && !amtStr.isEmpty()) {
                        long amount;
                        try {
                            amount = Long.parseLong(amtStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Por favor ingresa un monto válido", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Executors.newSingleThreadExecutor().execute(() -> {
                            java.util.Optional<com.deyvidjgv.inventario.domain.model.Jornada> openOpt = container.getSalesService().getOpenJornada();
                            Long jId = openOpt.map(com.deyvidjgv.inventario.domain.model.Jornada::getId).orElse(null);
                            container.getExpenseService().add(concept, amount, Instant.now(), jId);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Gasto registrado: " + concept, Toast.LENGTH_SHORT).show();
                                loadExpenses();
                            });
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteExpense(Expense expense) {
        Executors.newSingleThreadExecutor().execute(() -> {
            container.getExpenseService().delete(expense.getId());
            requireActivity().runOnUiThread(this::loadExpenses);
        });
    }

    private static class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {
        private final List<Expense> items;
        private final java.util.function.Consumer<Expense> onDelete;

        ExpenseAdapter(List<Expense> items, java.util.function.Consumer<Expense> onDelete) {
            this.items = items;
            this.onDelete = onDelete;
        }

        void updateData(List<Expense> newItems) {
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
            Expense e = items.get(position);
            LinearLayout layout = (LinearLayout) holder.itemView;
            layout.removeAllViews();

            TextView tv = new TextView(layout.getContext());
            String shiftLabel = e.getJornadaId() != null ? " [Turno #" + e.getJornadaId() + "]" : "";
            tv.setText(e.getConcept() + " — " + CurrencyFormatter.formatCOP(e.getAmount()) + shiftLabel);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(16f);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            layout.addView(tv, p);

            Button btn = new Button(layout.getContext());
            btn.setText("Eliminar");
            btn.setTextSize(12f);
            btn.setOnClickListener(v -> onDelete.accept(e));
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
