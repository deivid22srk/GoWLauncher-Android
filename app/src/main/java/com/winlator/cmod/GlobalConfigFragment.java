package com.winlator.cmod;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.card.MaterialCardView;

public class GlobalConfigFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.global_config_fragment, container, false);

        MaterialCardView cardGraphics = view.findViewById(R.id.CardGraphics);
        MaterialCardView cardEmulation = view.findViewById(R.id.CardEmulation);
        MaterialCardView cardEnvironment = view.findViewById(R.id.CardEnvironment);
        MaterialCardView cardAdvanced = view.findViewById(R.id.CardAdvanced);

        cardGraphics.setOnClickListener(v -> navigateToCategory(new GlobalGraphicsFragment()));
        cardEmulation.setOnClickListener(v -> navigateToCategory(new GlobalEmulationFragment()));
        cardEnvironment.setOnClickListener(v -> navigateToCategory(new GlobalEnvironmentFragment()));
        cardAdvanced.setOnClickListener(v -> navigateToCategory(new SettingsFragment()));

        return view;
    }

    private void navigateToCategory(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        );
        transaction.replace(R.id.FragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
