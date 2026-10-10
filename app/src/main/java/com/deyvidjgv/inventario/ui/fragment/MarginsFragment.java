package com.deyvidjgv.inventario.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.deyvidjgv.inventario.InventarioApplication;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.di.AppContainer;
import com.deyvidjgv.inventario.domain.dto.LotMargin;
import com.deyvidjgv.inventario.domain.dto.ProductMarginReport;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MarginsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppContainer container;
    private MarginsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_margins, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        recyclerView = view.findViewById(R.id.recycler_margins);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MarginsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadMargins();
        return view;
    }

    private void loadMargins() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ProductMarginReport> reports = container.getReportService().allMargins();
                requireActivity().runOnUiThread(() -> adapter.updateData(reports));
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private static class MarginsAdapter extends RecyclerView.Adapter<MarginsAdapter.ViewHolder> {
        private final List<ProductMarginReport> items;

        MarginsAdapter(List<ProductMarginReport> items) {
            this.items = items;
        }

        void updateData(List<ProductMarginReport> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout l = new LinearLayout(parent.getContext());
            l.setOrientation(LinearLayout.VERTICAL);
            l.setPadding(24, 20, 24, 20);
            l.setBackgroundColor(0xFF1E1E1E);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            l.setLayoutParams(params);
            return new ViewHolder(l);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductMarginReport r = items.get(position);
            LinearLayout layout = (LinearLayout) holder.itemView;
            layout.removeAllViews();

            TextView title = new TextView(layout.getContext());
            title.setText(r.getProductName() + " — Venta: " + CurrencyFormatter.formatCOP(r.getCurrentSalePrice()));
            title.setTextColor(0xFF00E676);
            title.setTextSize(18f);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            layout.addView(title);

            TextView diff = new TextView(layout.getContext());
            diff.setText("Ganancia Real: " + CurrencyFormatter.formatCOP(r.getRealProfit()) +
                    "\nCon Costo Nuevo: " + CurrencyFormatter.formatCOP(r.getProfitWithNewCost()) +
                    "\nDiferencia (Dejó de ganar): " + CurrencyFormatter.formatCOP(r.getDifference()));
            diff.setTextColor(0xFFFF9100);
            diff.setTextSize(14f);
            diff.setPadding(0, 8, 0, 8);
            layout.addView(diff);

            for (LotMargin lm : r.getLotMargins()) {
                TextView lotTv = new TextView(layout.getContext());
                String costMarginStr = lm.getMarginOnCost() != null ? CurrencyFormatter.formatPercent(lm.getMarginOnCost()) : "N/A";
                lotTv.setText("  • Lote #" + lm.getLotId() + " (Costo: " + CurrencyFormatter.formatCOP(lm.getUnitCost()) +
                        " | Quedan: " + lm.getQuantityRemaining() + ")\n" +
                        "    Margen s/Venta: " + CurrencyFormatter.formatPercent(lm.getMarginOnSale()) +
                        " | Margen s/Costo: " + costMarginStr);
                lotTv.setTextColor(0xFFB0B0B0);
                lotTv.setTextSize(13f);
                layout.addView(lotTv);
            }
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
