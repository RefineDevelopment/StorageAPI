package xyz.refinedev.api.storage.yaml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.comments.format.YamlCommentFormat;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.simpleyaml.configuration.file.YamlConfigurationOptions;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.configuration.implementation.api.QuoteStyle;
import org.simpleyaml.configuration.implementation.snakeyaml.SnakeYamlImplementation;

import xyz.refinedev.api.storage.annotations.Header;

import java.io.File;
import java.io.IOException;
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

    protected final String name;
    protected final YamlFile config;

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

        this.setup();
    }

    /**
     * Initiation method for a config file
     *
     * @param name         {@link String config file name}
     * @param folder       {@link String data folder}
     */
    public YamlStorage(String name, String folder) {
        File file = new File(folder, name + ".yml");

        this.name = name;
        this.config = new YamlFile(file);

        if (!file.exists()) {
            try {
                this.config.createNewFile(false);
            } catch (IOException ex) {
                LOGGER.error("[Storage] Could not create/save " + name + ".yml!");
                LOGGER.error("[Storage] Error: " + ex.getMessage());
            }
        }

        this.setup();
    }

    public void setup() {
        this.setupConfigOptions(this.config.options());
        this.loadConfig();
    }

    public void loadConfig() {
        try {
            this.config.loadWithComments();
        } catch (IOException ex) {
            LOGGER.error("[Storage] Could not load " + name + ".yml, please correct your syntax errors!");
            LOGGER.error("[Storage] Error: " + ex.getMessage());
        }
    }

    /**
     * Reload this config
     */
    public void reloadConfig() {
        try {
            this.config.loadWithComments();
        } catch (IOException ex) {
            LOGGER.error("[Storage] Could not load " + name + ".yml, please correct your syntax errors!");
            LOGGER.error("[Storage] Error: " + ex.getMessage());
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
     * Here you can set config options on your own
     *
     * @param options {@link YamlConfigurationOptions options}
     */
    public void setupConfigOptions(YamlConfigurationOptions options) {
        this.config.setCommentFormat(YamlCommentFormat.PRETTY);

        options.charset(com.google.common.base.Charsets.UTF_8);
        options.useComments(true);
        options.quoteStyleDefaults().setQuoteStyle(String.class, QuoteStyle.DOUBLE);
        options.quoteStyleDefaults().setQuoteStyle(List.class, QuoteStyle.DOUBLE);

        String[] header = this.getHeader();
        if (header != null) {
            options.header(String.join("\n", header));
        }

        SnakeYamlImplementation implementation = (SnakeYamlImplementation) options.configuration().getImplementation();
        implementation.getDumperOptions().setSplitLines(false);
    }

    /**
     * Comments that are not by config values but added
     * in paths that are separate.
     */
    public abstract void addSeparateComments();

    /**
     * The header for this configuration file
     *
     * @return {@link String[]} header
     */
    public String[] getHeader() {
        String[] header = null;
        if (this.getClass().isAnnotationPresent(Header.class)) {
            Header comment = this.getClass().getAnnotation(Header.class);
            header = comment.value();
        }
        return header;
    }

    public String getString(String path) {
        return this.config.contains(path) ? this.config.getString(path) : null;
    }

    public boolean contains(String path) {
        return this.config.contains(path);
    }

    public String getStringOrDefault(String path, String or) {
        return this.config.contains(path) ? this.config.getString(path) : or;
    }

    public int getInteger(String path) {
        return this.config.contains(path) ? this.config.getInt(path) : 0;
    }

    public int getInteger(String path, int or) {
        int toReturn = this.getInteger(path);
        return this.config.contains(path) ? toReturn : or;
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
        this.addComment(path, comment, false);
    }

    public void addCommentWithBlankLine(String path, String comment) {
        this.addComment(path, comment, true);
    }

    public void addComment(String path, String comment, boolean lineBreak) {
        this.addComment(path, new String[]{comment}, lineBreak);
    }

    public void addComment(String path, String[] comment, boolean lineBreak) {
        this.config.setComment(path, String.join("\n", comment));
        if (lineBreak) {
            this.config.setBlankLine(path);
        }
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
