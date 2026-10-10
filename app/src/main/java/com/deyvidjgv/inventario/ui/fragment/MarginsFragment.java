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
import com.deyvidjgv.inventario.domain.model.Jornada;
import com.deyvidjgv.inventario.ui.MainActivity;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import com.deyvidjgv.inventario.ui.widget.FinancialChartView;
import com.deyvidjgv.inventario.ui.widget.ProductDistributionChartView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class MarginsFragment extends Fragment {

    private RecyclerView recyclerView;
    private MaterialButtonToggleGroup toggleGroupPeriod;
    private MaterialButton btnFilterShift;
    private MaterialButton btnFilterAll;
    private AppContainer container;
    private VisualMarginsAdapter adapter;

    private boolean isShiftMode = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_margins, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        recyclerView = view.findViewById(R.id.recycler_margins);
        toggleGroupPeriod = view.findViewById(R.id.toggle_group_period);
        btnFilterShift = view.findViewById(R.id.btn_filter_shift);
        btnFilterAll = view.findViewById(R.id.btn_filter_all);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new VisualMarginsAdapter(new ArrayList<>(), "");
        recyclerView.setAdapter(adapter);

        // Preseleccionar Turno por defecto
        toggleGroupPeriod.check(R.id.btn_filter_shift);
        toggleGroupPeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_filter_daily) {
                    ((MainActivity) requireActivity()).loadFragment(new DailyReportFragment(), "Reporte Diario");
                } else {
                    isShiftMode = (checkedId == R.id.btn_filter_shift);
                    loadMargins();
                }
            }
        });

        loadMargins();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMargins();
    }

    private void loadMargins() {
        container.getExecutor().execute(() -> {
            try {
                List<ProductMarginReport> reports;
                String subtitle;

                if (isShiftMode) {
                    Optional<Jornada> openOpt = container.getSalesService().getOpenJornada();
                    Optional<Jornada> closedOpt = container.getSalesService().getLastClosedJornada();

                    if (openOpt.isPresent()) {
                        Jornada j = openOpt.get();
                        reports = container.getReportService().allMarginsForJornada(j.getId());
                        subtitle = "Turno en curso #" + j.getId();
                    } else if (closedOpt.isPresent()) {
                        Jornada j = closedOpt.get();
                        reports = container.getReportService().allMarginsForJornada(j.getId());
                        subtitle = "Último turno cerrado #" + j.getId();
                    } else {
                        reports = container.getReportService().allMargins();
                        subtitle = "Sin turnos registrados (Mostrando Histórico)";
                    }
                } else {
                    reports = container.getReportService().allMargins();
                    subtitle = "Histórico Total Acumulado";
                }

                final List<ProductMarginReport> finalReports = reports;
                final String finalSubtitle = subtitle;
                safeRunOnUiThread(() -> adapter.updateData(finalReports, finalSubtitle));
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private void safeRunOnUiThread(Runnable action) {
        if (isAdded() && getActivity() != null) {
            requireActivity().runOnUiThread(action);
        }
    }

    private static class VisualMarginsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_FINANCIAL_CHART = 0;
        private static final int TYPE_DISTRIBUTION_CHART = 1;
        private static final int TYPE_PRODUCT_ITEM = 2;

        private final List<ProductMarginReport> items;
        private String subtitle;

        VisualMarginsAdapter(List<ProductMarginReport> items, String subtitle) {
            this.items = items;
            this.subtitle = subtitle;
        }

        void updateData(List<ProductMarginReport> newItems, String newSubtitle) {
            items.clear();
            items.addAll(newItems);
            this.subtitle = newSubtitle;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return TYPE_FINANCIAL_CHART;
            if (position == 1) return TYPE_DISTRIBUTION_CHART;
            return TYPE_PRODUCT_ITEM;
        }

        @Override
        public int getItemCount() {
            return items.size() + 2;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_FINANCIAL_CHART) {
                MaterialCardView card = new MaterialCardView(parent.getContext());
                card.setCardBackgroundColor(0xFF1E1E1E);
                card.setRadius(20f);
                card.setCardElevation(6f);
                card.setStrokeColor(0xFF00E676);
                card.setStrokeWidth(2);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(8, 8, 8, 12);
                card.setLayoutParams(params);

                LinearLayout layout = new LinearLayout(parent.getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(20, 20, 20, 20);
                card.addView(layout);
                return new FinancialChartViewHolder(card);
            } else if (viewType == TYPE_DISTRIBUTION_CHART) {
                MaterialCardView card = new MaterialCardView(parent.getContext());
                card.setCardBackgroundColor(0xFF1E1E1E);
                card.setRadius(18f);
                card.setCardElevation(4f);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(8, 6, 8, 16);
                card.setLayoutParams(params);

                LinearLayout layout = new LinearLayout(parent.getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(20, 18, 20, 18);
                card.addView(layout);
                return new DistributionChartViewHolder(card);
            } else {
                MaterialCardView card = new MaterialCardView(parent.getContext());
                card.setCardBackgroundColor(0xFF1E1E1E);
                card.setRadius(14f);
                card.setCardElevation(3f);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(8, 6, 8, 10);
                card.setLayoutParams(params);

                LinearLayout layout = new LinearLayout(parent.getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(20, 18, 20, 18);
                card.addView(layout);
                return new ProductViewHolder(card);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder rawHolder, int position) {
            if (rawHolder instanceof FinancialChartViewHolder) {
                FinancialChartViewHolder holder = (FinancialChartViewHolder) rawHolder;
                long totalSales = 0;
                long totalCost = 0;
                long totalProfit = 0;
                for (ProductMarginReport r : items) {
                    totalSales += r.getTotalSales();
                    totalCost += r.getTotalCost();
                    totalProfit += r.getRealProfit();
                }

                LinearLayout l = (LinearLayout) ((MaterialCardView) holder.itemView).getChildAt(0);
                l.removeAllViews();

                TextView title = new TextView(l.getContext());
                title.setText("📈 Balance General de Ventas y Márgenes");
                title.setTextColor(0xFF00E676);
                title.setTextSize(17f);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                l.addView(title);

                FinancialChartView chartView = new FinancialChartView(l.getContext());
                chartView.setData(totalSales, totalCost, totalProfit, subtitle);
                l.addView(chartView);

                float marginPct = totalSales > 0 ? ((float) totalProfit * 100f / totalSales) : 0f;
                TextView tvSummary = new TextView(l.getContext());
                tvSummary.setText(String.format(java.util.Locale.US,
                        "• Ventas Totales: %s\n• Costo Mercancía: %s\n• Ganancia Real: %s (Margen Global: %.1f%%)",
                        CurrencyFormatter.formatCOP(totalSales),
                        CurrencyFormatter.formatCOP(totalCost),
                        CurrencyFormatter.formatCOP(totalProfit),
                        marginPct));
                tvSummary.setTextColor(0xFFE0E0E0);
                tvSummary.setTextSize(14f);
                tvSummary.setPadding(8, 10, 8, 4);
                l.addView(tvSummary);

            } else if (rawHolder instanceof DistributionChartViewHolder) {
                DistributionChartViewHolder holder = (DistributionChartViewHolder) rawHolder;
                LinearLayout l = (LinearLayout) ((MaterialCardView) holder.itemView).getChildAt(0);
                l.removeAllViews();

                TextView title = new TextView(l.getContext());
                title.setText("📊 Comparativa de Ventas por Producto");
                title.setTextColor(0xFFFFD54F);
                title.setTextSize(16f);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                l.addView(title);

                TextView subtitleView = new TextView(l.getContext());
                subtitleView.setText("Verde: Ganancia | Gris: Costo de Mercancía");
                subtitleView.setTextColor(0xFF9E9E9E);
                subtitleView.setTextSize(12f);
                subtitleView.setPadding(0, 2, 0, 10);
                l.addView(subtitleView);

                ProductDistributionChartView distChart = new ProductDistributionChartView(l.getContext());
                distChart.setData(items);
                l.addView(distChart);

            } else if (rawHolder instanceof ProductViewHolder) {
                ProductViewHolder holder = (ProductViewHolder) rawHolder;
                ProductMarginReport r = items.get(position - 2);
                LinearLayout l = (LinearLayout) ((MaterialCardView) holder.itemView).getChildAt(0);
                l.removeAllViews();

                // Cabecera Producto
                LinearLayout pHeader = new LinearLayout(l.getContext());
                pHeader.setOrientation(LinearLayout.HORIZONTAL);

                TextView name = new TextView(l.getContext());
                name.setText(r.getProductName());
                name.setTextColor(0xFFFFFFFF);
                name.setTextSize(17f);
                name.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                pHeader.addView(name, p1);

                TextView price = new TextView(l.getContext());
                price.setText("Precio: " + CurrencyFormatter.formatCOP(r.getCurrentSalePrice()));
                price.setTextColor(0xFF00E676);
                price.setTextSize(15f);
                price.setTypeface(null, android.graphics.Typeface.BOLD);
                pHeader.addView(price);

                l.addView(pHeader);

                // Cuadro de Ventas y Rendimiento del Producto
                MaterialCardView compCard = new MaterialCardView(l.getContext());
                compCard.setCardBackgroundColor(0xFF262626);
                compCard.setRadius(10f);
                ViewGroup.MarginLayoutParams compParams = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                compParams.setMargins(0, 10, 0, 10);
                compCard.setLayoutParams(compParams);

                LinearLayout compLayout = new LinearLayout(l.getContext());
                compLayout.setOrientation(LinearLayout.VERTICAL);
                compLayout.setPadding(14, 10, 14, 10);

                float marginPct = r.getTotalSales() > 0 ? ((float) r.getRealProfit() * 100f / r.getTotalSales()) : 0f;

                TextView statsText = new TextView(l.getContext());
                statsText.setText(String.format(java.util.Locale.US,
                        "● Cantidad vendida: %d unidades\n● Total vendido: %s\n● Costo de lo vendido: %s\n● Ganancia Neta: %s (Margen: %.1f%%)",
                        r.getUnitsSold(),
                        CurrencyFormatter.formatCOP(r.getTotalSales()),
                        CurrencyFormatter.formatCOP(r.getTotalCost()),
                        CurrencyFormatter.formatCOP(r.getRealProfit()),
                        marginPct));
                statsText.setTextColor(r.getUnitsSold() > 0 ? 0xFFFFFFFF : 0xFF9E9E9E);
                statsText.setTextSize(14f);
                statsText.setLineSpacing(4f, 1.1f);
                compLayout.addView(statsText);

                compCard.addView(compLayout);
                l.addView(compCard);

                // Lotes activos en stock
                if (r.getLotMargins().isEmpty()) {
                    TextView noLots = new TextView(l.getContext());
                    noLots.setText("Sin lotes activos en inventario");
                    noLots.setTextColor(0xFF757575);
                    noLots.setTextSize(12f);
                    l.addView(noLots);
                } else {
                    TextView lotsTitle = new TextView(l.getContext());
                    lotsTitle.setText("Lotes activos en almacén (FIFO):");
                    lotsTitle.setTextColor(0xFFB0B0B0);
                    lotsTitle.setTextSize(13f);
                    lotsTitle.setPadding(0, 4, 0, 4);
                    l.addView(lotsTitle);

                    for (LotMargin lm : r.getLotMargins()) {
                        TextView tvLot = new TextView(l.getContext());
                        tvLot.setText("• Lote #" + lm.getLotId() + " — Costo: " + CurrencyFormatter.formatCOP(lm.getUnitCost()) +
                                " | Quedan: " + lm.getQuantityRemaining() + " uds | Margen: " + CurrencyFormatter.formatCOP(lm.getProfitPerUnit()) + " (" + lm.getMarginOnSale() + "%)");
                        tvLot.setTextColor(0xFF9E9E9E);
                        tvLot.setTextSize(12f);
                        l.addView(tvLot);
                    }
                }
            }
        }

        static class FinancialChartViewHolder extends RecyclerView.ViewHolder {
            FinancialChartViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        static class DistributionChartViewHolder extends RecyclerView.ViewHolder {
            DistributionChartViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        static class ProductViewHolder extends RecyclerView.ViewHolder {
            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
