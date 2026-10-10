package com.deyvidjgv.inventario.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MarginsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppContainer container;
    private VisualMarginsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_margins, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        recyclerView = view.findViewById(R.id.recycler_margins);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new VisualMarginsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadMargins();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMargins();
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

    private static class VisualMarginsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private final List<ProductMarginReport> items;

        VisualMarginsAdapter(List<ProductMarginReport> items) {
            this.items = items;
        }

        void updateData(List<ProductMarginReport> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public int getItemCount() {
            return items.isEmpty() ? 0 : items.size() + 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                MaterialCardView card = new MaterialCardView(parent.getContext());
                card.setCardBackgroundColor(0xFF232B2B);
                card.setRadius(20f);
                card.setCardElevation(6f);
                card.setStrokeColor(0xFF00E676);
                card.setStrokeWidth(2);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(12, 12, 12, 16);
                card.setLayoutParams(params);

                LinearLayout layout = new LinearLayout(parent.getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(24, 20, 24, 20);
                card.addView(layout);
                return new HeaderViewHolder(card);
            } else {
                MaterialCardView card = new MaterialCardView(parent.getContext());
                card.setCardBackgroundColor(0xFF1E1E1E);
                card.setRadius(16f);
                card.setCardElevation(3f);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(12, 8, 12, 12);
                card.setLayoutParams(params);

                LinearLayout layout = new LinearLayout(parent.getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(24, 20, 24, 20);
                card.addView(layout);
                return new ItemViewHolder(card);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder rawHolder, int position) {
            if (rawHolder instanceof HeaderViewHolder) {
                HeaderViewHolder holder = (HeaderViewHolder) rawHolder;
                long totalReal = 0;
                long totalNew = 0;
                long totalDiff = 0;
                for (ProductMarginReport r : items) {
                    totalReal += r.getRealProfit();
                    totalNew += r.getProfitWithNewCost();
                    totalDiff += r.getDifference();
                }

                LinearLayout l = (LinearLayout) ((MaterialCardView) holder.itemView).getChildAt(0);
                l.removeAllViews();

                TextView title = new TextView(l.getContext());
                title.setText("📊 Balance Global de Ganancias y Márgenes");
                title.setTextColor(0xFF00E676);
                title.setTextSize(18f);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                l.addView(title);

                TextView tvStats = new TextView(l.getContext());
                tvStats.setText("• Ganancia Real Obtenida: " + CurrencyFormatter.formatCOP(totalReal) +
                        "\n• Con Precios de Compra Nuevos: " + CurrencyFormatter.formatCOP(totalNew) +
                        "\n• Diferencial de Compra: " + CurrencyFormatter.formatCOP(totalDiff));
                tvStats.setTextColor(0xFFFFFFFF);
                tvStats.setTextSize(15f);
                tvStats.setPadding(0, 10, 0, 10);
                l.addView(tvStats);

                if (totalDiff > 0) {
                    TextView notice = new TextView(l.getContext());
                    notice.setText("💡 Dejaste de ganar " + CurrencyFormatter.formatCOP(totalDiff) + " por haber vendido unidades que compraste más caras en lotes antiguos.");
                    notice.setTextColor(0xFFFF9100);
                    notice.setTextSize(13f);
                    notice.setTypeface(null, android.graphics.Typeface.ITALIC);
                    l.addView(notice);
                }
            } else {
                ItemViewHolder holder = (ItemViewHolder) rawHolder;
                ProductMarginReport r = items.get(position - 1);
                LinearLayout l = (LinearLayout) ((MaterialCardView) holder.itemView).getChildAt(0);
                l.removeAllViews();

                // Cabecera Producto
                LinearLayout pHeader = new LinearLayout(l.getContext());
                pHeader.setOrientation(LinearLayout.HORIZONTAL);

                TextView name = new TextView(l.getContext());
                name.setText(r.getProductName());
                name.setTextColor(0xFFFFFFFF);
                name.setTextSize(18f);
                name.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                pHeader.addView(name, p1);

                TextView price = new TextView(l.getContext());
                price.setText("Venta: " + CurrencyFormatter.formatCOP(r.getCurrentSalePrice()));
                price.setTextColor(0xFF00E676);
                price.setTextSize(16f);
                price.setTypeface(null, android.graphics.Typeface.BOLD);
                pHeader.addView(price);

                l.addView(pHeader);

                // Cuadro Comparativo del Producto
                MaterialCardView compCard = new MaterialCardView(l.getContext());
                compCard.setCardBackgroundColor(0xFF282828);
                compCard.setRadius(12f);
                ViewGroup.MarginLayoutParams compParams = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                compParams.setMargins(0, 12, 0, 12);
                compCard.setLayoutParams(compParams);

                LinearLayout compLayout = new LinearLayout(l.getContext());
                compLayout.setOrientation(LinearLayout.VERTICAL);
                compLayout.setPadding(16, 12, 16, 12);

                TextView compText = new TextView(l.getContext());
                compText.setText("Ganancia Real: " + CurrencyFormatter.formatCOP(r.getRealProfit()) +
                        " | Si fuera a costo nuevo: " + CurrencyFormatter.formatCOP(r.getProfitWithNewCost()) +
                        "\nDiferencia: " + CurrencyFormatter.formatCOP(r.getDifference()));
                compText.setTextColor(0xFFFF9100);
                compText.setTextSize(14f);
                compLayout.addView(compText);

                compCard.addView(compLayout);
                l.addView(compCard);

                // Desglose de Lotes con Gráficos de Barra
                if (r.getLotMargins().isEmpty()) {
                    TextView noLots = new TextView(l.getContext());
                    noLots.setText("Sin lotes activos en stock");
                    noLots.setTextColor(0xFFB0B0B0);
                    noLots.setTextSize(13f);
                    l.addView(noLots);
                } else {
                    TextView lotsTitle = new TextView(l.getContext());
                    lotsTitle.setText("Lotes Activos y Rentabilidad:");
                    lotsTitle.setTextColor(0xFFB0B0B0);
                    lotsTitle.setTextSize(14f);
                    lotsTitle.setPadding(0, 4, 0, 8);
                    l.addView(lotsTitle);

                    for (LotMargin lm : r.getLotMargins()) {
                        LinearLayout lotBox = new LinearLayout(l.getContext());
                        lotBox.setOrientation(LinearLayout.VERTICAL);
                        lotBox.setPadding(8, 8, 8, 8);
                        lotBox.setBackgroundColor(0xFF242424);
                        ViewGroup.MarginLayoutParams lotParams = new ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lotParams.setMargins(0, 0, 0, 8);
                        lotBox.setLayoutParams(lotParams);

                        // Fila info lote
                        TextView lotInfo = new TextView(l.getContext());
                        lotInfo.setText("Lote #" + lm.getLotId() + " (Costo: " + CurrencyFormatter.formatCOP(lm.getUnitCost()) +
                                " | Quedan: " + lm.getQuantityRemaining() + " u.) — Ganancia: " + CurrencyFormatter.formatCOP(lm.getProfitPerUnit()) + "/u");
                        lotInfo.setTextColor(0xFFFFFFFF);
                        lotInfo.setTextSize(13f);
                        lotBox.addView(lotInfo);

                        // Barra gráfica visual de margen sobre venta
                        LinearLayout barRow = new LinearLayout(l.getContext());
                        barRow.setOrientation(LinearLayout.HORIZONTAL);
                        barRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        barRow.setPadding(0, 6, 0, 2);

                        ProgressBar bar = new ProgressBar(l.getContext(), null, android.R.attr.progressBarStyleHorizontal);
                        bar.setMax(100);
                        bar.setProgress((int) Math.min(100, Math.max(0, lm.getMarginOnSale())));
                        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(0, 24, 1f);
                        barParams.setMarginEnd(12);
                        bar.setLayoutParams(barParams);
                        barRow.addView(bar);

                        TextView percLabel = new TextView(l.getContext());
                        percLabel.setText(CurrencyFormatter.formatPercent(lm.getMarginOnSale()) + " s/Venta");
                        percLabel.setTextColor(0xFF00E676);
                        percLabel.setTextSize(13f);
                        percLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                        barRow.addView(percLabel);

                        lotBox.addView(barRow);

                        // Margen sobre costo
                        if (lm.getMarginOnCost() != null) {
                            TextView costMarginTv = new TextView(l.getContext());
                            costMarginTv.setText("Rentabilidad sobre costo: " + CurrencyFormatter.formatPercent(lm.getMarginOnCost()));
                            costMarginTv.setTextColor(0xFF00B0FF);
                            costMarginTv.setTextSize(12f);
                            lotBox.addView(costMarginTv);
                        }

                        l.addView(lotBox);
                    }
                }
            }
        }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            ItemViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
