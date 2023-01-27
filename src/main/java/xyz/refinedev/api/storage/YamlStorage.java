package xyz.refinedev.api.storage;

import org.apache.commons.io.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.simpleyaml.configuration.file.YamlFile;

import xyz.refinedev.api.storage.annotations.ConfigValue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

/**
 * This Project is property of Refine Development © 2021 - 2022
 * Redistribution of this Project is not allowed
 *
 * @author Drizzy
 * Created: 7/27/2022
 * Project: StorageAPI
 */

@SuppressWarnings("unused")
public abstract class YamlStorage {

    private static final Logger LOGGER = LogManager.getLogger(YamlStorage.class);

    /*========================================================================*/
    private final String name;
    private final YamlFile config;
    /*========================================================================*/

    /**
     * Initiation method for a config file
     *
     * @param plugin       {@link JavaPlugin plugin instance}
     * @param name         {@link String config file name}
     * @param saveResource {@link Boolean should we save our built-in config}
     */
    public YamlStorage(JavaPlugin plugin, String name, boolean saveResource) {
        File file = new File(plugin.getDataFolder(), name + ".yml");

        this.name = name;
        this.config = new YamlFile(file);
        this.config.options().charset(Charsets.UTF_8);

        if (!file.exists()) {
            try {
                if (saveResource) {
                    plugin.saveResource(name + ".yml", false);
                } else {
                    this.config.createNewFile(false);
                }
            } catch (IOException ex) {
                LOGGER.error("[Storage] Could not create/save " + name + ".yml!");
                LOGGER.error("[Storage] Error: " + ex.getMessage());
            }
        }

        try {
            if (saveResource) {
                this.config.options().useComments(true);
                this.config.load(file);
            } else {
                this.config.load();
            }
        } catch (IOException ex) {
            LOGGER.error("[Storage] Could not load " + name + ".yml, please correct your syntax errors!");
            LOGGER.error("[Storage] Error: " + ex.getMessage());
        }

        this.config.options().header(String.join("\n", this.getHeader()));
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

                // Load the field's value from config
                if (this.config.contains(configValue.path()) && this.config.get(configValue.path()) != null) {
                    field.set(this, config.get(configValue.path()));
                } else {
                    this.config.set(configValue.path(), value); // Add a default value from the field
                }

                // Don't go adding empty comments, they'll just create empty lines
                // between different keys, making config look awful
                if (configValue.comment().length() > 0) {
                    this.config.setComment(configValue.path(), configValue.comment());
                }

            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.error("[Storage] Error invoking " + field, ex);
            }
        }

        this.addSeparateComments();
        this.saveConfig();
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

        this.saveConfig();
    }

    /**
     * Save the YAMLConfig (Bukkit API)
     */
    public void saveConfig() {
        try {
            config.save();
        } catch (IOException e) {
            LOGGER.error("[Storage] Unable to save " + name + ".yml!");
        }
    }

    /**
     * Clear the config of any values or paths
     */
    public void clearConfig() {
        this.config.getKeys(false).forEach(key -> config.set(key, null));
        this.saveConfig();
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
        this.config.setComment(path, comment);
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
