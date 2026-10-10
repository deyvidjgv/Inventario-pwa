package com.deyvidjgv.inventario.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.deyvidjgv.inventario.InventarioApplication;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.di.AppContainer;
import com.deyvidjgv.inventario.domain.exception.DomainException;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.Executors;

public class BackupFragment extends Fragment {

    private MaterialButton btnExport;
    private MaterialButton btnImport;
    private AppContainer container;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_backup, parent, false);
        container = ((InventarioApplication) requireActivity().getApplication()).getContainer();

        btnExport = view.findViewById(R.id.btn_export_backup);
        btnImport = view.findViewById(R.id.btn_import_backup);

        btnExport.setOnClickListener(v -> handleExport());
        btnImport.setOnClickListener(v -> handleImport());

        return view;
    }

    private void handleExport() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String json = container.getBackupService().exportJson();
                requireActivity().runOnUiThread(() -> {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, json);
                    sendIntent.setType("application/json");
                    Intent shareIntent = Intent.createChooser(sendIntent, "Guardar o compartir copia de seguridad");
                    startActivity(shareIntent);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error exportando: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void handleImport() {
        EditText inputJson = new EditText(getContext());
        inputJson.setHint("Pega el contenido JSON del respaldo aquí");
        inputJson.setLines(6);

        new AlertDialog.Builder(getContext())
                .setTitle("Restaurar Respaldo")
                .setMessage("¡Atención! Restaurar reemplazará la base de datos actual con los datos del respaldo.")
                .setView(inputJson)
                .setPositiveButton("Restaurar", (dialog, which) -> {
                    String json = inputJson.getText().toString().trim();
                    if (!json.isEmpty()) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                container.getBackupService().importJson(json);
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "¡Respaldo restaurado con éxito!", Toast.LENGTH_LONG).show();
                                });
                            } catch (DomainException e) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
