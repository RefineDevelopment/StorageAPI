package xyz.refinedev.storage;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IStorageProvider<K,V> {

    /**
     * Get a value directly from pre-loaded cache
     *
     * @param key {@link K} the key for the query
     * @return {@link V}
     */
    V getValueFromCache(K key);

    /**
     * Get a value directly by fetching it from db
     *
     * @param key {@link K} the key for the query
     * @return {@link V}
     */
    V getValueFromDataStore(K key);

    /**
     * Get a value directly by fetching it from db, async
     *
     * @param key {@link K} the key for the query
     * @return {@link CompletableFuture<V>}
     */
    CompletableFuture<V> getValueFromDataStoreAsync(K key);

    /**
     * Get all values directly by fetching it from db, async
     *
     * @return {@link CompletableFuture<List>}
     */
    CompletableFuture<List<V>> fetchAllEntries();

    /**
     * Get all values directly by fetching it from db, async
     *
     * @return {@link List<V>}
     */
    List<V> getAllCached();
    
    void saveData(K key, V value);

    /**
     * Deletes the designated query from the db
     *
     * @param key
     */
    void deleteData(K key);

    /**
     * Set your own GSON
     *
     * @param gson {@link Gson}
     */
    void setGSON(Gson gson);

    /**
     * Get the Cache Map
     *
     * @return {@link Map}
     */
    Map<K, V> getCache();

    /**
     * Custom load method
     */
    default void load() {}

    /**
     * Custom save method
     */
    default void save() {}

}
