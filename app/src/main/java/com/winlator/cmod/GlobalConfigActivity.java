package com.winlator.cmod;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.core.DefaultVersion;

import java.util.ArrayList;

public class GlobalConfigActivity extends AppCompatActivity {

    private Spinner spinnerDXVK;
    private Spinner spinnerFEXCore;
    private Spinner spinnerTurnip;
    private Spinner spinnerResolution;
    private Button btSaveConfig;
    
    private SharedPreferences sharedPreferences;
    
    private static final String PREF_GLOBAL_DXVK_VERSION = "global_dxvk_version";
    private static final String PREF_GLOBAL_FEXCORE_VERSION = "global_fexcore_version";
    private static final String PREF_GLOBAL_TURNIP_VERSION = "global_turnip_version";
    private static final String PREF_GLOBAL_RESOLUTION = "global_resolution";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.global_config_activity);

        MaterialToolbar toolbar = findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Configurações Globais");
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        spinnerDXVK = findViewById(R.id.SpinnerDXVK);
        spinnerFEXCore = findViewById(R.id.SpinnerFEXCore);
        spinnerTurnip = findViewById(R.id.SpinnerTurnip);
        spinnerResolution = findViewById(R.id.SpinnerResolution);
        btSaveConfig = findViewById(R.id.BTSaveConfig);

        setupSpinners();
        
        btSaveConfig.setOnClickListener(v -> saveGlobalConfig());
    }

    private void setupSpinners() {
        String[] resolutions = {"854x480", "1280x720", "1366x768", "1600x900", "1920x1080", "2560x1440"};
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resolutions);
        resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerResolution.setAdapter(resolutionAdapter);
        
        String savedResolution = sharedPreferences.getString(PREF_GLOBAL_RESOLUTION, "1920x1080");
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i].equals(savedResolution)) {
                spinnerResolution.setSelection(i);
                break;
            }
        }

        String[] dxvkVersions = {"2.6.2-1-arm64ec-gplasync", "2.3.1-arm64ec-gplasync", "2.3.1", "1.10.3-arm64ec-async", "1.10.3"};
        ArrayAdapter<String> dxvkAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dxvkVersions);
        dxvkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDXVK.setAdapter(dxvkAdapter);
        
        String savedDXVK = sharedPreferences.getString(PREF_GLOBAL_DXVK_VERSION, "2.6.2-1-arm64ec-gplasync");
        for (int i = 0; i < dxvkVersions.length; i++) {
            if (dxvkVersions[i].equals(savedDXVK)) {
                spinnerDXVK.setSelection(i);
                break;
            }
        }

        String[] fexVersions = {"2601", "2508"};
        ArrayAdapter<String> fexAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fexVersions);
        fexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFEXCore.setAdapter(fexAdapter);
        
        String savedFex = sharedPreferences.getString(PREF_GLOBAL_FEXCORE_VERSION, "2601");
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
        
        String savedTurnip = sharedPreferences.getString(PREF_GLOBAL_TURNIP_VERSION, "turnip25.1.0");
        for (int i = 0; i < turnipVersions.length; i++) {
            if (turnipVersions[i].equals(savedTurnip)) {
                spinnerTurnip.setSelection(i);
                break;
            }
        }
    }

    private void saveGlobalConfig() {
        String dxvkVersion = spinnerDXVK.getSelectedItem().toString();
        String fexVersion = spinnerFEXCore.getSelectedItem().toString();
        String turnipVersion = spinnerTurnip.getSelectedItem().toString();
        String resolution = spinnerResolution.getSelectedItem().toString();

        sharedPreferences.edit()
            .putString(PREF_GLOBAL_DXVK_VERSION, dxvkVersion)
            .putString(PREF_GLOBAL_FEXCORE_VERSION, fexVersion)
            .putString(PREF_GLOBAL_TURNIP_VERSION, turnipVersion)
            .putString(PREF_GLOBAL_RESOLUTION, resolution)
            .apply();

        ContainerManager containerManager = new ContainerManager(this);
        ArrayList<Container> containers = containerManager.getContainers();
        
        for (Container container : containers) {
            String graphicsDriverConfig = "vulkanVersion=1.3;version=" + turnipVersion + ";blacklistedExtensions=;maxDeviceMemory=0;presentMode=mailbox;syncFrame=0;disablePresentWait=0;resourceType=auto;bcnEmulation=auto;bcnEmulationType=software;bcnEmulationCache=0";
            container.setGraphicsDriverConfig(graphicsDriverConfig);
            container.setScreenSize(resolution);

            String dxwrapperConfig = "version=" + dxvkVersion + ",framerate=0,async=0,asyncCache=0,vkd3dVersion=" + DefaultVersion.VKD3D + ",vkd3dLevel=12_1,ddrawrapper=" + Container.DEFAULT_DDRAWRAPPER + ",csmt=3,gpuName=NVIDIA GeForce GTX 480,videoMemorySize=2048,strict_shader_math=1,OffscreenRenderingMode=fbo,renderer=gl";
            container.setDXWrapperConfig(dxwrapperConfig);

            container.setFEXCoreVersion(fexVersion);
            if (container.getBox64Version() == null || container.getBox64Version().isEmpty()) {
                container.setBox64Version(DefaultVersion.WOWBOX64);
            }
            container.saveData();
        }

        Toast.makeText(this, "Configurações globais salvas!", Toast.LENGTH_SHORT).show();
        finish();
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
