package com.leclowndu93150.corpse.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.leclowndu93150.corpse.data.CorpseData;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataDirectory;
    private final HytaleLogger logger;

    public DataManager(@Nonnull Path dataDirectory, @Nonnull HytaleLogger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public Map<String, CorpseData> loadCorpses() {
        Type type = new TypeToken<Map<String, CorpseData>>(){}.getType();
        Path file = dataDirectory.resolve("corpses.json");
        if (!Files.exists(file, new LinkOption[0])) {
            logger.atInfo().log("No existing corpses.json file found, starting fresh.");
            return new ConcurrentHashMap<>();
        }
        try {
            String json = Files.readString(file);
            Map<String, CorpseData> loaded = GSON.fromJson(json, type);
            if (loaded != null) {
                logger.atInfo().log("Loaded %d corpses from corpses.json", loaded.size());
                return new ConcurrentHashMap<>(loaded);
            }
        } catch (IOException e) {
            logger.atSevere().log("Failed to load corpses.json: %s", e.getMessage());
        }
        return new ConcurrentHashMap<>();
    }

    public void saveCorpses(Map<String, CorpseData> corpses) {
        try {
            if (!Files.exists(dataDirectory, new LinkOption[0])) {
                Files.createDirectories(dataDirectory);
            }
            Path file = dataDirectory.resolve("corpses.json");
            Type type = new TypeToken<Map<String, CorpseData>>(){}.getType();
            String json = GSON.toJson(corpses, type);
            Files.writeString(file, json);
            logger.atFine().log("Saved %d corpses to corpses.json", corpses.size());
        } catch (IOException e) {
            logger.atSevere().log("Failed to save corpses.json: %s", e.getMessage());
        }
    }
}
