package net.fabricmc.dakes.invoverstack.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.dakes.invoverstack.InvOverstackMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final String CONFIG_FILE_NAME = "invoverstack.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static ModConfig config = null;
    private static Path configPath = null;

    private static Path getConfigPath() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve(CONFIG_FILE_NAME);
        }
        return configPath;
    }

    public static ModConfig loadConfig() {
        Path path = getConfigPath();

        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                config = GSON.fromJson(json, ModConfig.class);

                if (config == null) {
                    InvOverstackMod.LOGGER.warn("Config file was empty or invalid, using defaults");
                    config = new ModConfig();
                } else {
                    config.validate();
                    InvOverstackMod.LOGGER.info("Configuration loaded from {}", path);
                }
            } catch (IOException e) {
                InvOverstackMod.LOGGER.error("Failed to load config from {}, using defaults", path, e);
                config = new ModConfig();
            } catch (Exception e) {
                InvOverstackMod.LOGGER.error("Failed to parse config JSON, using defaults", e);
                config = new ModConfig();
            }
        } else {
            InvOverstackMod.LOGGER.info("Config file not found, creating default at {}", path);
            config = new ModConfig();
            saveConfig();
        }

        return config;
    }

    public static boolean saveConfig() {
        if (config == null) {
            InvOverstackMod.LOGGER.warn("Attempted to save null config");
            return false;
        }

        Path path = getConfigPath();

        try {
            config.validate();

            Path configDir = path.getParent();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            String json = GSON.toJson(config);
            Files.writeString(path, json);

            InvOverstackMod.LOGGER.info("Configuration saved to {}", path);
            return true;
        } catch (IOException e) {
            InvOverstackMod.LOGGER.error("Failed to save config to {}", path, e);
            return false;
        }
    }

    public static boolean reloadConfig() {
        InvOverstackMod.LOGGER.info("Reloading configuration...");
        try {
            config = null;
            loadConfig();
            return true;
        } catch (Exception e) {
            InvOverstackMod.LOGGER.error("Failed to reload config", e);
            return false;
        }
    }

    public static ModConfig getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }

    public static void setConfig(ModConfig newConfig) {
        config = newConfig;
        saveConfig();
    }
}
