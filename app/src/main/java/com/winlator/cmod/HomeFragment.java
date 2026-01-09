package com.winlator.cmod;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.game.Game;
import com.winlator.cmod.game.GameManager;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xenvironment.ImageFsInstaller;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyCard;
    private GameManager gameManager;
    private GamesAdapter adapter;
    private ContainerManager containerManager;
    private PreloaderDialog preloaderDialog;

    public static final int REQUEST_ADD_GAME = 1001;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameManager = new GameManager(requireContext());
        containerManager = new ContainerManager(requireContext());
        preloaderDialog = new PreloaderDialog(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);

        recyclerView = view.findViewById(R.id.RecyclerViewGames);
        emptyCard = view.findViewById(R.id.EmptyCard);
        ExtendedFloatingActionButton fabAddGame = view.findViewById(R.id.FABAddGame);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        fabAddGame.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivityNew) {
                ((MainActivityNew) getActivity()).navigateToAddGame();
            }
        });

        checkInstallation();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadGames();
        if (getActivity() != null) {
            requireActivity().setTitle("Meus Jogos");
        }
    }

    private void checkInstallation() {
        if (!ImageFs.find(requireContext()).isValid()) {
            preloaderDialog.show(R.string.installing_system_files);
            if (getActivity() instanceof AppCompatActivity) {
                ImageFsInstaller.installIfNeeded((AppCompatActivity) getActivity(), () -> {
                    preloaderDialog.close();
                    if (isAdded()) {
                        loadGames();
                    }
                });
            }
        }
    }

    private void loadGames() {
        ArrayList<Game> games = gameManager.loadGames();
        
        if (games.isEmpty()) {
            emptyCard.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyCard.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new GamesAdapter(games);
            recyclerView.setAdapter(adapter);
        }
    }

    private class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.ViewHolder> {
        private final ArrayList<Game> games;

        public GamesAdapter(ArrayList<Game> games) {
            this.games = games;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.game_card_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Game game = games.get(position);
            holder.tvGameName.setText(game.name);

            Bitmap icon = game.getIcon();
            if (icon != null) {
                holder.ivGameIcon.setImageBitmap(icon);
            } else {
                holder.ivGameIcon.setImageResource(R.drawable.icon_wine);
            }

            holder.itemView.setOnClickListener(v -> launchGame(game));

            holder.btnMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), holder.btnMore);
                popup.getMenuInflater().inflate(R.menu.game_item_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(menuItem -> {
                    int itemId = menuItem.getItemId();
                    if (itemId == R.id.action_delete) {
                        deleteGame(game);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        @Override
        public int getItemCount() {
            return games.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivGameIcon;
            TextView tvGameName;
            ImageButton btnMore;

            ViewHolder(View view) {
                super(view);
                ivGameIcon = view.findViewById(R.id.IVGameIcon);
                tvGameName = view.findViewById(R.id.TVGameName);
                btnMore = view.findViewById(R.id.BTMore);
            }
        }
    }

    private void launchGame(Game game) {
        Container container = containerManager.getContainerById(game.containerId);
        
        if (container == null) {
            Toast.makeText(requireContext(), "Container não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        File exeFile = new File(game.executablePath);
        if (!exeFile.exists()) {
            Toast.makeText(requireContext(), "Arquivo executável não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String displayName = game.name;
            String androidPath = game.executablePath;
            String workDir = exeFile.getParent();

            File shortcutsDir = container.getDesktopDir();
            if (!shortcutsDir.exists()) shortcutsDir.mkdirs();

            File desktopFile = new File(shortcutsDir, displayName + ".desktop");

            try (PrintWriter writer = new PrintWriter(new FileWriter(desktopFile))) {
                writer.println("[Desktop Entry]");
                writer.println("Name=" + displayName);
                writer.println("Exec=env WINEPREFIX=\"/home/xuser/.wine\" wine \"" + androidPath + "\"");
                writer.println("Type=Application");
                writer.println("Terminal=false");
                writer.println("StartupNotify=true");
                writer.println("Icon=" + displayName);
                writer.println("Path=" + workDir);
                writer.println("container_id:" + container.id);
                writer.println("");
                writer.println("[Extra Data]");
                writer.println("container_id=" + container.id);
            }

            Intent intent = new Intent(requireContext(), XServerDisplayActivity.class);
            intent.putExtra("container_id", container.id);
            intent.putExtra("shortcut_path", desktopFile.getAbsolutePath());
            intent.putExtra("shortcut_name", displayName);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Erro ao iniciar jogo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteGame(Game game) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Excluir Jogo")
                .setMessage("Deseja excluir " + game.name + "?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    gameManager.deleteGame(game.id);
                    
                    File iconFile = new File(game.iconPath);
                    if (iconFile.exists()) {
                        iconFile.delete();
                    }
                    
                    loadGames();
                    Toast.makeText(requireContext(), "Jogo excluído", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Não", null)
                .show();
    }
}
