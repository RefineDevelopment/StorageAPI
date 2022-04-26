package xyz.refinedev.fireball;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IStorageProvider<K,V> {

    V getValueFromCache(K key);

    V getValueFromDataStore(K key);

    CompletableFuture<V> getValueFromDataStoreAsync(K key);

    CompletableFuture<List<V>> getAllEntries();

    void saveData(K key, V value);

    default void load() {

    }

    default void unload() {

    }
}
