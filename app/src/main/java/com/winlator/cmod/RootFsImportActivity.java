package com.winlator.cmod;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.Executors;

/**
 * Activity para importar Orion RootFS (.orfs)
 * Permite ao usuário selecionar e importar o sistema de arquivos root
 */
public class RootFsImportActivity extends AppCompatActivity {
    
    private static final String TAG = "RootFsImportActivity";
    private static final int PICK_ORFS_FILE = 1001;
    
    private Button btnSelectFile;
    private Button btnImport;
    private Button btnDownload;
    private TextView tvFilePath;
    private TextView tvStatus;
    private ProgressBar progressBar;
    
    private File selectedOrfsFile = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rootfs_import_activity);
        
        MaterialToolbar toolbar = findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Importar RootFS");
        }
        
        initViews();
        setupListeners();
        checkExistingInstallation();
    }
    
    private void initViews() {
        btnSelectFile = findViewById(R.id.BTSelectFile);
        btnImport = findViewById(R.id.BTImport);
        btnDownload = findViewById(R.id.BTDownload);
        tvFilePath = findViewById(R.id.TVFilePath);
        tvStatus = findViewById(R.id.TVStatus);
        progressBar = findViewById(R.id.ProgressBar);
        
        btnImport.setEnabled(false);
        progressBar.setVisibility(View.GONE);
    }
    
    private void setupListeners() {
        btnSelectFile.setOnClickListener(v -> selectOrfsFile());
        btnImport.setOnClickListener(v -> startImport());
        btnDownload.setOnClickListener(v -> openDownloadPage());
    }
    
    private void checkExistingInstallation() {
        ImageFs imageFs = ImageFs.find(this);
        if (imageFs.isValid()) {
            tvStatus.setText("✓ Sistema já instalado (versão " + imageFs.getVersion() + ")\\n" +
                           "Você pode reimportar para atualizar.");
        } else {
            tvStatus.setText("Sistema não instalado\\n" +
                           "Selecione um arquivo .orfs para instalar");
        }
    }
    
    private void selectOrfsFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Selecione o arquivo RootFS (.orfs)"),
                PICK_ORFS_FILE
            );
        } catch (android.content.ActivityNotFoundException ex) {
            showError("Nenhum gerenciador de arquivos encontrado");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_ORFS_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                handleSelectedFile(uri);
            }
        }
    }
    
    private void handleSelectedFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                showError("Não foi possível abrir o arquivo");
                return;
            }

            String fileName = FileUtils.getFileName(this, uri);
            if (fileName == null || !fileName.endsWith(".orfs")) {
                inputStream.close();
                showError("Arquivo inválido. Selecione um arquivo .orfs");
                return;
            }

            File cacheDir = new File(getCacheDir(), "orfs_temp");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            selectedOrfsFile = new File(cacheDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(selectedOrfsFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            inputStream.close();
            outputStream.close();

            long sizeBytes = selectedOrfsFile.length();
            String sizeStr = formatFileSize(sizeBytes);

            tvFilePath.setText("Arquivo: " + selectedOrfsFile.getName() + " (" + sizeStr + ")");
            tvFilePath.setVisibility(View.VISIBLE);
            tvStatus.setText("Pronto para importar\\nTamanho: " + sizeStr);
            btnImport.setEnabled(true);

        } catch (Exception e) {
            Log.e(TAG, "Error handling file", e);
            showError("Erro ao processar arquivo: " + e.getMessage());
        }
    }
    
    private void startImport() {
        if (selectedOrfsFile == null || !selectedOrfsFile.exists()) {
            showError("Nenhum arquivo selecionado");
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Confirmar Importação")
            .setMessage("Isso irá instalar/atualizar o sistema.\\n\\n" +
                       "Tempo estimado: 3-5 minutos\\n" +
                       "Espaço necessário: ~3GB\\n\\n" +
                       "Continuar?")
            .setPositiveButton("Sim", (dialog, which) -> performImport())
            .setNegativeButton("Não", null)
            .show();
    }
    
    private void performImport() {
        btnSelectFile.setEnabled(false);
        btnImport.setEnabled(false);
        btnDownload.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Importando...");
        
        com.winlator.cmod.xenvironment.ImageFsInstaller.installFromRootFs(
            this,
            selectedOrfsFile,
            () -> {
                runOnUiThread(() -> {
                    progressBar.setProgress(100);
                    tvStatus.setText("✓ Instalação concluída com sucesso!");
                    
                    new AlertDialog.Builder(this)
                        .setTitle("Sucesso!")
                        .setMessage("O RootFS foi importado com sucesso.\\n\\n" +
                                   "O sistema está pronto para uso.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            setResult(RESULT_OK);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
                });
            },
            (errorMessage) -> {
                runOnUiThread(() -> {
                    showError(errorMessage);
                    resetUI();
                });
            }
        );
    }
    
    private void updateProgress(String message, int progress) {
        runOnUiThread(() -> {
            tvStatus.setText(message);
            progressBar.setProgress(progress);
        });
    }
    
    private void resetUI() {
        btnSelectFile.setEnabled(true);
        btnImport.setEnabled(selectedOrfsFile != null);
        btnDownload.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        progressBar.setProgress(0);
    }
    
    private void openDownloadPage() {
        String url = "https://github.com/deivid22srk/Orion-RootFs/releases/latest";
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            showError("Não foi possível abrir o navegador");
        }
    }
    
    private void showError(String message) {
        new AlertDialog.Builder(this)
            .setTitle("Erro")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
