package xyz.refinedev.storage.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import xyz.refinedev.storage.IStorageProvider;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

public class JsonStorageProvider<K, V> implements IStorageProvider<K, V> {

    private final Map<K, V> map = new ConcurrentHashMap<>();
    private final File file;
    private Gson gson;

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
    public List<V> getAllCached() {
        return new ArrayList<>(this.map.values());
    }

    @Override
    public CompletableFuture<List<V>> fetchAllEntries() {
        return CompletableFuture.supplyAsync(() -> {
            try (FileReader fileReader = new FileReader(this.file)) {
                Type typeToken = new TypeToken<V>() {}.getType();
                List<V> found = this.gson.fromJson(fileReader, typeToken);

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
                    Type typeToken = new TypeToken<V>() {}.getType();
                    V val = gson.fromJson(object, typeToken);
                    this.map.putIfAbsent(key, val);
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

            this.map.putIfAbsent(key, value); // They could already be present so be careful lol

            for (V val : this.map.values()) {
                array.add(gson.toJson(val));
            }

            try (FileWriter fileWriter = new FileWriter(this.file)) {
                gson.toJson(array, fileWriter);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });
    }

    @Override
    public void deleteData(K key) {
        ForkJoinPool.commonPool().execute(() -> this.map.remove(key));
        this.getCache().forEach(this::saveData);
    }


    @Override
    public void setGSON(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Map<K, V> getCache() {
        return this.map;
    }

    public File getFile() {
        return file;
    }
}
