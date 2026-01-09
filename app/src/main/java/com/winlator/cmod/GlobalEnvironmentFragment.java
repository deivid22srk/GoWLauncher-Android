package com.winlator.cmod;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class GlobalEnvironmentFragment extends Fragment {

    private SwitchMaterial switchDXVKHUD;
    private SwitchMaterial switchMesaGLThread;
    private SwitchMaterial switchWineEsync;
    private SwitchMaterial switchShaderCache;
    private Spinner spinnerZinkDescriptors;
    private View layoutDXVKHUDOptions;
    
    private Chip chipFPS;
    private Chip chipFrametimes;
    private Chip chipGPULoad;
    private Chip chipMemory;
    private Chip chipDevInfo;
    private Chip chipVersion;
    
    private ExtendedFloatingActionButton fabSave;
    private SharedPreferences sharedPreferences;
    
    private static final String PREF_DXVK_HUD_ENABLED = "global_dxvk_hud_enabled";
    private static final String PREF_DXVK_HUD_OPTIONS = "global_dxvk_hud_options";
    private static final String PREF_MESA_GLTHREAD = "global_mesa_glthread";
    private static final String PREF_WINEESYNC = "global_wineesync";
    private static final String PREF_SHADER_CACHE_DISABLE = "global_shader_cache_disable";
    private static final String PREF_ZINK_DESCRIPTORS = "global_zink_descriptors";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.global_environment_fragment, container, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        switchDXVKHUD = view.findViewById(R.id.SwitchDXVKHUD);
        switchMesaGLThread = view.findViewById(R.id.SwitchMesaGLThread);
        switchWineEsync = view.findViewById(R.id.SwitchWineEsync);
        switchShaderCache = view.findViewById(R.id.SwitchShaderCache);
        spinnerZinkDescriptors = view.findViewById(R.id.SpinnerZinkDescriptors);
        layoutDXVKHUDOptions = view.findViewById(R.id.LayoutDXVKHUDOptions);
        fabSave = view.findViewById(R.id.FABSave);

        chipFPS = view.findViewById(R.id.ChipFPS);
        chipFrametimes = view.findViewById(R.id.ChipFrametimes);
        chipGPULoad = view.findViewById(R.id.ChipGPULoad);
        chipMemory = view.findViewById(R.id.ChipMemory);
        chipDevInfo = view.findViewById(R.id.ChipDevInfo);
        chipVersion = view.findViewById(R.id.ChipVersion);

        view.findViewById(R.id.Toolbar).setOnClickListener(v -> requireActivity().onBackPressed());

        setupViews();
        loadSavedPreferences();
        
        fabSave.setOnClickListener(v -> saveEnvironmentConfig());

        return view;
    }

    private void setupViews() {
        String[] zinkOptions = {"auto", "lazy", "cached", "notemplates"};
        ArrayAdapter<String> zinkAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, zinkOptions);
        zinkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerZinkDescriptors.setAdapter(zinkAdapter);

        switchDXVKHUD.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutDXVKHUDOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void loadSavedPreferences() {
        boolean dxvkHudEnabled = sharedPreferences.getBoolean(PREF_DXVK_HUD_ENABLED, false);
        switchDXVKHUD.setChecked(dxvkHudEnabled);
        layoutDXVKHUDOptions.setVisibility(dxvkHudEnabled ? View.VISIBLE : View.GONE);

        String hudOptions = sharedPreferences.getString(PREF_DXVK_HUD_OPTIONS, "fps");
        if (hudOptions.contains("fps")) chipFPS.setChecked(true);
        if (hudOptions.contains("frametimes")) chipFrametimes.setChecked(true);
        if (hudOptions.contains("gpuload")) chipGPULoad.setChecked(true);
        if (hudOptions.contains("memory")) chipMemory.setChecked(true);
        if (hudOptions.contains("devinfo")) chipDevInfo.setChecked(true);
        if (hudOptions.contains("version")) chipVersion.setChecked(true);

        switchMesaGLThread.setChecked(sharedPreferences.getBoolean(PREF_MESA_GLTHREAD, false));
        switchWineEsync.setChecked(sharedPreferences.getBoolean(PREF_WINEESYNC, false));
        switchShaderCache.setChecked(sharedPreferences.getBoolean(PREF_SHADER_CACHE_DISABLE, false));

        String zinkDescriptors = sharedPreferences.getString(PREF_ZINK_DESCRIPTORS, "lazy");
        String[] zinkOptions = {"auto", "lazy", "cached", "notemplates"};
        for (int i = 0; i < zinkOptions.length; i++) {
            if (zinkOptions[i].equals(zinkDescriptors)) {
                spinnerZinkDescriptors.setSelection(i);
                break;
            }
        }
    }

    private void saveEnvironmentConfig() {
        List<String> hudOptions = new ArrayList<>();
        if (chipFPS.isChecked()) hudOptions.add("fps");
        if (chipFrametimes.isChecked()) hudOptions.add("frametimes");
        if (chipGPULoad.isChecked()) hudOptions.add("gpuload");
        if (chipMemory.isChecked()) hudOptions.add("memory");
        if (chipDevInfo.isChecked()) hudOptions.add("devinfo");
        if (chipVersion.isChecked()) hudOptions.add("version");

        String hudValue = String.join(",", hudOptions);

        sharedPreferences.edit()
            .putBoolean(PREF_DXVK_HUD_ENABLED, switchDXVKHUD.isChecked())
            .putString(PREF_DXVK_HUD_OPTIONS, hudValue)
            .putBoolean(PREF_MESA_GLTHREAD, switchMesaGLThread.isChecked())
            .putBoolean(PREF_WINEESYNC, switchWineEsync.isChecked())
            .putBoolean(PREF_SHADER_CACHE_DISABLE, switchShaderCache.isChecked())
            .putString(PREF_ZINK_DESCRIPTORS, spinnerZinkDescriptors.getSelectedItem().toString())
            .apply();

        Toast.makeText(requireContext(), "Configurações de ambiente salvas!", Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }
}
