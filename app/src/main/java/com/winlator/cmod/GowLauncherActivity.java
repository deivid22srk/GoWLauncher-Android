package com.winlator.cmod;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.GowLogger;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.core.IconExtractor;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.game.Game;
import com.winlator.cmod.game.GameManager;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.widget.EditText;
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
    private View loadingOverlay;
    private TextView tvLoadingMessage;
    private SharedPreferences sharedPreferences;
    private GameManager gameManager;
    private String mode;

    private static final String PREF_SELECTED_EXE = "gow_selected_exe";
    private static final String PREF_CONTAINER_CREATED = "gow_container_created";
    private static final String GOW_CONTAINER_NAME = "God of War 2018";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        GowLogger.i("GowLauncher", "=== GoW Launcher Iniciado ===");
        GowLogger.i("GowLauncher", "Arquivo de log: " + GowLogger.getLogFilePath());
        setContentView(R.layout.gow_launcher_activity);
        
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "select_exe";
        
        MaterialToolbar toolbar = findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if ("add_game".equals(mode)) {
                getSupportActionBar().setTitle("Adicionar Jogo");
            } else {
                getSupportActionBar().setTitle("God of War");
            }
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gameManager = new GameManager(this);
        loadingOverlay = findViewById(R.id.LoadingOverlay);
        tvLoadingMessage = findViewById(R.id.TVLoadingMessage);

        tvCurrentPath = findViewById(R.id.TVCurrentPath);
        tvSelectedFile = findViewById(R.id.TVSelectedFile);
        btStartGame = findViewById(R.id.BTStartGame);
        recyclerView = findViewById(R.id.RecyclerViewFiles);
        
        findViewById(R.id.BTBackToMain).setOnClickListener(v -> {
            GowLogger.i("GowLauncher", "Voltando ao menu principal");
            finish();
        });
        findViewById(R.id.BTUpDir).setOnClickListener(v -> navigateUp());
        btStartGame.setOnClickListener(v -> {
            if ("add_game".equals(mode)) {
                showAddGameDialog();
            } else {
                startGame();
            }
        });

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
        loadingOverlay.setVisibility(View.VISIBLE);
        tvLoadingMessage.setText("Installing System Files");
        GowLogger.i("GowLauncher", "Iniciando criação do container God of War com Proton 9.0");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Create container with God of War settings
                JSONObject containerData = new JSONObject();
                containerData.put("name", GOW_CONTAINER_NAME);
                containerData.put("screenSize", "1920x1080"); // GoW native resolution
                containerData.put("envVars", Container.DEFAULT_ENV_VARS);
                containerData.put("graphicsDriver", Container.DEFAULT_GRAPHICS_DRIVER);
                containerData.put("graphicsDriverConfig", "vulkanVersion=1.3;version=turnip25.1.0;blacklistedExtensions=;maxDeviceMemory=0;presentMode=mailbox;syncFrame=0;disablePresentWait=0;resourceType=auto;bcnEmulation=auto;bcnEmulationType=software;bcnEmulationCache=0");
                containerData.put("dxwrapper", Container.DEFAULT_DXWRAPPER);
                containerData.put("dxwrapperConfig", "version=2.6.2-1-arm64ec-gplasync,framerate=0,async=0,asyncCache=0,vkd3dVersion=" + DefaultVersion.VKD3D + ",vkd3dLevel=12_1,ddrawrapper=" + Container.DEFAULT_DDRAWRAPPER + ",csmt=3,gpuName=NVIDIA GeForce GTX 480,videoMemorySize=2048,strict_shader_math=1,OffscreenRenderingMode=fbo,renderer=gl");
                containerData.put("audioDriver", Container.DEFAULT_AUDIO_DRIVER);
                containerData.put("emulator", Container.DEFAULT_EMULATOR);
                containerData.put("wincomponents", Container.DEFAULT_WINCOMPONENTS);
                containerData.put("drives", Container.DEFAULT_DRIVES);
                containerData.put("showFPS", false);
                containerData.put("fullscreenStretched", false);
                containerData.put("inputType", WinHandler.DEFAULT_INPUT_TYPE);
                containerData.put("startupSelection", Container.STARTUP_SELECTION_ESSENTIAL);
                containerData.put("wineVersion", WineInfo.MAIN_WINE_VERSION.identifier());
                containerData.put("cpuList", "");
                containerData.put("cpuListWoW64", "");
                containerData.put("box64Version", DefaultVersion.WOWBOX64);
                containerData.put("box64Preset", Box64Preset.COMPATIBILITY);
                containerData.put("fexcoreVersion", DefaultVersion.FEXCORE);
                containerData.put("fexcorePreset", FEXCorePreset.GOW_OPTIMIZED);
                containerData.put("desktopTheme", WineThemeManager.DEFAULT_DESKTOP_THEME);
                containerData.put("midiSoundFont", "");
                containerData.put("lc_all", "");
                containerData.put("primaryController", 1);
                containerData.put("controllerMapping", "");

                ContentsManager contentsManager = new ContentsManager(this);
                
                containerManager.createContainerAsync(containerData, contentsManager, (container) -> {
                    runOnUiThread(() -> {
                        if (container != null) {
                            gowContainer = container;
                            sharedPreferences.edit().putBoolean(PREF_CONTAINER_CREATED, true).apply();
                            GowLogger.i("GowLauncher", "Container criado com sucesso: " + container.getName() + " (ID: " + container.id + ")");
                            loadingOverlay.setVisibility(View.GONE);
                            Toast.makeText(this, "Sistema configurado com sucesso!", Toast.LENGTH_SHORT).show();
                            initializeFileBrowser();
                        } else {
                            loadingOverlay.setVisibility(View.GONE);
                            Toast.makeText(this, "Erro ao configurar sistema", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
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
            if ("add_game".equals(mode)) {
                btStartGame.setText("Adicionar Jogo");
            } else {
                btStartGame.setText("Iniciar Jogo");
            }
        } else {
            tvSelectedFile.setText("Nenhum arquivo selecionado");
            btStartGame.setEnabled(false);
        }
    }

    private void showAddGameDialog() {
        if (selectedExeFile == null || !selectedExeFile.exists()) {
            Toast.makeText(this, "Selecione um arquivo .exe primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gowContainer == null) {
            Toast.makeText(this, "Container não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nome do Jogo");

        final EditText input = new EditText(this);
        input.setText(selectedExeFile.getName().replace(".exe", ""));
        builder.setView(input);

        builder.setPositiveButton("Adicionar", (dialog, which) -> {
            String gameName = input.getText().toString().trim();
            if (gameName.isEmpty()) {
                Toast.makeText(this, "Digite um nome para o jogo", Toast.LENGTH_SHORT).show();
                return;
            }

            addGame(gameName);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addGame(String gameName) {
        Bitmap icon = IconExtractor.extractIcon(selectedExeFile);
        if (icon == null) {
            icon = IconExtractor.createDefaultIcon(gameName);
        }

        Game game = new Game(0, gameName, selectedExeFile.getAbsolutePath(), "", gowContainer.id);
        gameManager.addGame(game);

        File iconFile = gameManager.getGameIconFile(game.id);
        gameManager.saveGameIcon(game.id, icon);
        game.iconPath = iconFile.getAbsolutePath();
        gameManager.updateGame(game);

        Toast.makeText(this, "Jogo adicionado: " + gameName, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void startGame() {
        GowLogger.i("GowLauncher", "Abrindo tela de configurações para: " + selectedExeFile.getAbsolutePath());
        
        if (selectedExeFile == null || !selectedExeFile.exists()) {
            Toast.makeText(this, "Selecione um arquivo .exe primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gowContainer == null) {
            Toast.makeText(this, "Container não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, GowConfigActivity.class);
        intent.putExtra("exe_path", selectedExeFile.getAbsolutePath());
        intent.putExtra("container_id", gowContainer.id);
        startActivity(intent);
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
                holder.ivIcon.setImageResource(R.drawable.icon_folder);
                holder.ivIcon.clearColorFilter();
                int count = file.list() != null ? file.list().length : 0;
                holder.tvDetails.setText(count + " itens");
                holder.itemView.setOnClickListener(v -> loadDirectory(file));
            } else {
                holder.tvDetails.setText(formatSize(file.length()));
                boolean isExe = isExecutable(file);

                if (isExe) {
                    holder.ivIcon.setImageResource(R.drawable.icon_wine);
                    
                    if (file.equals(selectedExeFile)) {
                        holder.ivIcon.setColorFilter(getResources().getColor(R.color.md_theme_gow_primary, null));
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setCardBackgroundColor(getResources().getColor(R.color.md_theme_gow_primary_container, null));
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setStrokeWidth(4);
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setStrokeColor(getResources().getColor(R.color.md_theme_gow_primary, null));
                    } else {
                        holder.ivIcon.setColorFilter(getResources().getColor(R.color.md_theme_gow_secondary, null));
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setCardBackgroundColor(getResources().getColor(R.color.md_theme_gow_surface_container, null));
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setStrokeWidth(0);
                    }
                    
                    holder.itemView.setOnClickListener(v -> {
                        selectExeFile(file);
                        notifyDataSetChanged();
                    });
                } else {
                    holder.ivIcon.setImageResource(R.drawable.icon_file);
                    holder.ivIcon.setColorFilter(getResources().getColor(R.color.md_theme_gow_tertiary, null));
                    ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setCardBackgroundColor(getResources().getColor(R.color.md_theme_gow_surface_container, null));
                    ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setStrokeWidth(0);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            GowLogger.i("GowLauncher", "Voltando para MainActivity via ActionBar");
            navigateBackToMain();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        GowLogger.i("GowLauncher", "Voltando para MainActivity via botão de voltar");
        navigateBackToMain();
    }

    private void navigateBackToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
