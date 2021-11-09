package xyz.refinedev.fireball.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import xyz.refinedev.fireball.IStorageProvider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RedisStorageProvider<K,V> implements IStorageProvider<K,V> {

    protected final Map<K,V> map = new ConcurrentHashMap<>();

    private final String password;

    private final JedisPool jedisPool;

    private final Gson gson = new Gson();

    public RedisStorageProvider(String host, int port, String password) {
        this.password = password;
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
            return gson.fromJson(jedis.get(String.valueOf(key)), new TypeToken<V>(){}.getType());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public CompletableFuture<V> getValueFromDataStoreAsync(K key) {
        return CompletableFuture.supplyAsync(() -> this.getValueFromDataStore(key));
    }

    @Override
    public void saveData(K key, V value) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!password.isEmpty()) {
                jedis.auth(password);
            }

            jedis.set(String.valueOf(key), gson.toJson(value));
        }
    }
}
