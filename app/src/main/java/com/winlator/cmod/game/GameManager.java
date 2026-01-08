package com.winlator.cmod.game;

import android.content.Context;
import android.graphics.Bitmap;

import com.winlator.cmod.core.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class GameManager {
    private final Context context;
    private static final String GAMES_FILE = "games.json";

    public GameManager(Context context) {
        this.context = context;
    }

    private File getGamesFile() {
        return new File(context.getFilesDir(), GAMES_FILE);
    }

    public ArrayList<Game> loadGames() {
        ArrayList<Game> games = new ArrayList<>();
        File gamesFile = getGamesFile();
        
        if (!gamesFile.exists()) {
            return games;
        }

        try {
            String content = FileUtils.readString(gamesFile);
            JSONArray jsonArray = new JSONArray(content);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                games.add(Game.fromJSON(jsonObject));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return games;
    }

    public void saveGames(ArrayList<Game> games) {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (Game game : games) {
                jsonArray.put(game.toJSON());
            }

            FileUtils.writeString(getGamesFile(), jsonArray.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addGame(Game game) {
        ArrayList<Game> games = loadGames();
        
        int maxId = 0;
        for (Game g : games) {
            if (g.id > maxId) maxId = g.id;
        }
        game.id = maxId + 1;
        
        games.add(game);
        saveGames(games);
    }

    public void updateGame(Game game) {
        ArrayList<Game> games = loadGames();
        
        for (int i = 0; i < games.size(); i++) {
            if (games.get(i).id == game.id) {
                games.set(i, game);
                break;
            }
        }
        
        saveGames(games);
    }

    public void deleteGame(int gameId) {
        ArrayList<Game> games = loadGames();
        games.removeIf(game -> game.id == gameId);
        saveGames(games);
    }

    public Game getGameById(int gameId) {
        ArrayList<Game> games = loadGames();
        for (Game game : games) {
            if (game.id == gameId) {
                return game;
            }
        }
        return null;
    }

    public File getGameIconFile(int gameId) {
        File iconsDir = new File(context.getFilesDir(), "game_icons");
        if (!iconsDir.exists()) {
            iconsDir.mkdirs();
        }
        return new File(iconsDir, "game_" + gameId + ".png");
    }

    public void saveGameIcon(int gameId, Bitmap icon) {
        File iconFile = getGameIconFile(gameId);
        FileUtils.saveBitmapToFile(icon, iconFile);
    }
}
