package com.winlator.cmod.game;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class Game {
    public int id;
    public String name;
    public String executablePath;
    public String iconPath;
    public int containerId;
    private Bitmap icon;

    public Game(int id, String name, String executablePath, String iconPath, int containerId) {
        this.id = id;
        this.name = name;
        this.executablePath = executablePath;
        this.iconPath = iconPath;
        this.containerId = containerId;
    }

    public Bitmap getIcon() {
        if (icon == null && iconPath != null && !iconPath.isEmpty()) {
            File iconFile = new File(iconPath);
            if (iconFile.exists()) {
                icon = BitmapFactory.decodeFile(iconPath);
            }
        }
        return icon;
    }

    public void setIcon(Bitmap bitmap) {
        this.icon = bitmap;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("executablePath", executablePath);
        json.put("iconPath", iconPath != null ? iconPath : "");
        json.put("containerId", containerId);
        return json;
    }

    public static Game fromJSON(JSONObject json) throws JSONException {
        return new Game(
            json.getInt("id"),
            json.getString("name"),
            json.getString("executablePath"),
            json.optString("iconPath", ""),
            json.getInt("containerId")
        );
    }
}
