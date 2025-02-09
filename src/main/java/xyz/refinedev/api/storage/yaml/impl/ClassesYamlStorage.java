package xyz.refinedev.api.storage.yaml.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import org.simpleyaml.configuration.ConfigurationSection;

import xyz.refinedev.api.storage.yaml.YamlStorage;
import xyz.refinedev.api.storage.annotations.Comment;
import xyz.refinedev.api.storage.annotations.Create;
import xyz.refinedev.api.storage.annotations.Ignore;
import xyz.refinedev.api.storage.utils.ReflectionUtils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * <p>
 * This class is the property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this class without permission is not allowed.<br>
 * </p>
 * <p>
 *     This configuration is based on the same system Phoenix or FAWE use.
 *     Where classes represent configuration sections with their fields representing the values.
 *     We can use {@link Create} to mark classes as configuration sections both in parent and child classes.
 *     The classes and fields must be public whilst the fields must be non-final and non-static.
 * </p>
 *
 * @author Drizzy
 * @version StorageAPI
 * @since 12/8/2024
 */
public abstract class ClassesYamlStorage extends YamlStorage {

    private static final Logger LOGGER = LogManager.getLogger(ClassesYamlStorage.class);

    /**
     * Initiation method for a config file
     *
     * @param plugin       {@link JavaPlugin plugin instance}
     * @param name         {@link String config file name}
     * @param saveResource {@link Boolean should we save our built-in config}
     */
    public ClassesYamlStorage(JavaPlugin plugin, String name, boolean saveResource) {
        super(plugin, name, saveResource);
    }

    /**
     * Initiation method for a config file
     *
     * @param name         {@link String config file name}
     * @param dataFolder   {@link String data folder}
     */
    public ClassesYamlStorage(String name, String dataFolder) {
        super(name, dataFolder);
    }

    public void reloadConfig() {
        this.loadConfig();

        // Set initial configuration from the loaded YAML data
        this.setSectionValue(this.config.getConfigurationSection(this.config.getCurrentPath()), "");

        this.clearConfig();
        this.saveConfig();
    }

    public void setup() {
        this.setupConfigOptions(this.config.options());

        try {
            this.setupInstances("", this.getClass(),this);
        } catch (Exception e) {
            LOGGER.error("[Storage] Failed to setup instances.", e);
        }

        this.loadConfig();

        // Set initial configuration from the loaded YAML data
        this.setSectionValue(this.config.getConfigurationSection(this.config.getCurrentPath()), "");

        this.clearConfig();
        this.saveConfig();
    }

    /**
     * Comments that are not by config values but added
     * in paths that are separate.
     */
    public void addSeparateComments() {
        // Nothing as  default
    }

    /**
     * Saves the current configuration state of the object to the YAML structure and writes it to file.
     */
    public void saveConfig() {
        try {
            // Save all configurations recursively from the root of this class
            save("", this.getClass(), this);
            config.save();  // Save the YAML data to the file
        } catch (Exception e) {
            LOGGER.error("[Storage] Unable to save {}.yml!", name, e);
        }
    }

    /**
     * Recursively initializes instances of fields annotated with @Create in the given class.
     *
     * @param path     The current path to the field, used for hierarchical structure.
     * @param clazz    The class containing fields to initialize.
     * @param instance The instance of the class to which the fields belong.
     * @throws Exception If there is an issue accessing or instantiating fields.
     */
    public void setupInstances(String path, Class<?> clazz, Object instance) throws Exception {
        for (Field field : clazz.getFields()) {
            // Skip fields annotated with @Ignore
            if (field.getAnnotation(Ignore.class) != null) continue;

            String prefix = (!path.isEmpty() ? "." : "");

            // Initialize fields annotated with @Create
            if (field.isAnnotationPresent(Create.class)) {
                Class<?> current = field.getType();
                ReflectionUtils.setAccessible(field);

                // Retrieve or create an instance for the field if it is null
                Object value = field.get(instance);
                if (value == null) {
                    value = current.getConstructor().newInstance();
                    field.set(instance, value);
                }

                // Recursively initialize nested instances
                setupInstances(path + prefix + ReflectionUtils.toNodeName(current.getSimpleName()), current, value);
            }
        }
    }

    /**
     * Recursively saves field values to a YAML structure, handling comments and nested objects.
     *
     * @param path     The current path to the field for YAML hierarchy.
     * @param clazz    The class containing fields to save.
     * @param instance The instance of the class whose fields are saved.
     * @throws Exception If there is an issue accessing or setting field values.
     */
    public void save(String path, Class<?> clazz, Object instance) throws Exception {
        for (Field field : clazz.getFields()) {
            // Skip fields annotated with @Ignore
            if (field.getAnnotation(Ignore.class) != null) continue;

            String prefix = (!path.isEmpty() ? "." : "");
            String currentPath = path + prefix + ReflectionUtils.toNodeName(field.getName());

            // If the field is not annotated with @Create, save it directly
            if (!field.isAnnotationPresent(Create.class)) {
                // Handle comments if the field has a @Comment annotation
                if (field.isAnnotationPresent(Comment.class)) {
                    Comment comment = field.getAnnotation(Comment.class);
                    this.addComment(currentPath, comment.value(), comment.lineBreak());
                }
                // Set field value in the YAML structure
                this.config.set(currentPath, field.get(instance));
            } else {
                // For fields annotated with @Create, save nested objects recursively
                Class<?> current = field.getType();
                if (current.isAnnotationPresent(Comment.class)) {
                    Comment comment = current.getAnnotation(Comment.class);
                    this.addComment(currentPath, comment.value(), comment.lineBreak());
                }

                ReflectionUtils.setAccessible(field);
                Object value = field.get(instance);

                // Instantiate the object if it is null
                if (value == null) {
                    value = current.getConstructor().newInstance();
                    field.set(instance, value);
                }

                // Recursively save nested object fields
                save(path + prefix + ReflectionUtils.toNodeName(field.getName()), current, value);
            }
        }
    }

