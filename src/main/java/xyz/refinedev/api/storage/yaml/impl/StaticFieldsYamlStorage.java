package xyz.refinedev.api.storage.yaml.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.refinedev.api.storage.yaml.YamlStorage;
import xyz.refinedev.api.storage.annotations.ConfigValue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p>
 * This class is the property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this class without permission is not allowed.<br>
 * </p>
 *
 * @author Drizzy
 * @version StorageAPI
 * @since 12/8/2024
 */
public class StaticFieldsYamlStorage extends YamlStorage {

    private static final Logger LOGGER = LogManager.getLogger(StaticFieldsYamlStorage.class);

    private List<Field> fields;

    /**
     * Initiation method for a config file
     *
     * @param name   {@link String config file name}
     * @param folder {@link String data folder}
     */
    public StaticFieldsYamlStorage(String name, String folder) {
        super(name, folder);

        this.readConfig();
    }

    /**
     * Initiation method for a config file
     *
     * @param plugin       {@link JavaPlugin plugin instance}
     * @param name         {@link String config file name}
     * @param saveResource {@link Boolean should we save our built-in config}
     */
    public StaticFieldsYamlStorage(JavaPlugin plugin, String name, boolean saveResource) {
        super(plugin, name, saveResource);

        this.readConfig();
    }

    public void setup() {
        this.fields = this.getConfigFields();
        super.setup();
    }

    /**
     * Read config values from the config, if some are not present
     * we save the default value of the {@link ConfigValue} to the config
     */
    public void readConfig() {
        for ( Field field : this.fields ) {
            try {
                ConfigValue configValue = field.getAnnotation(ConfigValue.class);
                Object value = field.get(null);

                // Load the field's value from config
                if (this.config.contains(configValue.path())) {
                    field.setAccessible(true);
                    field.set(null, config.get(configValue.path()));
                    field.setAccessible(false);
                } else {
                    this.config.set(configValue.path(), value); // Add a default value from the field
                }

                // Don't go adding empty comments, they'll just create empty lines
                // between different keys, making config look awful
                if (!configValue.comment().isEmpty()) {
                    this.config.path(configValue.path()).comment(configValue.comment()).blankLine();
                }

            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.error("[Storage] Error invoking {}", field, ex);
            }
        }

        this.addSeparateComments();
        this.saveConfig();
    }

    /**
     * Write our config values to the config
     */
    public void writeConfig() {
        for ( Field field : this.fields) {
            ConfigValue configValue = field.getAnnotation(ConfigValue.class);
            try {
                Object value = field.get(null);
                config.set(configValue.path(), value);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.error("[Storage] Error invoking " + field, ex);
            }
        }

        this.saveConfig();
    }

    /**
     * Reload this config
     */
    public void reloadConfig() {
        try {
            this.config.loadWithComments();
        } catch (IOException ex) {
            LOGGER.error("[Storage] Could not load {}.yml, please correct your syntax errors!", name);
            LOGGER.error("[Storage] Error: {}", ex.getMessage());
        }

        this.readConfig();
    }

    /**
     * Comments that are not by config values but added
     * in paths that are separate.
     */
    public void addSeparateComments() {

    }

    /**
     * Returns the configuration fields with {@link ConfigValue}
     * annotation on them.
     *
     * @return {@link List}
     */
    public List<Field> getConfigFields() {
        List<Field> annotatedFields = new ArrayList<>();

        for ( Field field : this.getClass().getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isStatic(field.getModifiers())) continue;

            ConfigValue configValue = field.getAnnotation(ConfigValue.class);
            if (configValue == null) continue;

            annotatedFields.add(field);
        }

        annotatedFields.sort(Comparator.comparingInt(field -> field.getAnnotation(ConfigValue.class).priority()));

        return annotatedFields;
    }
}
