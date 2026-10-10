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
import com.deyvidjgv.inventario.InventarioApplication;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.di.AppContainer;
import com.deyvidjgv.inventario.domain.dto.JornadaSummary;
import com.deyvidjgv.inventario.domain.dto.ProductStock;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.model.Jornada;
import com.deyvidjgv.inventario.ui.MainActivity;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a").withZone(BOGOTA_ZONE);

    private TextView textStatus;
    private TextView textTime;
    private TextView textSales;
    private TextView textCost;
    private TextView textGrossProfit;
    private TextView textExpenses;
    private TextView textNetInformative;

    private MaterialButton btnOpen;
    private MaterialButton btnClose;
    private MaterialButton btnReopen;
    private MaterialButton btnBackup;
    private MaterialButton btnQuickExpense;

    private AppContainer container;
    private Long activeJornadaId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup containerView, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, containerView, false);

        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        textStatus = view.findViewById(R.id.text_jornada_status);
        textTime = view.findViewById(R.id.text_jornada_time);
        textSales = view.findViewById(R.id.text_total_sales);
        textCost = view.findViewById(R.id.text_total_cost);
        textGrossProfit = view.findViewById(R.id.text_gross_profit);
        textExpenses = view.findViewById(R.id.text_expenses);
        textNetInformative = view.findViewById(R.id.text_net_informative);

        btnOpen = view.findViewById(R.id.btn_open_jornada);
        btnClose = view.findViewById(R.id.btn_close_jornada);
        btnReopen = view.findViewById(R.id.btn_reopen_jornada);
        btnBackup = view.findViewById(R.id.btn_goto_backup);
        btnQuickExpense = view.findViewById(R.id.btn_quick_expense);

        btnOpen.setOnClickListener(v -> handleOpenJornada());
        btnClose.setOnClickListener(v -> handleCloseJornada());
        btnReopen.setOnClickListener(v -> handleReopenJornada());
        btnBackup.setOnClickListener(v -> ((MainActivity) requireActivity()).loadFragment(new BackupFragment(), "Respaldo"));
        btnQuickExpense.setOnClickListener(v -> showQuickExpenseDialog());

        loadJornadaState();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadJornadaState();
    }

    private void loadJornadaState() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Optional<Jornada> openOpt = container.getSalesService().getOpenJornada();
                Optional<Jornada> lastClosedOpt = container.getSalesService().getLastClosedJornada();

                if (openOpt.isPresent()) {
                    Jornada open = openOpt.get();
                    activeJornadaId = open.getId();
                    JornadaSummary summary = container.getReportService().summary(open.getId());
                    requireActivity().runOnUiThread(() -> updateUIOpen(open, summary));
                } else {
                    activeJornadaId = null;
                    JornadaSummary summary = lastClosedOpt.isPresent() ?
                            container.getReportService().summary(lastClosedOpt.get().getId()) : null;
                    requireActivity().runOnUiThread(() -> updateUIClosed(lastClosedOpt.orElse(null), summary));
                }
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    private void updateUIOpen(Jornada open, JornadaSummary summary) {
        if (!isAdded()) return;
        textStatus.setText("● JORNADA ABIERTA");
        textStatus.setTextColor(0xFF00E676);
        textTime.setText("Iniciada: " + TIME_FORMATTER.format(open.getOpenedAt()));

        btnOpen.setVisibility(View.GONE);
        btnClose.setVisibility(View.VISIBLE);
        btnReopen.setVisibility(View.GONE);

        renderSummary(summary);
    }

    private void updateUIClosed(Jornada lastClosed, JornadaSummary summary) {
        if (!isAdded()) return;
        textStatus.setText("○ JORNADA CERRADA");
        textStatus.setTextColor(0xFFFF9100);

        if (lastClosed != null && lastClosed.getClosedAt() != null) {
            textTime.setText("Último cierre: " + TIME_FORMATTER.format(lastClosed.getClosedAt()));
            btnReopen.setVisibility(View.VISIBLE);
        } else {
            textTime.setText("Sin jornada activa. Presiona Abrir para comenzar.");
            btnReopen.setVisibility(View.GONE);
        }

        btnOpen.setVisibility(View.VISIBLE);
        btnClose.setVisibility(View.GONE);

        renderSummary(summary);
    }

    private void renderSummary(JornadaSummary summary) {
        if (summary != null) {
            textSales.setText(CurrencyFormatter.formatCOP(summary.getTotalSales()));
            textCost.setText(CurrencyFormatter.formatCOP(summary.getTotalCost()));
            textGrossProfit.setText(CurrencyFormatter.formatCOP(summary.getGrossProfit()));
            if (summary.getTotalExpenses() > 0) {
                textExpenses.setText("-" + CurrencyFormatter.formatCOP(summary.getTotalExpenses()));
            } else {
                textExpenses.setText("$0");
            }
            textNetInformative.setText(CurrencyFormatter.formatCOP(summary.getNetInformativeProfit()));
        } else {
            textSales.setText("$0");
            textCost.setText("$0");
            textGrossProfit.setText("$0");
            textExpenses.setText("$0");
            textNetInformative.setText("$0");
        }
    }

    private void showQuickExpenseDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText inputConcept = new EditText(getContext());
        inputConcept.setHint("Concepto (e.g. Hielo, Bolsas, Limpieza)");
        layout.addView(inputConcept);

        EditText inputAmount = new EditText(getContext());
        inputAmount.setHint("Monto en COP (e.g. 5000)");
        inputAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputAmount);

        new AlertDialog.Builder(getContext())
                .setTitle("Registrar Gasto de Turno")
                .setMessage("Este gasto se restará inmediatamente de las ganancias de la sesión.")
                .setView(layout)
                .setPositiveButton("Registrar", (d, w) -> {
                    String concept = inputConcept.getText().toString().trim();
                    String amtStr = inputAmount.getText().toString().trim();
                    if (!concept.isEmpty() && !amtStr.isEmpty()) {
                        long amount = Long.parseLong(amtStr);
                        Executors.newSingleThreadExecutor().execute(() -> {
                            Optional<Jornada> openOpt = container.getSalesService().getOpenJornada();
                            Long jId = openOpt.map(Jornada::getId).orElse(activeJornadaId);
                            container.getExpenseService().add(concept, amount, Instant.now(), jId);
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Gasto registrado: " + concept + " (-" + CurrencyFormatter.formatCOP(amount) + ")", Toast.LENGTH_SHORT).show();
                                loadJornadaState();
                            });
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void handleOpenJornada() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Jornada j = container.getSalesService().openJornada(Instant.now());
                activeJornadaId = j.getId();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Jornada abierta. ¡Ya puedes registrar ventas!", Toast.LENGTH_SHORT).show();
                    loadJornadaState();
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void handleCloseJornada() {
        if (activeJornadaId == null) return;

        CharSequence[] options = {"Cerrar directamente (Sin conteo físico)", "Hacer conteo físico de stock (Recomendado)"};

        new AlertDialog.Builder(getContext())
                .setTitle("Cierre de Jornada")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        executeClose(Collections.emptyMap());
                    } else {
                        showPhysicalCountDialog();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showPhysicalCountDialog() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ProductStock> stocks = container.getInventoryService().listStock();
            requireActivity().runOnUiThread(() -> {
                ScrollView scroll = new ScrollView(getContext());
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(32, 16, 32, 16);
                scroll.addView(layout);

                Map<Long, EditText> inputs = new HashMap<>();

                for (ProductStock s : stocks) {
                    if (!s.getProduct().isTracksStock()) continue;

                    TextView label = new TextView(getContext());
                    label.setText(s.getProduct().getName() + " (Esperado en sistema: " + s.getCurrentStock() + ")");
                    label.setTextColor(0xFFFFFFFF);
                    label.setTextSize(14f);
                    label.setPadding(0, 12, 0, 4);
                    layout.addView(label);

                    EditText input = new EditText(getContext());
                    input.setHint("Cantidad física contada");
                    input.setText(String.valueOf(s.getCurrentStock()));
                    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    layout.addView(input);

                    inputs.put(s.getProduct().getId(), input);
                }

                new AlertDialog.Builder(getContext())
                        .setTitle("Conteo Físico al Cierre")
                        .setView(scroll)
                        .setPositiveButton("Confirmar y Cerrar", (d, w) -> {
                            Map<Long, Integer> countedMap = new HashMap<>();
                            for (Map.Entry<Long, EditText> entry : inputs.entrySet()) {
                                String val = entry.getValue().getText().toString().trim();
                                if (!val.isEmpty()) {
                                    countedMap.put(entry.getKey(), Integer.parseInt(val));
                                }
                            }
                            executeClose(countedMap);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        });
    }

    private void executeClose(Map<Long, Integer> countedMap) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JornadaSummary summary = container.getSalesService().closeJornada(activeJornadaId, Instant.now(), countedMap);
                activeJornadaId = null;
                requireActivity().runOnUiThread(() -> {
                    String msg = "Jornada cerrada exitosamente.\n" +
                            "• Ventas: " + CurrencyFormatter.formatCOP(summary.getTotalSales()) + "\n" +
                            "• Ganancia Bruta: " + CurrencyFormatter.formatCOP(summary.getGrossProfit());
                    if (!summary.getAdjustments().isEmpty()) {
                        msg += "\n• Ajustes por conteo: " + summary.getAdjustments().size();
                    }
                    new AlertDialog.Builder(getContext())
                            .setTitle("Resumen de Cierre")
                            .setMessage(msg)
                            .setPositiveButton("Entendido", null)
                            .show();
                    loadJornadaState();
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void handleReopenJornada() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Jornada j = container.getSalesService().reopenLastJornada();
                activeJornadaId = j.getId();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Última jornada reabierta", Toast.LENGTH_SHORT).show();
                    loadJornadaState();
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}
