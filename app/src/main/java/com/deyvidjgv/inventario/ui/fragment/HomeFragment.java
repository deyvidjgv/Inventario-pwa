package com.deyvidjgv.inventario.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.deyvidjgv.inventario.InventarioApplication;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.di.AppContainer;
import com.deyvidjgv.inventario.domain.dto.JornadaSummary;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.deyvidjgv.inventario.domain.model.Jornada;
import com.deyvidjgv.inventario.ui.MainActivity;
import com.deyvidjgv.inventario.ui.util.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

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

        btnOpen.setOnClickListener(v -> handleOpenJornada());
        btnClose.setOnClickListener(v -> handleCloseJornada());
        btnReopen.setOnClickListener(v -> handleReopenJornada());
        btnBackup.setOnClickListener(v -> ((MainActivity) requireActivity()).loadFragment(new BackupFragment(), "Respaldo"));

        loadJornadaState();
        return view;
    }

    private void loadJornadaState() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Verificar si hay jornada abierta
                Jornada open = null;
                try {
                    open = container.getSalesService().openJornada(Instant.now()); // Intento o consulta
                } catch (DomainException e) {
                    // Si ya está abierta, no pudimos abrirla
                }
                
                // Cargar resúmenes
                requireActivity().runOnUiThread(this::updateUI);
            } catch (Exception e) {
                // Manejo general
            }
        });
    }

    private void updateUI() {
        // En UI thread
    }

    private void handleOpenJornada() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Jornada j = container.getSalesService().openJornada(Instant.now());
                activeJornadaId = j.getId();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Jornada abierta exitosamente", Toast.LENGTH_SHORT).show();
                    refreshState();
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void handleCloseJornada() {
        if (activeJornadaId == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Cerrar Jornada")
                .setMessage("¿Deseas cerrar la jornada actual?")
                .setPositiveButton("Cerrar", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            JornadaSummary summary = container.getSalesService().closeJornada(activeJornadaId, Instant.now(), Collections.emptyMap());
                            activeJornadaId = null;
                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Jornada cerrada. Ganancia: " + CurrencyFormatter.formatCOP(summary.getGrossProfit()), Toast.LENGTH_LONG).show();
                                refreshState();
                            });
                        } catch (DomainException e) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void handleReopenJornada() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Jornada j = container.getSalesService().reopenLastJornada();
                activeJornadaId = j.getId();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Última jornada reabierta", Toast.LENGTH_SHORT).show();
                    refreshState();
                });
            } catch (DomainException e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void refreshState() {
        if (isAdded()) {
            // Actualizar vista
        }
    }
}
