package xyz.refinedev.api.storage.impl;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.refinedev.api.storage.YamlStorage;
import xyz.refinedev.api.storage.annotations.ConfigValue;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * This Project is property of Refine Development © 2021 - 2022
 * Redistribution of this Project is not allowed
 *
 * @author Drizzy
 * Created: 7/27/2022
 * Project: StorageAPI

 * This class is basically what default bukkit config is.
 * It saves the pre-defined file from your plugin and uses that.
 * No pre-defined config fields or comments.
 *
 * This can be used for language config, menu config etc.
 */
public class BasicYamlStorage extends YamlStorage {

    /**
     * Initiation method for a config file
     *
     * @param plugin       {@link JavaPlugin plugin instance}
     * @param name         {@link String config file name}
     * @param saveResource {@link Boolean save the resource from plugin}
     */
    public BasicYamlStorage(JavaPlugin plugin, String name, boolean saveResource) {
        super(plugin, name, saveResource);
    }

    /**
     * Comments that are not by config values but added
     * in paths that are separate.
     */
    public void addSeparateComments() {
        // Nothing
    }

    /**
     * Returns the configuration fields with {@link ConfigValue}
     * annotation on them.
     *
     * @return {@link List}
     */
    public List<Field> getConfigFields() {
        return Collections.emptyList(); // No pre-defined stuff
    }

    /**
     * The header for this configuration file
     *
     * @return {@link String[]} header
     */
    public String[] getHeader() {
        return new String[]{"This configuration file is part of a Refine Development Project. Purchased at https://dsc.gg/refine"};
    }
}