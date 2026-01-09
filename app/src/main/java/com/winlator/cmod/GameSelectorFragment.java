package com.winlator.cmod;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.GowLogger;
import com.winlator.cmod.core.IconExtractor;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.game.Game;
import com.winlator.cmod.game.GameManager;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class GameSelectorFragment extends Fragment {

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

    private static final String PREF_CONTAINER_CREATED = "gow_container_created";
    private static final String GOW_CONTAINER_NAME = "God of War 2018";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.game_selector_fragment, container, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        gameManager = new GameManager(requireContext());
        loadingOverlay = view.findViewById(R.id.LoadingOverlay);
        tvLoadingMessage = view.findViewById(R.id.TVLoadingMessage);

        tvCurrentPath = view.findViewById(R.id.TVCurrentPath);
        tvSelectedFile = view.findViewById(R.id.TVSelectedFile);
        btStartGame = view.findViewById(R.id.BTStartGame);
        recyclerView = view.findViewById(R.id.RecyclerViewFiles);
        
        view.findViewById(R.id.BTUpDir).setOnClickListener(v -> navigateUp());
        btStartGame.setOnClickListener(v -> showAddGameDialog());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        containerManager = new ContainerManager(requireContext());

        if (!sharedPreferences.getBoolean(PREF_CONTAINER_CREATED, false)) {
            createGowContainer();
        } else {
            loadGowContainer();
            initializeFileBrowser();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            requireActivity().setTitle("Selecionar Jogo");
        }
    }

    private void createGowContainer() {
        loadingOverlay.setVisibility(View.VISIBLE);
        tvLoadingMessage.setText("Instalando Sistema...");
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONObject containerData = new JSONObject();
                containerData.put("name", GOW_CONTAINER_NAME);
                containerData.put("screenSize", "1920x1080");
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
                containerData.put("box64Version", "");
                containerData.put("box64Preset", Box64Preset.COMPATIBILITY);
                containerData.put("fexcoreVersion", "2601");
                containerData.put("fexcorePreset", FEXCorePreset.GOW_OPTIMIZED);
                containerData.put("desktopTheme", WineThemeManager.DEFAULT_DESKTOP_THEME);
                containerData.put("midiSoundFont", "");
                containerData.put("lc_all", "");
                containerData.put("primaryController", 1);
                containerData.put("controllerMapping", "");

                ContentsManager contentsManager = new ContentsManager(requireContext());
                
                containerManager.createContainerAsync(containerData, contentsManager, (container) -> {
                    if (getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            if (container != null) {
                                gowContainer = container;
                                sharedPreferences.edit().putBoolean(PREF_CONTAINER_CREATED, true).apply();
                                loadingOverlay.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Sistema configurado!", Toast.LENGTH_SHORT).show();
                                initializeFileBrowser();
                            } else {
                                loadingOverlay.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Erro ao configurar sistema", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
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
        
        if (gowContainer == null) {
            sharedPreferences.edit().putBoolean(PREF_CONTAINER_CREATED, false).apply();
            createGowContainer();
        }
    }

    private void initializeFileBrowser() {
        currentDir = Environment.getExternalStorageDirectory();
        loadDirectory(currentDir);
    }

    private void navigateUp() {
        if (currentDir != null && currentDir.getParentFile() != null && currentDir.getParentFile().canRead()) {
            loadDirectory(currentDir.getParentFile());
        } else {
            Toast.makeText(requireContext(), "Root alcançado", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.canRead()) {
            Toast.makeText(requireContext(), "Não é possível acessar este diretório", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentDir = dir;
        tvCurrentPath.setText(dir.getAbsolutePath());

        File[] files = dir.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            fileList.addAll(Arrays.asList(files));
        } else {
            Toast.makeText(requireContext(), "Não é possível listar arquivos neste diretório", Toast.LENGTH_SHORT).show();
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

    private void showAddGameDialog() {
        if (selectedExeFile == null || !selectedExeFile.exists()) {
            Toast.makeText(requireContext(), "Selecione um arquivo .exe primeiro", Toast.LENGTH_SHORT).show();
            return;
        }

        if (gowContainer == null) {
            Toast.makeText(requireContext(), "Container não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nome do Jogo");

        final EditText input = new EditText(requireContext());
        input.setText(selectedExeFile.getName().replace(".exe", ""));
        builder.setView(input);

        builder.setPositiveButton("Adicionar", (dialog, which) -> {
            String gameName = input.getText().toString().trim();
            if (gameName.isEmpty()) {
                Toast.makeText(requireContext(), "Digite um nome para o jogo", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(requireContext(), "Jogo adicionado: " + gameName, Toast.LENGTH_SHORT).show();
        
        if (getActivity() instanceof MainActivityNew) {
            ((MainActivityNew) getActivity()).navigateToHome();
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
                holder.ivIcon.setImageResource(R.drawable.icon_folder);
                holder.ivIcon.clearColorFilter();
                String[] contents = file.list();
                int count = contents != null ? contents.length : 0;
                holder.tvDetails.setText(count + " itens");
                holder.itemView.setOnClickListener(v -> loadDirectory(file));
            } else {
                holder.tvDetails.setText(formatSize(file.length()));
                boolean isExe = isExecutable(file);

                if (isExe) {
                    holder.ivIcon.setImageResource(R.drawable.icon_wine);
                    
                    if (file.equals(selectedExeFile)) {
                        holder.ivIcon.setColorFilter(requireContext().getResources().getColor(R.color.md_theme_gow_primary, null));
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setCardBackgroundColor(requireContext().getResources().getColor(R.color.md_theme_gow_primary_container, null));
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setStrokeWidth(4);
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setStrokeColor(requireContext().getResources().getColor(R.color.md_theme_gow_primary, null));
                    } else {
                        holder.ivIcon.setColorFilter(requireContext().getResources().getColor(R.color.md_theme_gow_secondary, null));
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setCardBackgroundColor(requireContext().getResources().getColor(R.color.md_theme_gow_surface_container, null));
                        ((com.google.android.material.card.MaterialCardView) holder.itemView)
                            .setStrokeWidth(0);
                    }
                    
                    holder.itemView.setOnClickListener(v -> {
                        selectExeFile(file);
                        notifyDataSetChanged();
                    });
                } else {
                    holder.ivIcon.setImageResource(R.drawable.icon_file);
                    holder.ivIcon.setColorFilter(requireContext().getResources().getColor(R.color.md_theme_gow_tertiary, null));
                    ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setCardBackgroundColor(requireContext().getResources().getColor(R.color.md_theme_gow_surface_container, null));
                    ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setStrokeWidth(0);
                    holder.itemView.setOnClickListener(v -> 
                        Toast.makeText(requireContext(), "Selecione apenas arquivos .exe", Toast.LENGTH_SHORT).show()
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
