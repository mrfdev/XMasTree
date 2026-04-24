package ru.meloncode.xmas.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;

public class ConfigUtils {

    public static FileConfiguration loadConfig(File file) {
        YamlConfiguration configuration = new YamlConfiguration();
        if (!file.exists()) {
            return configuration;
        }

        try {
            configuration.loadFromString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException exception) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to load YAML configuration from " + file.getPath(), exception);
        }
        return configuration;
    }

}
