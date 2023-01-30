package xyz.refinedev.api.storage.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.refinedev.api.storage.YamlStorage;
import xyz.refinedev.api.storage.annotations.Comment;
import xyz.refinedev.api.storage.annotations.ConfigValue;
import xyz.refinedev.api.storage.annotations.Create;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * This Project is property of Refine Development © 2021 - 2023
 * Redistribution of this Project is not allowed
 *
 * @author Drizzy
 * Created: 1/27/2023
 * Project: StorageAPI
 */

@SuppressWarnings("unused")
public class ClassBasedYamlStorage extends YamlStorage {

    private static final Logger LOGGER = LogManager.getLogger(ClassBasedYamlStorage.class);

    /**
     * Initiation method for a config file
     *
     * @param plugin       {@link JavaPlugin plugin instance}
     * @param name         {@link String config file name}
     * @param saveResource {@link Boolean should we save our built-in config}
     */
    public ClassBasedYamlStorage(JavaPlugin plugin, String name, boolean saveResource) {
        super(plugin, name, saveResource);
    }

    /**
     * Comments that are not by config values but added
     * in paths that are separate.
     */
    public void addSeparateComments() {
    }

    /**
     * Read the config values from file and assign them to our classes' fields
     */
    public void readConfig() {
        this.readValueForField(this.getClass(), null);
    }

    /**
     * Write the field values to the config and save our config
     */
    public void writeConfig() {
        this.clearConfig();
        this.readFieldClass(this.getClass(), null);
        this.saveConfig();
    }

    /**
     * This method basically reads a field's value (This field can be inside a config object)
     * from the config file and assigns its value accordingly
     *
     * @param clazz      {@link Class clazz}
     * @param parentPath {@link String path}
     */
    public void readValueForField(Class<?> clazz, String parentPath) {
        for ( Field field : clazz.getFields()) {

            String path = this.toNode(field);
            if (parentPath != null && parentPath.length() > 0) {
                path = parentPath + "." + path;
            }

            // It's a custom object class, not a simple field then
            if (field.getType().isAnnotationPresent(Create.class)) {
                Class<?> fieldClass = field.getType();
                int modifiers = fieldClass.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                    try {
                        field.set(this, fieldClass.newInstance());
                    } catch (IllegalArgumentException | IllegalAccessException | InstantiationException ex) {
                        LOGGER.error("[Storage] Error invoking " + field + " with parent key " + path, ex);
                    }
                    this.readValueForField(fieldClass, path);
                } else {
                    LOGGER.error("[Storage] The field " + fieldClass.getSimpleName() + " is not static or public, can not convert to config!");
                }
            } else {
                try {
                    field.set(this, this.get(path));
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    LOGGER.error("[Storage] Error invoking " + field + " with parent key " + path, ex);
                }
            }
        }
    }

    /**
     * This method basically writes a field's value (This field can be inside a config object)
     * to the config file and assigns its value accordingly
     *
     * @param clazz      {@link Class clazz}
     * @param path {@link String path}
     */
    private void readFieldClass(Class<?> clazz, String path) {
        for ( Field field : clazz.getFields() ) {
            String fieldPath = this.toNode(field);

            if (path != null && path.length() > 0) {
                fieldPath = path + "." + fieldPath;
            }

            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                for ( String string : comment.value() ) {
                    this.addComment(fieldPath, string);
                }
            }

            // It's a custom object class, not a simple field then
            if (field.getType().isAnnotationPresent(Create.class)) {
                Class<?> fieldClass = field.getType();
                int modifiers = fieldClass.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                    try {
                        field.set(this, fieldClass.newInstance());
                    } catch (IllegalArgumentException | IllegalAccessException | InstantiationException ex) {
                        LOGGER.error("[Storage] Error invoking " + field + " with parent key " + path, ex);
                    }
                    this.readFieldClass(fieldClass, fieldPath);
                } else {
                    LOGGER.error("[Storage] The field " + fieldClass.getSimpleName() + " is not static or public, can not convert to config!");
                }
            } else {
                try  {
                    Object value = field.get(this);
                    this.set(fieldPath, value);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    LOGGER.error("[Storage] Error invoking " + field, ex);
                }
            }
        }
    }

    /**
     * Returns the configuration fields with {@link ConfigValue}
     * annotation on them.
     *
     * @return {@link List}
     */
    public List<Field> getConfigFields() {
        throw new UnsupportedOperationException("This is not supported in class based configs!");
    }

    /**
     * The header for this configuration file
     *
     * @return {@link String[]} header
     */
    public String[] getHeader() {
        return new String[]{"This configuration file is part of a Refine Development Project. Purchased at https://dsc.gg/refine"};
    }

    private String toNode(Field field) {
        Class<?> type = field.getType();
        return type.getSimpleName().replaceAll("_", "-");
    }

//    private String toFieldName(String name) {
//        return name.replaceAll("_", "-");
//    }
}
