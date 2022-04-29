package xyz.refinedev.api.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import xyz.refinedev.api.IStorageProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RedisStorageProvider<K,V> implements IStorageProvider<K,V> {

    protected final Map<K,V> map = new ConcurrentHashMap<>();
    private Gson gson;
    private final JedisPool jedisPool;

    private final String password, keyPrefix;

    public RedisStorageProvider(String host, int port, String password, String keyPrefix) {
        this.password = password;
        this.keyPrefix = keyPrefix;
        this.gson = new Gson();
        this.jedisPool = new JedisPool(host, port);
    }

    @Override
    public V getValueFromCache(K key) {
        return this.map.getOrDefault(key, null);
    }

    @Override
    public V getValueFromDataStore(K key) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!password.isEmpty()) {
                jedis.auth(password);
            }
            return gson.fromJson(jedis.get(keyPrefix + "_" + key), new TypeToken<V>(){}.getType());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public CompletableFuture<List<V>> getAllEntries() {
        return CompletableFuture.supplyAsync(() -> {
            List<V> found = new ArrayList<>();
            try (Jedis jedis = this.jedisPool.getResource()) {
                if (!password.isEmpty()) jedis.auth(password);

                for (String key : jedis.keys(keyPrefix + "_*")) {
                    found.add(this.gson.fromJson(jedis.get(key), new TypeToken<V>(){}.getType()));
                }
            }
            return found;
        });
    }

    @Override
    public CompletableFuture<V> getValueFromDataStoreAsync(K key) {
        return CompletableFuture.supplyAsync(() -> this.getValueFromDataStore(key));
    }

    @Override
    public void saveData(K key, V value) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!password.isEmpty()) jedis.auth(password);

            jedis.set(keyPrefix + "_" + key, gson.toJson(value));
        }
    }

    @Override
    public void setGSON(Gson gson) {
        this.gson = gson;
    }
}
