package xyz.refinedev.api.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import xyz.refinedev.api.IStorageProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

public class MongoStorageProvider<K, V> implements IStorageProvider<K, V> {

    protected final Map<K, V> map = new ConcurrentHashMap<>();
    private final MongoCollection<Document> collection;
    private final Gson gson;

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
    public CompletableFuture<List<V>> getAllEntries() {
        return CompletableFuture.supplyAsync(() -> {
            List<V> found = new ArrayList<>();
            for (Document document : this.collection.find()) {
                if (document == null) {
                    continue;
                }
                found.add(this.gson.fromJson(document.toJson(), new TypeToken<V>() {
                }.getType()));
            }
            return found;
        });
    }

    @Override
    public V getValueFromDataStore(K key) {
        Document document = this.collection.find(Filters.eq("_id", String.valueOf(key))).first();

        if (document == null) {
            return null;
        }

        V value = this.gson.fromJson(document.toJson(), new TypeToken<V>() {
        }.getType());

        if (value == null) {
            return null;
        }

        this.map.put(key, value);

        return value;
    }

    @Override
    public CompletableFuture<V> getValueFromDataStoreAsync(K key) {
        return CompletableFuture.supplyAsync(() -> this.getValueFromDataStore(key));
    }

    @Override
    public void saveData(K key, V value) {
        ForkJoinPool.commonPool()
                .execute(() -> this.collection.replaceOne(Filters.eq("_id", String.valueOf(key)), Document.parse(gson.toJson(value)), new UpdateOptions().upsert(true)));
    }
}
