package com.winlator.cmod;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.os.Environment;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.GowLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class GowConfigActivity extends AppCompatActivity {

    private Spinner spinnerDXVK;
    private Spinner spinnerFEXCore;
    private Spinner spinnerTurnip;
    private Button btStartGame;
    
    private Container gowContainer;
    private File selectedExeFile;
    private SharedPreferences sharedPreferences;
    
    private static final String PREF_DXVK_VERSION = "gow_dxvk_version";
    private static final String PREF_FEXCORE_VERSION = "gow_fexcore_version";
    private static final String PREF_TURNIP_VERSION = "gow_turnip_version";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gow_config_activity);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Configurações - God of War");
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String exePath = getIntent().getStringExtra("exe_path");
        int containerId = getIntent().getIntExtra("container_id", -1);

        GowLogger.i("GowConfig", "Tela de configuração aberta para: " + exePath);

        if (exePath == null || containerId == -1) {
            Toast.makeText(this, "Erro: dados inválidos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        selectedExeFile = new File(exePath);
        ContainerManager manager = new ContainerManager(this);
        gowContainer = manager.getContainerById(containerId);

        spinnerDXVK = findViewById(R.id.SpinnerDXVK);
        spinnerFEXCore = findViewById(R.id.SpinnerFEXCore);
        spinnerTurnip = findViewById(R.id.SpinnerTurnip);
        btStartGame = findViewById(R.id.BTStartGame);

        setupSpinners();
        
        btStartGame.setOnClickListener(v -> startGame());
    }

    private void setupSpinners() {
        String[] dxvkVersions = {"2.3.1-arm64ec-gplasync", "2.3.1", "1.10.3-arm64ec-async", "1.10.3"};
        ArrayAdapter<String> dxvkAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dxvkVersions);
        dxvkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDXVK.setAdapter(dxvkAdapter);
        
        String savedDXVK = sharedPreferences.getString(PREF_DXVK_VERSION, DefaultVersion.DXVK);
        for (int i = 0; i < dxvkVersions.length; i++) {
            if (dxvkVersions[i].equals(savedDXVK)) {
                spinnerDXVK.setSelection(i);
                break;
            }
        }

        String[] fexVersions = {DefaultVersion.FEXCORE};
        ArrayAdapter<String> fexAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fexVersions);
        fexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFEXCore.setAdapter(fexAdapter);
        
        String savedFex = sharedPreferences.getString(PREF_FEXCORE_VERSION, DefaultVersion.FEXCORE);
        for (int i = 0; i < fexVersions.length; i++) {
            if (fexVersions[i].equals(savedFex)) {
                spinnerFEXCore.setSelection(i);
                break;
            }
        }

        String[] turnipVersions = {"turnip25.1.0", "v819"};
        ArrayAdapter<String> turnipAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, turnipVersions);
        turnipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTurnip.setAdapter(turnipAdapter);
        
        String savedTurnip = sharedPreferences.getString(PREF_TURNIP_VERSION, "turnip25.1.0");
        for (int i = 0; i < turnipVersions.length; i++) {
            if (turnipVersions[i].equals(savedTurnip)) {
                spinnerTurnip.setSelection(i);
                break;
            }
        }
    }

    private String convertAndroidPathToWinePath(String androidPath) {
        String externalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        
        if (androidPath.startsWith(externalStoragePath)) {
            String relativePath = androidPath.substring(externalStoragePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            String winePath = "D:\\" + relativePath.replace("/", "\\");
            return winePath;
        }
        
        return androidPath.replace("/", "\\");
    }

    private void startGame() {
        String dxvkVersion = spinnerDXVK.getSelectedItem().toString();
        String fexVersion = spinnerFEXCore.getSelectedItem().toString();
        String turnipVersion = spinnerTurnip.getSelectedItem().toString();

        sharedPreferences.edit()
            .putString(PREF_DXVK_VERSION, dxvkVersion)
            .putString(PREF_FEXCORE_VERSION, fexVersion)
            .putString(PREF_TURNIP_VERSION, turnipVersion)
            .apply();

        GowLogger.i("GowConfig", "Configurações salvas - DXVK: " + dxvkVersion + ", FEXCore: " + fexVersion + ", Turnip: " + turnipVersion);

        String graphicsDriverConfig = "vulkanVersion=1.3;version=" + turnipVersion + ";blacklistedExtensions=;maxDeviceMemory=0;presentMode=mailbox;syncFrame=0;disablePresentWait=0;resourceType=auto;bcnEmulation=auto;bcnEmulationType=software;bcnEmulationCache=0";
        gowContainer.setGraphicsDriverConfig(graphicsDriverConfig);

        String dxwrapperConfig = "version=" + dxvkVersion + ",framerate=0,async=0,asyncCache=0,vkd3dVersion=" + DefaultVersion.VKD3D + ",vkd3dLevel=12_1,ddrawrapper=" + Container.DEFAULT_DDRAWRAPPER + ",csmt=3,gpuName=NVIDIA GeForce GTX 480,videoMemorySize=2048,strict_shader_math=1,OffscreenRenderingMode=fbo,renderer=gl";
        gowContainer.setDXWrapperConfig(dxwrapperConfig);

        gowContainer.setFEXCoreVersion(fexVersion);
        gowContainer.saveData();

        GowLogger.i("GowConfig", "Container atualizado com novas configurações");

        try {
            String displayName = "God of War";
            String winePath = convertAndroidPathToWinePath(selectedExeFile.getAbsolutePath());
            String wineWorkDir = convertAndroidPathToWinePath(selectedExeFile.getParent());

            GowLogger.i("GowConfig", "Caminho convertido - Android: " + selectedExeFile.getAbsolutePath() + " -> Wine: " + winePath);
            GowLogger.i("GowConfig", "Diretório convertido - Android: " + selectedExeFile.getParent() + " -> Wine: " + wineWorkDir);

            File shortcutsDir = gowContainer.getDesktopDir();
            if (!shortcutsDir.exists()) shortcutsDir.mkdirs();

            File desktopFile = new File(shortcutsDir, displayName + ".desktop");

            try (PrintWriter writer = new PrintWriter(new FileWriter(desktopFile))) {
                writer.println("[Desktop Entry]");
                writer.println("Name=" + displayName);
                writer.println("Exec=env WINEPREFIX=\"/home/xuser/.wine\" wine \"" + winePath + "\"");
                writer.println("Type=Application");
                writer.println("Terminal=false");
                writer.println("StartupNotify=true");
                writer.println("Icon=" + displayName);
                writer.println("Path=" + wineWorkDir);
                writer.println("container_id:" + gowContainer.id);
                writer.println("");
                writer.println("[Extra Data]");
                writer.println("container_id=" + gowContainer.id);
            }

            GowLogger.i("GowConfig", "Iniciando XServerDisplayActivity");

            Intent intent = new Intent(this, XServerDisplayActivity.class);
            intent.putExtra("container_id", gowContainer.id);
            intent.putExtra("shortcut_path", desktopFile.getAbsolutePath());
            intent.putExtra("shortcut_name", displayName);
            startActivity(intent);

        } catch (Exception e) {
            GowLogger.e("GowConfig", "Erro ao iniciar jogo", e);
            Toast.makeText(this, "Erro ao iniciar jogo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
