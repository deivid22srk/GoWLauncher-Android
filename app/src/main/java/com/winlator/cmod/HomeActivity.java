package com.winlator.cmod;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.game.Game;
import com.winlator.cmod.game.GameManager;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private GameManager gameManager;
    private GamesAdapter adapter;
    private ContainerManager containerManager;
    private BottomNavigationView bottomNavigationView;

    public static final int REQUEST_ADD_GAME = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        MaterialToolbar toolbar = findViewById(R.id.Toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Meus Jogos");
        }

        gameManager = new GameManager(this);
        containerManager = new ContainerManager(this);

        recyclerView = findViewById(R.id.RecyclerViewGames);
        emptyTextView = findViewById(R.id.TVEmptyText);
        FloatingActionButton fabAddGame = findViewById(R.id.FABAddGame);
        bottomNavigationView = findViewById(R.id.BottomNavigationView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        fabAddGame.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, GowLauncherActivity.class);
            intent.putExtra("mode", "add_game");
            startActivityForResult(intent, REQUEST_ADD_GAME);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_settings) {
                Intent intent = new Intent(HomeActivity.this, GlobalConfigActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        loadGames();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGames();
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void loadGames() {
        ArrayList<Game> games = gameManager.loadGames();
        
        if (games.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new GamesAdapter(games);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_GAME && resultCode == RESULT_OK) {
            loadGames();
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
                PopupMenu popup = new PopupMenu(HomeActivity.this, holder.btnMore);
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
            Toast.makeText(this, "Container não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        File exeFile = new File(game.executablePath);
        if (!exeFile.exists()) {
            Toast.makeText(this, "Arquivo executável não encontrado", Toast.LENGTH_SHORT).show();
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

            Intent intent = new Intent(this, XServerDisplayActivity.class);
            intent.putExtra("container_id", container.id);
            intent.putExtra("shortcut_path", desktopFile.getAbsolutePath());
            intent.putExtra("shortcut_name", displayName);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao iniciar jogo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteGame(Game game) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Excluir Jogo")
                .setMessage("Deseja excluir " + game.name + "?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    gameManager.deleteGame(game.id);
                    
                    File iconFile = new File(game.iconPath);
                    if (iconFile.exists()) {
                        iconFile.delete();
                    }
                    
                    loadGames();
                    Toast.makeText(HomeActivity.this, "Jogo excluído", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Não", null)
                .show();
    }
}
