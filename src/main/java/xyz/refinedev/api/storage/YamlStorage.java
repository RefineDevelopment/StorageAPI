package xyz.refinedev.api.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.refinedev.api.storage.utils.ConfigSaver;
import xyz.refinedev.api.storage.utils.ConfigValue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This Project is property of Refine Development © 2021 - 2022
 * Redistribution of this Project is not allowed
 *
 * @author Drizzy
 * Created: 7/27/2022
 * Project: StorageAPI
 */

public abstract class YamlStorage {

    private static final Logger LOGGER = LogManager.getLogger(YamlStorage.class);

    /*========================================================================*/
    private final String name;
    private final File file;
    private final YamlConfiguration config;
    private final Map<String, String> comments;
    /*========================================================================*/

    /**
     * Initiation method for a config file
     *
     * @param plugin       {@link JavaPlugin plugin instance}
     * @param name         {@link String config file name}
     * @param saveResource {@link Boolean should we save our built-in config}
     */
    public YamlStorage(JavaPlugin plugin, String name, boolean saveResource) {
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name + ".yml");
        this.config = new YamlConfiguration();
        this.comments = new HashMap<>();

        if (!file.exists()) {
            try {
                if (saveResource) {
                    plugin.saveResource(name + ".yml", false);
                } else {
                    boolean created = file.createNewFile();
                    if (!created) throw new IOException("Failed to create file");
                }
            } catch (IOException ex) {
                LOGGER.error("[Storage] Could not create/save " + name + ".yml!");
                LOGGER.error("[Storage] Error: " + ex.getMessage());
            }
        }

        try {
            config.load(file);
        } catch (InvalidConfigurationException | IOException ex) {
            LOGGER.error("[Storage] Could not load " + name + ".yml, please correct your syntax errors!");
            LOGGER.error("[Storage] Error: " + ex.getMessage());
        }

        config.options().header(String.join("\n", this.getHeader()));
        this.readConfig();
    }

    /**
     * Read config values from the config, if some are not present
     * we save the default value of the {@link ConfigValue} to the config
     */
    public void readConfig() {
        for ( Field field : this.getConfigFields() ) {
            try {
                ConfigValue configValue = field.getAnnotation(ConfigValue.class);
                Object value = field.get(null);
                if (config.contains(configValue.path()) && config.get(configValue.path()) != null) {
                    field.set(this, config.get(configValue.path()));
                } else {
                    config.set(configValue.path(), value);
                }
                comments.putIfAbsent(configValue.path(), configValue.comment());
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.error("[Storage] Error invoking " + field, ex);
            }
        }

        this.addSeparateComments();
        this.saveConfig(true);
    }

    /**
     * Write our config values to the config
     */
    public void writeConfig() {
        this.clearConfig();

        for ( Field field : this.getConfigFields()) {
            ConfigValue configValue = field.getAnnotation(ConfigValue.class);
            try {
                Object value = field.get(this);
                config.set(configValue.path(), value);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.error("[Storage] Error invoking " + field, ex);
            }
        }
        this.saveConfig(true);
    }

    /**
     * Save the YAMLConfig (Bukkit API)
     *
     * @param comments {@link Boolean} should we save comments too?
     */
    private void saveConfig(boolean comments) {
        try {
            config.save(file);
            if (comments && !this.comments.isEmpty()) {
                ConfigSaver.writeWithComments(file, config, this.comments);
            }
        } catch (Exception e) {
            LOGGER.error("[Storage] Unable to save " + name + ".yml!");
        }
    }

    /**
     * Clear the config of any values or paths
     */
    private void clearConfig() {
        this.config.getKeys(false).forEach(key -> config.set(key, null));
        this.saveConfig(false);
    }

    /**
     * Comments that are not by config values but added
     * in paths that are separate.
     */
    public abstract void addSeparateComments();

    /**
     * Returns the configuration fields with {@link ConfigValue}
     * annotation on them.
     *
     * @return {@link List}
     */
    public abstract List<Field> getConfigFields();

    /**
     * The header for this configuration file
     *
     * @return {@link String[]} header
     */
    public abstract String[] getHeader();

    public String getString(String path) {
        return this.config.contains(path) ? ChatColor.translateAlternateColorCodes('&', this.config.getString(path)) : null;
    }

    public boolean contains(String path) {
        return this.config.contains(path);
    }

    public String getStringOrDefault(String path, String or) {
        String toReturn = this.getString(path);
        if (toReturn == null) {
            config.set(path, or);
            return or;
        } else {
            return toReturn;
        }
    }

    public int getInteger(String path) {
        return this.config.contains(path) ? this.config.getInt(path) : 0;
    }

    public int getInteger(String path, int or) {
        int toReturn = this.getInteger(path);
        return this.config.contains(path) ? or : toReturn;
    }

    public void set(String path, Object value) {
        this.config.set(path, value);
    }

    public boolean getBoolean(String path) {
        return this.config.contains(path) && this.config.getBoolean(path);
    }

    public double getDouble(String path) {
        return this.config.contains(path) ? this.config.getDouble(path) : 0.0D;
    }

    public void addComment(String path, String comment) {
        this.comments.put(path, comment);
    }

    public Object get(String path) {
        return this.config.contains(path) ? this.config.get(path) : null;
    }

    public List<String> getStringList(String path) {
        return this.config.contains(path) ? this.config.getStringList(path) : null;
    }

    public ConfigurationSection getConfigurationSection(String path) {
        return this.config.getConfigurationSection(path);
    }

    public ConfigurationSection createSection(String path) {
        return this.config.createSection(path);
    }

    public YamlConfiguration getConfiguration() {
        return this.config;
    }
}