    /**
     * Recursively sets configuration values from a ConfigurationSection by traversing its keys.
     *
     * @param yml     The ConfigurationSection containing key-value pairs.
     * @param oldPath The existing path for nested sections, used to build the full key path.
     */
    private void setSectionValue(ConfigurationSection yml, String oldPath) {
        for (String key : yml.getKeys(false)) {
            Object value = yml.get(key);
            String newPath = oldPath + (oldPath.isEmpty() ? "" : ".") + key;

            // Recursively process nested sections
            if (value instanceof ConfigurationSection) {
                setSectionValue((ConfigurationSection) value, newPath.toUpperCase());
                continue;
            }

            // Set configuration value with an uppercase key
            setKeyValue(newPath.toUpperCase(), value);
        }
    }

    /**
     * Sets a configuration value for a specified key in the object instance.
     * If the key path leads to a nested field, it retrieves or initializes the nested instance first.
     *
     * @param key   The path to the field, with each level separated by a period.
     * @param value The value to set for the specified field.
     */
    private void setKeyValue(String key, Object value) {
        String[] split = key.split("\\.");  // Split key path for nested field access
        Object instance = getInstance(split, getClass());

        // Check if the nested instance was found
        if (instance == null) {
            return;
        }

        Field field = ReflectionUtils.getField(split, instance);

        // Check if the field was found in the instance
        if (field == null) {
            return;
        }

        try {
            // Convert value to string if the field type is String and the value isn't already a string
            if (field.getType() == String.class && !(value instanceof String)) {
                value = value.toString();
            }
            field.set(instance, value);  // Set the field with the provided value
        } catch (IllegalAccessException | IllegalArgumentException e) {
            LOGGER.error("Failed to set value for {}: {}", key, e);
        }
    }

    /**
     * Retrieves an instance of a nested field within the current object, based on a specified path.
     * The path is an array of strings where each entry represents a nested class or field name.
     * If a field is not yet initialized, this method attempts to instantiate it.
     *
     * @param split Array of strings representing the path to the field, where each entry is a nested class or field name.
     * @param root  The root class to start searching from. If null, the search begins from the calling class.
     * @return The instance of the specified field if found; otherwise, returns null.
     */
    private Object getInstance(String[] split, Class<?> root) {
        try {
            // Determine the starting class. Use root if provided, otherwise use the calling class.
            Class<?> clazz = (root == null) ? MethodHandles.lookup().lookupClass() : root;
            Object instance = this;  // Start with the current object as the instance

            // Iterate over the path array
            while (split.length > 0) {
                // If only one element remains, return the current instance (end of path)
                if (split.length == 1) {
                    return instance;
                }

                String[] finalSplit = split;

                // If the current class is null, find the inner class containing the desired field
                if (clazz == null) {
                    for ( Class<?> current : root.getDeclaredClasses() ) {
                        if (Arrays.stream(current.getDeclaredFields()).anyMatch(f -> f.getName().equalsIgnoreCase(ReflectionUtils.toFieldName(finalSplit[0])))) {
                            clazz = current;
                            break;
                        }
                    }
                    if (clazz == null) {
                        return null;
                    }
                }

                // Find the nested class matching the current path element
                Class<?> found = Arrays.stream(clazz.getDeclaredClasses())
                        .filter(current -> current.getSimpleName().equalsIgnoreCase(ReflectionUtils.toFieldName(finalSplit[0])))
                        .findFirst()
                        .orElse(null);

                // Attempt to access the field in the current class
                try {
                    Field instanceField = clazz.getDeclaredField(ReflectionUtils.toFieldName(split[0]));
                    ReflectionUtils.setAccessible(instanceField);

                    // Get the field's value from the current instance
                    Object value = instanceField.get(instance);

                    // If the field is null and a nested class is found, instantiate it
                    if (value == null && found != null) {
                        value = found.getDeclaredConstructor().newInstance();
                        instanceField.set(instance, value);
                    }

                    // Move deeper into the nested structure
                    clazz = found;
                    instance = value;
                    split = Arrays.copyOfRange(split, 1, split.length);
                } catch (NoSuchFieldException e) {
                    // If field doesn't exist, return null
                    return null;
                }
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.error("[Storage] Invalid Instance. {}", e.getMessage(), e);
        }
        return null;
    }
}
