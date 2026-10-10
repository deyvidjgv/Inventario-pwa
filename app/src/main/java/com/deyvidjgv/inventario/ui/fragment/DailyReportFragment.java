package com.deyvidjgv.inventario.ui.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.deyvidjgv.inventario.InventarioApplication;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.di.AppContainer;
import com.deyvidjgv.inventario.domain.dto.DailyReport;
import com.deyvidjgv.inventario.domain.dto.ProductSaleDetail;
import com.deyvidjgv.inventario.domain.dto.StockAdjustmentDetail;
import com.deyvidjgv.inventario.domain.dto.StockEntryDetail;
import com.deyvidjgv.inventario.domain.model.Expense;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DailyReportFragment extends Fragment {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", new Locale("es", "CO"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a").withZone(BOGOTA_ZONE);

    private LocalDate selectedDate = LocalDate.now();
    private AppContainer container;

    private MaterialButton btnPrevDay;
    private MaterialButton btnPickDate;
    private MaterialButton btnNextDay;

    private TextView textDailySales;
    private TextView textDailyCost;
    private TextView textDailyGross;
    private TextView textDailyExpenses;
    private TextView textDailyNet;
    private TextView textDailyMarginBadge;

    private LinearLayout containerDailySales;
    private LinearLayout containerDailyEntries;
    private LinearLayout containerDailyOutflows;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_report, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        btnPrevDay = view.findViewById(R.id.btn_prev_day);
        btnPickDate = view.findViewById(R.id.btn_pick_date);
        btnNextDay = view.findViewById(R.id.btn_next_day);

        textDailySales = view.findViewById(R.id.text_daily_sales);
        textDailyCost = view.findViewById(R.id.text_daily_cost);
        textDailyGross = view.findViewById(R.id.text_daily_gross);
        textDailyExpenses = view.findViewById(R.id.text_daily_expenses);
        textDailyNet = view.findViewById(R.id.text_daily_net);
        textDailyMarginBadge = view.findViewById(R.id.text_daily_margin_badge);

        containerDailySales = view.findViewById(R.id.container_daily_sales);
        containerDailyEntries = view.findViewById(R.id.container_daily_entries);
        containerDailyOutflows = view.findViewById(R.id.container_daily_outflows);

        btnPrevDay.setOnClickListener(v -> changeDate(selectedDate.minusDays(1)));
        btnNextDay.setOnClickListener(v -> changeDate(selectedDate.plusDays(1)));
        btnPickDate.setOnClickListener(v -> showDatePicker());

        loadReport();
        return view;
    }

    private void changeDate(LocalDate newDate) {
        this.selectedDate = newDate;
        loadReport();
    }

    private void showDatePicker() {
        if (getContext() == null) return;
        DatePickerDialog dialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> changeDate(LocalDate.of(year, month + 1, dayOfMonth)),
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth()
        );
        dialog.show();
    }

    private void loadReport() {
        LocalDate today = LocalDate.now();
        String dateLabel;
        if (selectedDate.equals(today)) {
            dateLabel = "📅 Hoy (" + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
        } else if (selectedDate.equals(today.minusDays(1))) {
            dateLabel = "📅 Ayer (" + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
        } else {
            dateLabel = "📅 " + selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        btnPickDate.setText(dateLabel);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                DailyReport report = container.getReportService().dailyReport(selectedDate);
                requireActivity().runOnUiThread(() -> renderReport(report));
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private void renderReport(DailyReport report) {
        if (!isAdded() || getContext() == null) return;

        // 1. Resumen Financiero
        textDailySales.setText(CurrencyFormatter.formatCOP(report.getTotalSales()));
        textDailyCost.setText(CurrencyFormatter.formatCOP(report.getTotalCost()));
        textDailyGross.setText(CurrencyFormatter.formatCOP(report.getGrossProfit()));

        if (report.getTotalExpenses() > 0) {
            textDailyExpenses.setText("-" + CurrencyFormatter.formatCOP(report.getTotalExpenses()));
        } else {
            textDailyExpenses.setText("$0");
        }

        textDailyNet.setText(CurrencyFormatter.formatCOP(report.getNetProfit()));
        textDailyMarginBadge.setText(String.format(Locale.US,
                "Margen neto: %.1f%% | Ventas realizadas: %d tickets | Fecha: %s",
                report.getNetMarginPercent(),
                report.getSalesCount(),
                selectedDate.format(DATE_DISPLAY_FORMAT)));

        // 2. ¿Qué se vendió este día?
        containerDailySales.removeAllViews();
        if (report.getProductsSold().isEmpty()) {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("No se registraron ventas en esta fecha.");
            tvEmpty.setTextColor(0xFF9E9E9E);
            tvEmpty.setTextSize(14f);
            tvEmpty.setPadding(0, 8, 0, 8);
            containerDailySales.addView(tvEmpty);
        } else {
            for (ProductSaleDetail p : report.getProductsSold()) {
                MaterialCardView pCard = new MaterialCardView(getContext());
                pCard.setCardBackgroundColor(0xFF242424);
                pCard.setRadius(10f);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 6, 0, 6);
                pCard.setLayoutParams(params);

                LinearLayout pLayout = new LinearLayout(getContext());
                pLayout.setOrientation(LinearLayout.VERTICAL);
                pLayout.setPadding(14, 12, 14, 12);

                // Nombre y cantidad
                LinearLayout topRow = new LinearLayout(getContext());
                topRow.setOrientation(LinearLayout.HORIZONTAL);

                TextView tvName = new TextView(getContext());
                tvName.setText(p.getProductName());
                tvName.setTextColor(0xFFFFFFFF);
                tvName.setTextSize(16f);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                topRow.addView(tvName, p1);

                TextView tvQty = new TextView(getContext());
                tvQty.setText(p.getUnitsSold() + " uds");
                tvQty.setTextColor(0xFF00E676);
                tvQty.setTextSize(15f);
                tvQty.setTypeface(null, android.graphics.Typeface.BOLD);
                topRow.addView(tvQty);
                pLayout.addView(topRow);

                // Cifras financieras
                TextView tvDetails = new TextView(getContext());
                tvDetails.setText(String.format(Locale.US,
                        "• Venta: %s  |  Costo: %s\n• Ganancia: +%s (Margen: %.1f%%)",
                        CurrencyFormatter.formatCOP(p.getTotalSales()),
                        CurrencyFormatter.formatCOP(p.getTotalCost()),
                        CurrencyFormatter.formatCOP(p.getRealProfit()),
                        p.getMarginOnSale()));
                tvDetails.setTextColor(0xFFB0B0B0);
                tvDetails.setTextSize(13f);
                tvDetails.setPadding(0, 4, 0, 0);
                pLayout.addView(tvDetails);

                pCard.addView(pLayout);
                containerDailySales.addView(pCard);
            }
        }

        // 3. ¿Qué entró este día? (Entradas de Stock)
        containerDailyEntries.removeAllViews();
        if (report.getStockEntries().isEmpty()) {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("No se recibieron compras ni entradas de inventario este día.");
            tvEmpty.setTextColor(0xFF9E9E9E);
            tvEmpty.setTextSize(14f);
            tvEmpty.setPadding(0, 8, 0, 8);
            containerDailyEntries.addView(tvEmpty);
        } else {
            for (StockEntryDetail entry : report.getStockEntries()) {
                MaterialCardView eCard = new MaterialCardView(getContext());
                eCard.setCardBackgroundColor(0xFF242424);
                eCard.setRadius(10f);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 6, 0, 6);
                eCard.setLayoutParams(params);

                LinearLayout eLayout = new LinearLayout(getContext());
                eLayout.setOrientation(LinearLayout.VERTICAL);
                eLayout.setPadding(14, 12, 14, 12);

                LinearLayout topRow = new LinearLayout(getContext());
                topRow.setOrientation(LinearLayout.HORIZONTAL);

                TextView tvName = new TextView(getContext());
                tvName.setText(entry.getProductName());
                tvName.setTextColor(0xFFFFFFFF);
                tvName.setTextSize(16f);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                topRow.addView(tvName, p1);

                TextView tvQty = new TextView(getContext());
                tvQty.setText("+" + entry.getQuantity() + " uds");
                tvQty.setTextColor(0xFFFF9100);
                tvQty.setTextSize(15f);
                tvQty.setTypeface(null, android.graphics.Typeface.BOLD);
                topRow.addView(tvQty);
                eLayout.addView(topRow);

                String noteStr = !entry.getNote().isEmpty() ? " | " + entry.getNote() : "";
                TextView tvDetails = new TextView(getContext());
                tvDetails.setText(String.format(Locale.US,
                        "• Costo unitario: %s  |  Inversión total: %s\n• Hora: %s%s",
                        CurrencyFormatter.formatCOP(entry.getUnitCost()),
                        CurrencyFormatter.formatCOP(entry.getTotalInvestment()),
                        TIME_FORMAT.format(entry.getReceivedAt()),
                        noteStr));
                tvDetails.setTextColor(0xFFB0B0B0);
                tvDetails.setTextSize(13f);
                tvDetails.setPadding(0, 4, 0, 0);
                eLayout.addView(tvDetails);

                eCard.addView(eLayout);
                containerDailyEntries.addView(eCard);
            }
        }

        // 4. ¿Qué salió este día? (Gastos y Mermas)
        containerDailyOutflows.removeAllViews();
        boolean hasOutflows = !report.getExpenses().isEmpty() || !report.getAdjustments().isEmpty();

        if (!hasOutflows) {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("No se registraron gastos de dinero ni mermas en esta fecha.");
            tvEmpty.setTextColor(0xFF9E9E9E);
            tvEmpty.setTextSize(14f);
            tvEmpty.setPadding(0, 8, 0, 8);
            containerDailyOutflows.addView(tvEmpty);
        } else {
            // Gastos de Dinero
            for (Expense exp : report.getExpenses()) {
                MaterialCardView expCard = new MaterialCardView(getContext());
                expCard.setCardBackgroundColor(0xFF242424);
                expCard.setRadius(10f);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 6, 0, 6);
                expCard.setLayoutParams(params);

                LinearLayout expLayout = new LinearLayout(getContext());
                expLayout.setOrientation(LinearLayout.HORIZONTAL);
                expLayout.setPadding(14, 12, 14, 12);
                expLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                LinearLayout textCol = new LinearLayout(getContext());
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

                TextView tvConcept = new TextView(getContext());
                tvConcept.setText(exp.getConcept());
                tvConcept.setTextColor(0xFFFFFFFF);
                tvConcept.setTextSize(15f);
                tvConcept.setTypeface(null, android.graphics.Typeface.BOLD);
                textCol.addView(tvConcept);

                TextView tvTime = new TextView(getContext());
                String shiftStr = exp.getJornadaId() != null ? " • Turno #" + exp.getJornadaId() : "";
                tvTime.setText("Salida de caja (" + TIME_FORMAT.format(exp.getCreatedAt()) + ")" + shiftStr);
                tvTime.setTextColor(0xFF9E9E9E);
                tvTime.setTextSize(12f);
                textCol.addView(tvTime);

                expLayout.addView(textCol, p1);

                TextView tvAmount = new TextView(getContext());
                tvAmount.setText("-" + CurrencyFormatter.formatCOP(exp.getAmount()));
                tvAmount.setTextColor(0xFFFF5252);
                tvAmount.setTextSize(16f);
                tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
                expLayout.addView(tvAmount);

                expCard.addView(expLayout);
                containerDailyOutflows.addView(expCard);
            }

            // Mermas / Ajustes Físicos
            for (StockAdjustmentDetail adj : report.getAdjustments()) {
                if (adj.getDifference() != 0) {
                    MaterialCardView adjCard = new MaterialCardView(getContext());
                    adjCard.setCardBackgroundColor(0xFF242424);
                    adjCard.setRadius(10f);
                    ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 6, 0, 6);
                    adjCard.setLayoutParams(params);

                    LinearLayout adjLayout = new LinearLayout(getContext());
                    adjLayout.setOrientation(LinearLayout.VERTICAL);
                    adjLayout.setPadding(14, 12, 14, 12);

                    TextView tvTitle = new TextView(getContext());
                    String impactStr = adj.getUnitCost() > 0 ?
                            " (" + CurrencyFormatter.formatCOP(Math.abs(adj.getTotalLossOrGainAtCost())) + " al costo)" : "";
                    String diffStr = adj.getDifference() < 0 ?
                            "⚠️ " + adj.getProductName() + ": Faltante de " + Math.abs(adj.getDifference()) + " uds" + impactStr :
                            "ℹ️ " + adj.getProductName() + ": Sobrante de +" + adj.getDifference() + " uds" + impactStr;
                    tvTitle.setText(diffStr);
                    tvTitle.setTextColor(adj.getDifference() < 0 ? 0xFFFF5252 : 0xFFFFD54F);
                    tvTitle.setTextSize(14f);
                    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    adjLayout.addView(tvTitle);

                    TextView tvSub = new TextView(getContext());
                    tvSub.setText("Esperado en sistema: " + adj.getExpected() + " | Conteo físico real: " + adj.getCounted() + " • Turno #" + adj.getJornadaId());
                    tvSub.setTextColor(0xFF9E9E9E);
                    tvSub.setTextSize(12f);
                    adjLayout.addView(tvSub);

                    adjCard.addView(adjLayout);
                    containerDailyOutflows.addView(adjCard);
                }
            }
        }
    }
}
