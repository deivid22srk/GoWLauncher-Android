package com.winlator.cmod;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.GowLogger;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class GowLauncherActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvCurrentPath;
    private TextView tvSelectedFile;
    private Button btStartGame;
    private File currentDir;
    private File selectedExeFile;
    private FileAdapter adapter;
    private ContainerManager containerManager;
    private Container gowContainer;
    private PreloaderDialog preloaderDialog;
    private SharedPreferences sharedPreferences;

    private static final String PREF_SELECTED_EXE = "gow_selected_exe";
    private static final String PREF_CONTAINER_CREATED = "gow_container_created";
    private static final String GOW_CONTAINER_NAME = "God of War 2018";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        GowLogger.i("GowLauncher", "=== GoW Launcher Iniciado ===");
        GowLogger.i("GowLauncher", "Arquivo de log: " + GowLogger.getLogFilePath());
        setContentView(R.layout.gow_launcher_activity);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        preloaderDialog = new PreloaderDialog(this);

        tvCurrentPath = findViewById(R.id.TVCurrentPath);
        tvSelectedFile = findViewById(R.id.TVSelectedFile);
        btStartGame = findViewById(R.id.BTStartGame);
        recyclerView = findViewById(R.id.RecyclerViewFiles);
        
        findViewById(R.id.BTBackToMain).setOnClickListener(v -> {
            GowLogger.i("GowLauncher", "Voltando ao menu principal");
            finish();
        });
        findViewById(R.id.BTUpDir).setOnClickListener(v -> navigateUp());
        btStartGame.setOnClickListener(v -> startGame());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        containerManager = new ContainerManager(this);

        // Check if container needs to be created
        if (!sharedPreferences.getBoolean(PREF_CONTAINER_CREATED, false)) {
            createGowContainer();
        } else {
            loadGowContainer();
            initializeFileBrowser();
        }
    }

    private void createGowContainer() {
        preloaderDialog.show(R.string.loading);
        GowLogger.i("GowLauncher", "Iniciando criação do container God of War com Proton 9.0");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Create container with God of War settings
                JSONObject containerData = new JSONObject();
                containerData.put("name", GOW_CONTAINER_NAME);
                containerData.put("screenSize", "1920x1080"); // GoW native resolution
                containerData.put("envVars", Container.DEFAULT_ENV_VARS);
                containerData.put("graphicsDriver", Container.DEFAULT_GRAPHICS_DRIVER);
                containerData.put("dxwrapper", Container.DEFAULT_DXWRAPPER);
                containerData.put("audioDriver", Container.DEFAULT_AUDIO_DRIVER);
                containerData.put("emulator", Container.DEFAULT_EMULATOR);
                containerData.put("wincomponents", Container.DEFAULT_WINCOMPONENTS);
                containerData.put("drives", Container.DEFAULT_DRIVES);
                containerData.put("showFPS", false);
                containerData.put("startupSelection", Container.STARTUP_SELECTION_ESSENTIAL);
                containerData.put("wineVersion", WineInfo.MAIN_WINE_VERSION.identifier());

                ContentsManager contentsManager = new ContentsManager(this);
                
                containerManager.createContainerAsync(containerData, contentsManager, (container) -> {
                    runOnUiThread(() -> {
                        if (container != null) {
                            gowContainer = container;
                            sharedPreferences.edit().putBoolean(PREF_CONTAINER_CREATED, true).apply();
                            GowLogger.i("GowLauncher", "Container criado com sucesso: " + container.getName() + " (ID: " + container.id + ")");
                            preloaderDialog.close();
                            Toast.makeText(this, "Container criado com sucesso!", Toast.LENGTH_SHORT).show();
                            initializeFileBrowser();
                        } else {
                            preloaderDialog.close();
                            Toast.makeText(this, "Erro ao criar container", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    preloaderDialog.close();
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void loadGowContainer() {
        ArrayList<Container> containers = containerManager.getContainers();
        for (Container container : containers) {
            if (container.getName().equals(GOW_CONTAINER_NAME)) {
                gowContainer = container;
                break;
            }
        }
        
        // If container was deleted, recreate it
        if (gowContainer == null) {
            sharedPreferences.edit().putBoolean(PREF_CONTAINER_CREATED, false).apply();
            createGowContainer();
        }
    }

    private void initializeFileBrowser() {
        // Try to load previously selected exe
        String savedExePath = sharedPreferences.getString(PREF_SELECTED_EXE, "");
        if (!savedExePath.isEmpty()) {
            selectedExeFile = new File(savedExePath);
            if (selectedExeFile.exists()) {
                updateSelectedFile();
                currentDir = selectedExeFile.getParentFile();
            }
        }
        
        // Start at external storage if no previous selection
        if (currentDir == null) {
            currentDir = Environment.getExternalStorageDirectory();
        }
        
        loadDirectory(currentDir);
    }

    private void navigateUp() {
        if (currentDir != null && currentDir.getParentFile() != null && currentDir.getParentFile().canRead()) {
            loadDirectory(currentDir.getParentFile());
        } else {
            Toast.makeText(this, "Root alcançado", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDirectory(File dir) {
        currentDir = dir;
        tvCurrentPath.setText(dir.getAbsolutePath());

        File[] files = dir.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            fileList.addAll(Arrays.asList(files));
        }

        Collections.sort(fileList, (f1, f2) -> {
            boolean d1 = f1.isDirectory();
            boolean d2 = f2.isDirectory();
            
            if (d1 && !d2) return -1;
            if (!d1 && d2) return 1;

            if (!d1 && !d2) {
                boolean isExe1 = isExecutable(f1);
                boolean isExe2 = isExecutable(f2);
                if (isExe1 && !isExe2) return -1;
                if (!isExe1 && isExe2) return 1;
            }

            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        adapter = new FileAdapter(fileList);
        recyclerView.setAdapter(adapter);
    }

    private boolean isExecutable(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".exe");
    }

    private void selectExeFile(File file) {
        selectedExeFile = file;
        GowLogger.i("GowLauncher", "Arquivo selecionado: " + file.getAbsolutePath());
        sharedPreferences.edit().putString(PREF_SELECTED_EXE, file.getAbsolutePath()).apply();
        updateSelectedFile();
    }

    private void updateSelectedFile() {
        if (selectedExeFile != null && selectedExeFile.exists()) {
            tvSelectedFile.setText("Arquivo: " + selectedExeFile.getName());
            btStartGame.setEnabled(true);
        } else {
            tvSelectedFile.setText("Nenhum arquivo selecionado");
            btStartGame.setEnabled(false);
        }
    }

    private void startGame() {
        GowLogger.i("GowLauncher", "Iniciando jogo: " + selectedExeFile.getAbsolutePath());
        if (selectedExeFile == null || !selectedExeFile.exists()) {
            Toast.makeText(this, "Selecione um arquivo .exe primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gowContainer == null) {
            Toast.makeText(this, "Container não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create a shortcut file similar to how FileManagerFragment does it
            String displayName = "God of War";
            String unixPath = selectedExeFile.getAbsolutePath();
            String workDir = selectedExeFile.getParent();

            File shortcutsDir = gowContainer.getDesktopDir();
            if (!shortcutsDir.exists()) shortcutsDir.mkdirs();

            File desktopFile = new File(shortcutsDir, displayName + ".desktop");

            try (PrintWriter writer = new PrintWriter(new FileWriter(desktopFile))) {
                writer.println("[Desktop Entry]");
                writer.println("Name=" + displayName);
                writer.println("Exec=env WINEPREFIX=\"/home/xuser/.wine\" wine \"" + unixPath + "\"");
                writer.println("Type=Application");
                writer.println("Terminal=false");
                writer.println("StartupNotify=true");
                writer.println("Icon=" + displayName);
                writer.println("Path=" + workDir);
                writer.println("container_id:" + gowContainer.id);
                writer.println("");
                writer.println("[Extra Data]");
                writer.println("container_id=" + gowContainer.id);
            }

            // Launch the game via XServerDisplayActivity
            Intent intent = new Intent(this, XServerDisplayActivity.class);
            intent.putExtra("container_id", gowContainer.id);
            intent.putExtra("shortcut_path", desktopFile.getAbsolutePath());
            intent.putExtra("shortcut_name", displayName);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao iniciar jogo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
        private final List<File> files;

        public FileAdapter(List<File> files) {
            this.files = files;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            File file = files.get(position);
            holder.tvName.setText(file.getName());

            if (file.isDirectory()) {
                holder.ivIcon.setImageResource(R.drawable.icon_open);
                int count = file.list() != null ? file.list().length : 0;
                holder.tvDetails.setText(count + " itens");
                holder.itemView.setOnClickListener(v -> loadDirectory(file));
            } else {
                holder.tvDetails.setText(formatSize(file.length()));
                boolean isExe = isExecutable(file);

                if (isExe) {
                    holder.ivIcon.setImageResource(R.drawable.icon_wine);
                    
                    // Highlight if this is the selected file
                    if (file.equals(selectedExeFile)) {
                        holder.itemView.setBackgroundColor(0xFF4CAF50); // Green highlight
                    } else {
                        holder.itemView.setBackgroundColor(0x00000000); // Transparent
                    }
                    
                    holder.itemView.setOnClickListener(v -> {
                        selectExeFile(file);
                        notifyDataSetChanged(); // Refresh to update highlighting
                    });
                } else {
                    holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                    holder.itemView.setBackgroundColor(0x00000000);
                    holder.itemView.setOnClickListener(v -> 
                        Toast.makeText(GowLauncherActivity.this, "Selecione apenas arquivos .exe", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDetails;
            ImageView ivIcon;

            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.TVFileName);
                tvDetails = v.findViewById(R.id.TVFileDetails);
                ivIcon = v.findViewById(R.id.IVIcon);
            }
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}
