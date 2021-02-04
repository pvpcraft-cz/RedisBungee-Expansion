package com.helpchat.redisbungeeexpansion.system;

import com.helpchat.redisbungeeexpansion.RedisBungeeExpansion;
import com.helpchat.redisbungeeexpansion.struct.ServerCache;
import com.helpchat.redisbungeeexpansion.struct.ServerInfo;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Random;

public class RedisManager {

    public static final String SERVER_KEY = "serverinfo:%s:online";

    private final int updateInterval;

    private final RedisBungeeExpansion expansion;

    private final ServerStatusListener serverStatusListener;

    @Getter
    private final ServerCache cache = new ServerCache();

    final JedisPoolConfig poolConfig = buildPoolConfig();

    @Getter
    private JedisPool jedisPool;

    private BukkitTask updateTask;

    public RedisManager(RedisBungeeExpansion expansion) {
        this.expansion = expansion;
        this.serverStatusListener = new ServerStatusListener(expansion);
        Random random = new Random();
        this.updateInterval = expansion.getInt("check-interval", 30) + random.nextInt(10); // generate a random refresh time
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    private void buildJedis() {
        this.jedisPool = new JedisPool(poolConfig,
                expansion.getString("redis.host", "localhost"),
                expansion.getInt("redis.port", 6379),
                expansion.getString("redis.user", "user"),
                expansion.getString("redis.pass", "password"));
    }

    public void unregister() {
        serverStatusListener.unregister();
    }

    public void setup() {

        // Populate cache
        List<String> servers = expansion.getStringList("tracked_servers");
        servers.forEach(name -> cache.add(new ServerInfo(name, true)));

        buildJedis();

        serverStatusListener.register();
    }

    public void startUpdate() {
        stopUpdate();

        this.updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(expansion.getPlaceholderAPI(), this::update, 20L, updateInterval);
    }

    private void update() {
        setUpdate();
        getUpdate();
    }

    public void stopUpdate() {
        if (updateTask == null)
            return;

        updateTask.cancel();
        this.updateTask = null;
    }

    // Total player count, ignore servers that are not tracked
    public int getTotal() {
        return cache.getValues().stream()
                .filter(info -> info.isTracked() && info.isOnline())
                .mapToInt(ServerInfo::getPlayerCount)
                .sum();
    }

    // Set updated data to redis
    private void setUpdate() {
        try (Jedis jedis = jedisPool.getResource()) {
            int online = expansion.getPlaceholderAPI().getServer().getOnlinePlayers().size();
            jedis.set(String.format(SERVER_KEY, expansion.getPlaceholderAPI().getServer().getName()), String.valueOf(online));
        }
    }

    // Get updates from redis
    private void getUpdate() {
        try (Jedis jedis = jedisPool.getResource()) {
            for (ServerInfo server : cache.getValues()) {
                // Send a request for a server status update.
                serverStatusListener.sendServerStatusRequest(server.getName());

                String value = jedis.get(String.format(SERVER_KEY, server.getName()));

                if (value == null)
                    continue;

                int online;
                try {
                    online = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    continue;
                }

                server.setPlayerCount(online);
            }
        }
    }
}
