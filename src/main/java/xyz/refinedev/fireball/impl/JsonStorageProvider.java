package xyz.refinedev.fireball.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import xyz.refinedev.fireball.IStorageProvider;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

public class JsonStorageProvider<K, V> implements IStorageProvider<K, V> {

    private final File file;

    private final Gson gson;

    protected final Map<K, V> map = new ConcurrentHashMap<>();

    public JsonStorageProvider(String name, String directory) {

        this.file = new File(directory, name + ".json");
        this.gson = new Gson();

        if (!this.file.exists()) {
            try {
                this.file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public V getValueFromCache(K key) {
        return this.map.getOrDefault(key, null);
    }

    @Override
    public CompletableFuture<List<V>> getAllEntries() {
        return CompletableFuture.supplyAsync(() -> {
            try (FileReader fileReader = new FileReader(this.file)) {
                List<V> found = this.gson.fromJson(fileReader, new TypeToken<List<V>>(){}.getType());

                if (found == null || found.isEmpty()) {
                    return Collections.emptyList();
                }

                return found;
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public V getValueFromDataStore(K key) {
        try (FileReader reader = new FileReader(this.file)) {
            JsonArray jsonArray = this.gson.fromJson(reader, JsonArray.class);
            for (JsonElement jsonElement : jsonArray) {
                if (jsonElement == null || !jsonElement.isJsonObject()) {
                    continue;
                }

                JsonObject object = jsonElement.getAsJsonObject();

                if (object.get("_id").getAsString().equalsIgnoreCase(String.valueOf(key))) {
                    V val = gson.fromJson(object, new TypeToken<V>() {
                    }.getType());
                    this.map.put(key, val);
                    return val;
                }
            }
            return null;
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public CompletableFuture<V> getValueFromDataStoreAsync(K key) {
        return CompletableFuture.supplyAsync(() -> getValueFromDataStore(key));
    }

    @Override
    public void saveData(K key, V value) {
        ForkJoinPool.commonPool().execute(() -> {
            JsonArray array = new JsonArray();

            for (V val : this.map.values()) {
                array.add(gson.toJson(val));
            }

            array.add(this.gson.toJson(value));

            try (FileWriter fileWriter = new FileWriter(this.file)) {
                gson.toJson(array, fileWriter);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void saveData(V value) {
        this.saveData(null, value);
    }
}
