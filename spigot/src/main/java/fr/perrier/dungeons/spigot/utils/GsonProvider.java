package fr.perrier.dungeons.spigot.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonProvider {

    private GsonProvider() {
        // Private constructor to prevent instantiation
    }

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

}
