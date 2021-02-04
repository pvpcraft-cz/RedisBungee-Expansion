package com.helpchat.redisbungeeexpansion;

import lombok.Getter;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class RedisBungeeExpansion extends PlaceholderExpansion implements Configurable, Cacheable, Taskable {

    private static final String VERSION = "3.1.2";

    @Getter
    private final RedisManager redisManager = new RedisManager(this);

    public RedisBungeeExpansion() {
    }

    @Override
    public boolean register() {
        redisManager.setup();
        return super.register();
    }

    @Override
    public String getIdentifier() {
        return "newredisbungee";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "qwz";
    }

    @Override
    public @NotNull String getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> getDefaults() {
        final Map<String, Object> defaults = new HashMap<>();

        defaults.put("check-interval", 30);
        defaults.put("tracked-servers", Arrays.asList("lobby", "survival"));

        // Redis settings
        defaults.put("redis.host", "localhost");
        defaults.put("redis.port", 6379);
        defaults.put("redis.user", "user");
        defaults.put("redis.pass", "password");
        return defaults;
    }

    /*
     * %identifier_count/status_server%
     * */
    @Override
    public String onPlaceholderRequest(Player player, String params) {

        String[] args = params.split("_");

        if (args.length < 2) {
            return "not-enough-args";
        }

        String server = args[1];

        ServerInfo info = redisManager.getCache().get(server);

        if (info == null)
            return "invalid-server";

        if (args[0].equalsIgnoreCase("status")) {
            return info.isOnline() ? "yes" : "no";
        }

        if (args[0].equalsIgnoreCase("count")) {
            if (server.equalsIgnoreCase("total") || server.equalsIgnoreCase("all")) {
                return String.valueOf(redisManager.getTotal());
            } else
                return String.valueOf(info.getPlayerCount());
        }
        return "invalid-params";
    }

    @Override
    public void clear() {
        redisManager.getCache().clear();

        if (isRegistered())
            redisManager.unregister();
    }

    @Override
    public void start() {
        redisManager.startUpdate();
    }

    @Override
    public void stop() {
        redisManager.stopUpdate();
    }
}
