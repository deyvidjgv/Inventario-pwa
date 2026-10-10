package com.deyvidjgv.inventario.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.deyvidjgv.inventario.InventarioApplication;
import com.deyvidjgv.inventario.R;
import com.deyvidjgv.inventario.di.AppContainer;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class BackupFragment extends Fragment {

    private MaterialButton btnExport;
    private MaterialButton btnImport;
    private AppContainer container;

    private ActivityResultLauncher<String> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                importFromUri(uri);
            }
        });
    }

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
        Context ctx = getContext();
        if (ctx == null) return;

        container.getExecutor().execute(() -> {
            try {
                String json = container.getBackupService().exportJson();
                Activity act = getActivity();
                if (act == null || !isAdded()) return;

                act.runOnUiThread(() -> {
                    try {
                        // Guardar en archivo caché para compartir como archivo real sin límite de 1MB
                        File cacheDir = act.getCacheDir();
                        File backupFile = new File(cacheDir, "inventario_pool_backup.json");
                        try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                            fos.write(json.getBytes(StandardCharsets.UTF_8));
                        }

                        Uri contentUri = FileProvider.getUriForFile(
                                act,
                                act.getPackageName() + ".fileprovider",
                                backupFile
                        );

                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.setType("application/json");
                        sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        Intent chooser = Intent.createChooser(sendIntent, "Guardar o compartir respaldo JSON");
                        startActivity(chooser);
                    } catch (Exception fileEx) {
                        // Fallback a texto directo si FileProvider falla
                        Intent textIntent = new Intent(Intent.ACTION_SEND);
                        textIntent.setType("text/plain");
                        textIntent.putExtra(Intent.EXTRA_TEXT, json);
                        startActivity(Intent.createChooser(textIntent, "Guardar respaldo"));
                    }
                });
            } catch (Exception e) {
                Activity act = getActivity();
                if (act != null && isAdded()) {
                    act.runOnUiThread(() -> Toast.makeText(getContext(), "Error exportando: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void handleImport() {
        CharSequence[] options = {"Seleccionar archivo .json (Recomendado)", "Pegar texto JSON manualmente"};
        new AlertDialog.Builder(getContext())
                .setTitle("Restaurar Copia de Seguridad")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        try {
                            filePickerLauncher.launch("*/*");
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "No se pudo abrir el selector de archivos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showPasteJsonDialog();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void importFromUri(Uri uri) {
        Context ctx = getContext();
        if (ctx == null) return;

        new AlertDialog.Builder(ctx)
                .setTitle("Confirmar Restauración")
                .setMessage("¡Atención! Restaurar este archivo reemplazará los datos actuales con los del respaldo. ¿Deseas continuar?")
                .setPositiveButton("Sí, Restaurar", (d, w) -> {
                    container.getExecutor().execute(() -> {
                        try {
                            InputStream is = ctx.getContentResolver().openInputStream(uri);
                            if (is == null) throw new IllegalStateException("No se pudo leer el archivo");
                            StringBuilder sb = new StringBuilder();
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    sb.append(line).append("\n");
                                }
                            }
                            executeRestore(sb.toString());
                        } catch (Exception e) {
                            Activity act = getActivity();
                            if (act != null && isAdded()) {
                                act.runOnUiThread(() -> Toast.makeText(getContext(), "Error al leer archivo: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showPasteJsonDialog() {
        EditText inputJson = new EditText(getContext());
        inputJson.setHint("Pega el contenido JSON del respaldo aquí");
        inputJson.setLines(6);

        new AlertDialog.Builder(getContext())
                .setTitle("Pegar Respaldo JSON")
                .setMessage("¡Atención! Restaurar reemplazará la base de datos actual.")
                .setView(inputJson)
                .setPositiveButton("Restaurar", (dialog, which) -> {
                    String json = inputJson.getText().toString().trim();
                    if (!json.isEmpty()) {
                        container.getExecutor().execute(() -> executeRestore(json));
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executeRestore(String json) {
        try {
            container.getBackupService().importJson(json);
            Activity act = getActivity();
            if (act != null && isAdded()) {
                act.runOnUiThread(() -> Toast.makeText(getContext(), "¡Respaldo restaurado con éxito!", Toast.LENGTH_LONG).show());
            }
        } catch (Exception e) {
            Activity act = getActivity();
            if (act != null && isAdded()) {
                act.runOnUiThread(() -> Toast.makeText(getContext(), "Error al restaurar: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }
}
