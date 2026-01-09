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

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class GlobalGraphicsFragment extends Fragment {

    private Spinner spinnerDXVK;
    private Spinner spinnerFEXCore;
    private Spinner spinnerTurnip;
    private Spinner spinnerResolution;
    private ExtendedFloatingActionButton fabSave;
    
    private SharedPreferences sharedPreferences;
    
    private static final String PREF_GLOBAL_DXVK_VERSION = "global_dxvk_version";
    private static final String PREF_GLOBAL_FEXCORE_VERSION = "global_fexcore_version";
    private static final String PREF_GLOBAL_TURNIP_VERSION = "global_turnip_version";
    private static final String PREF_GLOBAL_RESOLUTION = "global_resolution";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.global_graphics_fragment, container, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        spinnerDXVK = view.findViewById(R.id.SpinnerDXVK);
        spinnerFEXCore = view.findViewById(R.id.SpinnerFEXCore);
        spinnerTurnip = view.findViewById(R.id.SpinnerTurnip);
        spinnerResolution = view.findViewById(R.id.SpinnerResolution);
        fabSave = view.findViewById(R.id.FABSave);

        view.findViewById(R.id.Toolbar).setOnClickListener(v -> requireActivity().onBackPressed());

        setupSpinners();
        
        fabSave.setOnClickListener(v -> saveGraphicsConfig());

        return view;
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

        String[] fexVersions = {"2508"};
        ArrayAdapter<String> fexAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fexVersions);
        fexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFEXCore.setAdapter(fexAdapter);

        String[] turnipVersions = {"Turnip 25.1.0", "v819"};
        ArrayAdapter<String> turnipAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, turnipVersions);
        turnipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTurnip.setAdapter(turnipAdapter);
        
        String savedTurnip = sharedPreferences.getString(PREF_GLOBAL_TURNIP_VERSION, "Turnip 25.1.0");
        for (int i = 0; i < turnipVersions.length; i++) {
            if (turnipVersions[i].equals(savedTurnip)) {
                spinnerTurnip.setSelection(i);
                break;
            }
        }
    }

    private void saveGraphicsConfig() {
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

        Toast.makeText(requireContext(), "Configurações de gráficos salvas!", Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }
}
