package fr.perrier.dungeons.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonProvider {

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

}
