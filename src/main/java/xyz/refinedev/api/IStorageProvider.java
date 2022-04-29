package xyz.refinedev.api;

import java.util.List;
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
    CompletableFuture<List<V>> getAllEntries();
    
    void saveData(K key, V value);

    /**
     * Custom load method
     */
    default void load() {}

    /**
     * Custom save method
     */
    default void save() {}

    /**
     * Custom unload method
     */
    default void unload() {}
}
