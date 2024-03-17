package xyz.refinedev.api.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * This Project is property of Refine Development © 2021 - 2022
 * Redistribution of this Project is not allowed
 *
 * @author Drizzy
 * Created: 7/27/2022
 * Project: StorageAPI
 */

@SuppressWarnings("unused")
public class MongoStorage<V> {

    private static final ReplaceOptions REPLACE_OPTIONS = new ReplaceOptions().upsert(true);

    private final MongoCollection<Document> collection;
    private final Gson gson;

    public MongoStorage(MongoCollection<Document> collection, Gson gson) {
        this.collection = collection;
        this.gson = gson;
    }

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

    public CompletableFuture<List<Document>> fetchAllRawEntries() {
        return CompletableFuture.supplyAsync(() -> {
            List<Document> found = new ArrayList<>();
            for (Document document : this.collection.find()) {
                found.add(document);
            }
            return found;
        });
    }

    public void saveData(UUID key, V value, Type type) {
        CompletableFuture.runAsync(() -> this.saveDataSync(key, value, type));
    }

    public void saveDataSync(UUID key, V value, Type type) {
        Bson query = Filters.eq("_id", key.toString());
        Document parsed = Document.parse(gson.toJson(value, type));
        this.collection.replaceOne(query, parsed, REPLACE_OPTIONS);
    }

    public void saveRawData(UUID key, Document document) {
        CompletableFuture.runAsync(() -> this.saveRawDataSync(key, document));
    }

    public void saveRawDataSync(UUID key, Document document) {
        Bson query = Filters.eq("_id", key.toString());
        this.collection.replaceOne(query, document, REPLACE_OPTIONS);
    }

    public V loadData(UUID key, Type type) {
        Bson query = Filters.eq("_id", key.toString());

        Document document = this.collection.find(query).first();
        if (document == null) return null;

        return this.gson.fromJson(document.toJson(), type);
    }

    public CompletableFuture<V> loadDataAsync(UUID key, Type type) {
        return CompletableFuture.supplyAsync(() -> this.loadData(key, type));
    }

    public Document loadRawData(UUID key) {
        Bson query = Filters.eq("_id", key.toString());
        return this.collection.find(query).first();
    }

    public CompletableFuture<Document> loadRawDataAsync(UUID key) {
        return CompletableFuture.supplyAsync(() -> this.loadRawData(key));
    }

    public void deleteData(UUID key) {
        CompletableFuture.runAsync(() -> {
            Bson query = Filters.eq("_id", key.toString());
            this.collection.deleteOne(query);
        });
    }

    /**
     * Delete a certain key in all documents inside the collection
     * Uses long because it could surpass the limit of integer
     *
     * @param key {@link String key}
     * @return    {@link Integer amount of deleted documents}
     */
    public CompletableFuture<Integer> deleteKeyInAll(String key) {
        return CompletableFuture.supplyAsync(() -> {
            int deleteCount = 0;
            for ( Document document : collection.find() ) {
                if (document == null) continue;

                if (document.get(key) != null) {
                    document.remove(key);
                    deleteCount++;
                }
                collection.replaceOne(Filters.eq("_id", document.get("_id")), document);
            }
            return deleteCount;
        });
    }
}
