package xyz.refinedev.storage.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import xyz.refinedev.storage.IStorageProvider;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

public class MongoStorageProvider<K, V> implements IStorageProvider<K, V> {

    private static final ReplaceOptions REPLACE_OPTIONS = new ReplaceOptions().upsert(true);

    private final Map<K, V> map = new ConcurrentHashMap<>();
    private final MongoCollection<Document> collection;
    private Gson gson;

    /**
     * Constructs a new {@link MongoStorageProvider} instance
     *
     * @param collection collection from the database
     */
    public MongoStorageProvider(MongoCollection<Document> collection) {
        this.collection = collection;
        this.gson = new Gson();
    }

    @Override
    public V getValueFromCache(K key) {
        return map.getOrDefault(key, null);
    }

    @Override
    public CompletableFuture<List<V>> fetchAllEntries() {
        return CompletableFuture.supplyAsync(() -> {
            List<V> found = new ArrayList<>();
            for (Document document : this.collection.find()) {
                if (document == null) {
                    continue;
                }
                Type typeToken = new TypeToken<V>() {}.getType();
                found.add(this.gson.fromJson(document.toJson(), typeToken));
            }
            return found;
        });
    }

    @Override
    public List<V> getAllCached() {
        return new ArrayList<>(this.map.values());
    }

    @Override
    public V getValueFromDataStore(K key) {
        Document document = this.collection.find(Filters.eq("_id", String.valueOf(key))).first();
        if (document == null) return null;

        V value = this.gson.fromJson(document.toJson(), new TypeToken<V>() {}.getType());
        if (value == null) return null;

        this.map.putIfAbsent(key, value);

        return value;
    }

    @Override
    public CompletableFuture<V> getValueFromDataStoreAsync(K key) {
        return CompletableFuture.supplyAsync(() -> this.getValueFromDataStore(key));
    }

    @Override
    public void saveData(K key, V value) {
        ForkJoinPool.commonPool().execute(() -> {
            Bson query = Filters.eq("_id", String.valueOf(key));
            Document parsed = Document.parse(gson.toJson(value));
            this.map.putIfAbsent(key, value);
            this.collection.replaceOne(query, parsed, REPLACE_OPTIONS);
        });
    }

    @Override
    public void deleteData(K key) {
        ForkJoinPool.commonPool().execute(() -> {
            this.map.remove(key);
            Bson query = Filters.eq("_id", String.valueOf(key));
            this.collection.deleteOne(query);
        });
    }

    @Override
    public void setGSON(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Map<K, V> getCache() {
        return this.map;
    }
}
