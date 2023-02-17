package xyz.refinedev.api.storage.impl;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.refinedev.api.storage.YamlStorage;
import xyz.refinedev.api.storage.annotations.ConfigValue;

import java.lang.reflect.Field;
import java.util.*;

public abstract class ParentYamlStorage extends YamlStorage {

    /**
     * Set based cache for child storages of this Parent Storage
     * We keep it concurrent to allow asynchronous file I/O
     */
    private final Set<ChildYamlStorage> childStorages = Collections.synchronizedSet(new HashSet<>());

    /**
     * Initiation method for a config file
     *
     * @param plugin       {@link JavaPlugin plugin instance}
     * @param name         {@link String config file name}
     */
    public ParentYamlStorage(JavaPlugin plugin, String name) {
        super(plugin, name, false);
    }

    /**
     * Link a {@link ChildYamlStorage} to this parent storage
     *
     * @param storage {@link ChildYamlStorage storage}
     */
    public void addChildStorage(ChildYamlStorage storage) {
        this.childStorages.add(storage);
    }

    /**
     * Returns the configuration fields with {@link ConfigValue}
     * annotation on them.
     *
     * @return {@link List}
     */
    public List<Field> getConfigFields() {
        List<Field> annotatedFields = new ArrayList<>(this.getParentFields());

        // Add child fields
        this.childStorages.stream().map(ChildYamlStorage::getConfigFields).forEach(annotatedFields::addAll);

        // Sort according to priority
        annotatedFields.sort(Comparator.comparingInt(field -> field.getAnnotation(ConfigValue.class).priority()));

        return annotatedFields;
    }

    /**
     * Returns the fields of this parent storage
     *
     * @return {@link List}
     */
    public abstract List<Field> getParentFields();

    /**
     * The header for this configuration file
     *
     * @return {@link String[]} header
     */
    public abstract String[] getHeader();

    /**
     * Comments that are not by config values but added
     * in paths that are separate.
     */
    public abstract void addSeparateComments();

    /**
     * We register all our child storages in this method
     * since it is called before reading the config
     */
    public abstract void registerChildStorages();
}