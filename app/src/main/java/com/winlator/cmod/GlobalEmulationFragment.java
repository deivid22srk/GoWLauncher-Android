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
import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.fexcore.FEXCorePresetManager;

import java.util.ArrayList;

public class GlobalEmulationFragment extends Fragment {

    private Spinner spinnerBox64Preset;
    private Spinner spinnerFEXCorePreset;
    private ExtendedFloatingActionButton fabSave;
    
    private SharedPreferences sharedPreferences;
    
    private static final String PREF_GLOBAL_BOX64_PRESET = "global_box64_preset";
    private static final String PREF_GLOBAL_FEXCORE_PRESET = "global_fexcore_preset";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.global_emulation_fragment, container, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        spinnerBox64Preset = view.findViewById(R.id.SpinnerBox64Preset);
        spinnerFEXCorePreset = view.findViewById(R.id.SpinnerFEXCorePreset);
        fabSave = view.findViewById(R.id.FABSave);

        view.findViewById(R.id.Toolbar).setOnClickListener(v -> requireActivity().onBackPressed());

        setupSpinners();
        
        fabSave.setOnClickListener(v -> saveEmulationConfig());

        return view;
    }

    private void setupSpinners() {
        Box64PresetManager box64Manager = new Box64PresetManager(requireContext());
        ArrayList<Box64Preset> box64Presets = box64Manager.getPresets();
        
        String[] box64Names = new String[box64Presets.size()];
        for (int i = 0; i < box64Presets.size(); i++) {
            box64Names[i] = box64Presets.get(i).getName();
        }
        
        ArrayAdapter<String> box64Adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, box64Names);
        box64Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBox64Preset.setAdapter(box64Adapter);

        String savedBox64 = sharedPreferences.getString(PREF_GLOBAL_BOX64_PRESET, "");
        for (int i = 0; i < box64Names.length; i++) {
            if (box64Names[i].equals(savedBox64)) {
                spinnerBox64Preset.setSelection(i);
                break;
            }
        }

        FEXCorePresetManager fexManager = new FEXCorePresetManager(requireContext());
        ArrayList<FEXCorePreset> fexPresets = fexManager.getPresets();
        
        String[] fexNames = new String[fexPresets.size()];
        for (int i = 0; i < fexPresets.size(); i++) {
            fexNames[i] = fexPresets.get(i).getName();
        }
        
        ArrayAdapter<String> fexAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, fexNames);
        fexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFEXCorePreset.setAdapter(fexAdapter);

        String savedFEX = sharedPreferences.getString(PREF_GLOBAL_FEXCORE_PRESET, "");
        for (int i = 0; i < fexNames.length; i++) {
            if (fexNames[i].equals(savedFEX)) {
                spinnerFEXCorePreset.setSelection(i);
                break;
            }
        }
    }

    private void saveEmulationConfig() {
        String box64Preset = spinnerBox64Preset.getSelectedItem().toString();
        String fexPreset = spinnerFEXCorePreset.getSelectedItem().toString();

        sharedPreferences.edit()
            .putString(PREF_GLOBAL_BOX64_PRESET, box64Preset)
            .putString(PREF_GLOBAL_FEXCORE_PRESET, fexPreset)
            .apply();

        Toast.makeText(requireContext(), "Configurações de emulação salvas!", Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }
}
