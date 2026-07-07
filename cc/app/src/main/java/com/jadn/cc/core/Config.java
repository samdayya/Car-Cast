package com.jadn.cc.core;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Config {
    Context context;

    public Config(Context context) {
        this.context = context;
        getPodcastsRoot().mkdirs();
    }

    public int getMax() {
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return Integer.parseInt(app_preferences.getString("listmax", "2"));
    }

    public File getCarCastRoot() {
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
        File externalRoot = context.getExternalFilesDir(null);
        File defaultRoot = externalRoot != null ? externalRoot : context.getFilesDir();
        File legacyRoot = new File(defaultRoot, "carcast");
        String file = app_preferences.getString("CarCastRoot", null);

        if (file == null || file.equals(legacyRoot.getAbsolutePath()) || file.equals(legacyRoot.toString())) {
            SharedPreferences.Editor editor = app_preferences.edit();
            // Mantindre l'estructura original de l'aplicació per coincidir amb la ruta de l'APK autor.
            editor.putString("CarCastRoot", defaultRoot.getAbsolutePath());
            editor.commit();
            file = app_preferences.getString("CarCastRoot", null);
        }
        return new File(file);
    }

    public File getPodcastsRoot() {
        return new File(getCarCastRoot(), "podcasts");
    }

    public File getPodcastRootPath(String path) {
        return new File(getPodcastsRoot(), path);
    }

    public File getCarCastPath(String path) {
        return new File(getCarCastRoot(), path);
    }
}
