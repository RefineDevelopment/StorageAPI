package xyz.refinedev.api.storage.data;

import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * <p>
 * This Project is property of Refine Development.<br>
 * Copyright © 2024, All Rights Reserved.<br>
 * Redistribution of this Project is not allowed.<br>
 * </p>
 *
 * @author Drizzy
 * @version StorageAPI
 * @since 2/18/2024
 */

public class PluginData {

    private final String pluginName;

    private PluginDescriptionFile descriptionFile;
    private File dataFolder;

    public PluginData(String pluginName) {
        this.pluginName = pluginName;
    }

    public void saveResource(String name, File dataFolder, boolean replace) {
        name = name.replace('\\', '/');

        InputStream in = getClass().getClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + name + "' cannot be found ");
        }

        File outFile = new File(dataFolder, name);
        int lastIndex = name.lastIndexOf(47);
        File outDir = new File(dataFolder, name.substring(0, Math.max(lastIndex, 0)));
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (outFile.exists() && !replace) {
               System.out.println("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            } else {
                OutputStream out = Files.newOutputStream(outFile.toPath());
                byte[] buf = new byte[1024];

                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                out.close();
                in.close();
            }
        } catch (IOException ex) {
            System.out.println("Could not save " + outFile.getName() + " to " + outFile);
            ex.printStackTrace();
        }
    }

    public File getDataFolder() {
        if (this.dataFolder == null) {
            this.dataFolder = new File("plugins/" + pluginName);
            if (!this.dataFolder.exists()) {
                this.dataFolder.mkdir();
            }
        }

        return dataFolder;
    }

    public PluginDescriptionFile getDescription() {
        if (this.descriptionFile != null) return this.descriptionFile;

        try {
            this.descriptionFile = new PluginDescriptionFile(getClass().getClassLoader().getResourceAsStream("plugin.yml"));
            return this.descriptionFile;
        } catch (Exception e) {
            return null;
        }
    }

}
