package xyz.refinedev.api.storage.utils;

import com.google.common.base.Preconditions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This Project is property of Refine Development © 2021 - 2022
 * Redistribution of this Project is not allowed
 *
 * @author Drizzy
 * Created: 7/27/2022
 * Project: StorageAPI
 */

public class ConfigSaver {

    //Used for separating keys in the keyBuilder inside parseComments method
    private static final char SEPARATOR = '.';

    public static void writeWithComments(File file, YamlConfiguration config, Map<String, String> comments) {
        Preconditions.checkArgument(file.exists(), "The file doesn't exist!");
        try {
            // will write updated config file "contents" to a string
            StringWriter writer = new StringWriter();
            write(config, new BufferedWriter(writer), comments);
            String value = writer.toString(); // config contents

            Path path = file.toPath();
            Files.write(path, value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void write(YamlConfiguration currentConfig, BufferedWriter writer, Map<String, String> comments) throws IOException {
        //Used for converting objects to yaml, then cleared
        FileConfiguration parserConfig = new YamlConfiguration();
        writer.write("# " + currentConfig.options().header().replace("\n", "\n# ") + "\n");

        for ( String fullKey : currentConfig.getKeys(true) ) {
            String indents = KeyBuilder.getIndents(fullKey, SEPARATOR);
            writeCommentIfExists(comments, writer, fullKey, indents);

            Object currentValue = currentConfig.get(fullKey);
            if (currentValue == null) continue;

            String[] splitFullKey = fullKey.split("[" + SEPARATOR + "]");
            String trailingKey = splitFullKey[splitFullKey.length - 1];

            if (currentValue instanceof ConfigurationSection) {
                writer.write(indents + trailingKey + ":");

                if (!((ConfigurationSection) currentValue).getKeys(false).isEmpty())
                    writer.write("\n");
                else
                    writer.write(" {}\n");

                continue;
            }

            parserConfig.set(trailingKey, currentValue);
            String yaml = parserConfig.saveToString();
            yaml = yaml.substring(0, yaml.length() - 1).replace("\n", "\n" + indents);
            String toWrite = indents + yaml + "\n";
            parserConfig.set(trailingKey, null);
            writer.write(toWrite);
        }

        String danglingComments = comments.get(null);

        if (danglingComments != null)
            writer.write(danglingComments);

        writer.close();
    }

    private static void writeCommentIfExists(Map<String, String> comments, BufferedWriter writer, String fullKey, String indents) throws IOException {
        String comment = comments.get(fullKey);

        //Comments always end with new line (\n)
        if (comment != null)
            //Replaces all '\n' with '\n' + indents except for the last one
            writer.write(indents + comment.substring(0, comment.length() - 1).replace("\n", "\n" + indents) + "\n");
    }
}
