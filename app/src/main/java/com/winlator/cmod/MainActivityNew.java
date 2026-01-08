package com.winlator.cmod;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigationbar.NavigationBarView;
import com.winlator.cmod.xenvironment.ImageFs;

public class MainActivityNew extends AppCompatActivity {

    private NavigationBarView navigationBar;
    private SharedPreferences sharedPreferences;
    
    private static final byte PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean isBigPictureModeEnabled = sharedPreferences.getBoolean("enable_big_picture_mode", false);
        if (isBigPictureModeEnabled) {
            Intent intent = new Intent(this, BigPictureActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setTheme(R.style.Theme.GoW.DynamicColors);
        setContentView(R.layout.main_activity_new);

        navigationBar = findViewById(R.id.NavigationBar);
        
        navigationBar.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new GlobalConfigFragment());
                return true;
            }
            return false;
        });

        if (!requestAppPermissions()) {
            if (ImageFs.find(this).isValid()) {
                loadFragment(new HomeFragment());
            } else {
                loadFragment(new HomeFragment());
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showAllFilesAccessDialog();
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        );
        transaction.replace(R.id.FragmentContainer, fragment);
        transaction.commit();
    }

    public void navigateToAddGame() {
        navigationBar.setVisibility(android.view.View.GONE);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        );
        transaction.replace(R.id.FragmentContainer, new GameSelectorFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void navigateToHome() {
        navigationBar.setVisibility(android.view.View.VISIBLE);
        getSupportFragmentManager().popBackStack();
        navigationBar.setSelectedItemId(R.id.nav_home);
    }

    @Override
    public void onBackPressed() {
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        if (backStackCount > 0) {
            navigationBar.setVisibility(android.view.View.VISIBLE);
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void showAllFilesAccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Acesso a Arquivos Necessário")
                .setMessage("Para acessar dispositivos de armazenamento USB, é necessário permissão de acesso a todos os arquivos.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean requestAppPermissions() {
        boolean hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasManageStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();

        if (hasWritePermission && hasReadPermission && hasManageStoragePermission) {
            return false;
        }

        if (!hasWritePermission || !hasReadPermission) {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFragment(new HomeFragment());
            } else {
                finish();
            }
        }
    }
}
