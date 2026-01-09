package com.winlator.cmod;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.core.DefaultVersion;

import java.util.ArrayList;

public class GlobalConfigFragment extends Fragment {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.global_config_fragment, container, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        spinnerDXVK = view.findViewById(R.id.SpinnerDXVK);
        spinnerFEXCore = view.findViewById(R.id.SpinnerFEXCore);
        spinnerTurnip = view.findViewById(R.id.SpinnerTurnip);
        spinnerResolution = view.findViewById(R.id.SpinnerResolution);
        btSaveConfig = view.findViewById(R.id.BTSaveConfig);

        setupSpinners();
        
        btSaveConfig.setOnClickListener(v -> saveGlobalConfig());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setupSpinners() {
        String[] resolutions = {"854x480", "1280x720", "1366x768", "1600x900", "1920x1080", "2560x1440"};
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, resolutions);
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
        ArrayAdapter<String> dxvkAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, dxvkVersions);
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
        ArrayAdapter<String> fexAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fexVersions);
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
        ArrayAdapter<String> turnipAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, turnipVersions);
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

        ContainerManager containerManager = new ContainerManager(requireContext());
        ArrayList<Container> containers = containerManager.getContainers();
        
        for (Container container : containers) {
            String graphicsDriverConfig = "vulkanVersion=1.3;version=" + turnipVersion + ";blacklistedExtensions=;maxDeviceMemory=0;presentMode=mailbox;syncFrame=0;disablePresentWait=0;resourceType=auto;bcnEmulation=auto;bcnEmulationType=software;bcnEmulationCache=0";
            container.setGraphicsDriverConfig(graphicsDriverConfig);
            container.setScreenSize(resolution);

            String dxwrapperConfig = "version=" + dxvkVersion + ",framerate=0,async=0,asyncCache=0,vkd3dVersion=" + DefaultVersion.VKD3D + ",vkd3dLevel=12_1,ddrawrapper=" + Container.DEFAULT_DDRAWRAPPER + ",csmt=3,gpuName=NVIDIA GeForce GTX 480,videoMemorySize=2048,strict_shader_math=1,OffscreenRenderingMode=fbo,renderer=gl";
            container.setDXWrapperConfig(dxwrapperConfig);

            container.setFEXCoreVersion(fexVersion);
            container.saveData();
        }

        Toast.makeText(requireContext(), "Configurações salvas!", Toast.LENGTH_SHORT).show();
    }
}
