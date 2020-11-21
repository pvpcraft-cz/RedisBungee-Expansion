package com.helpchat.redisbungeeexpansion;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RedisBungeeExpansion extends PlaceholderExpansion implements PluginMessageListener, Taskable, Cacheable, Configurable {

    private static final String VERSION = "3.0.0";

    private static final int FETCH_INTERVAL = 60;

    private final Map<String, ServerInfo> serverInfo = new ConcurrentHashMap<>();

    private int total = 0;

    private BukkitTask task;

    private final static String REDIS_CHANNEL = "legacy:redisbungee";
    private final static String STATUS_CHANNEL = "me:serverstatus";

    public RedisBungeeExpansion() {
        registerChannel(REDIS_CHANNEL);
        registerChannel(STATUS_CHANNEL);
    }

    private void registerChannel(String channel) {
        if (Bukkit.getMessenger().isIncomingChannelRegistered(getPlaceholderAPI(), channel)) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(getPlaceholderAPI(), channel);
        }

        if (Bukkit.getMessenger().isOutgoingChannelRegistered(getPlaceholderAPI(), channel)) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(getPlaceholderAPI(), channel);
        }

        Bukkit.getMessenger().registerOutgoingPluginChannel(getPlaceholderAPI(), channel);
        Bukkit.getMessenger().registerIncomingPluginChannel(getPlaceholderAPI(), channel, this);
        Bukkit.getLogger().info("Registered channel " + channel);
    }

    @Override
    public boolean register() {
        List<String> servers = getStringList("tracked_servers");
        servers.forEach(server -> this.serverInfo.put(server, new ServerInfo(server)));
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
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> getDefaults() {
        final Map<String, Object> defaults = new HashMap<>();
        defaults.put("check_interval", 30);
        defaults.put("tracked_servers", Arrays.asList("lobby", "survival", "skyblock", "pit"));
        return defaults;
    }

    private ServerInfo getServer(String server) {
        return this.serverInfo.containsKey(server) ? this.serverInfo.get(server) : createServer(server);
    }

    private ServerInfo createServer(String server) {
        ServerInfo serverInfo = new ServerInfo(server);
        this.serverInfo.put(server, serverInfo);
        return serverInfo;
    }

    private void sendPlayerCountRequest(String server) {

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        try {
            out.writeUTF("PlayerCount");
            out.writeUTF(server);
            Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(getPlaceholderAPI(), REDIS_CHANNEL, out.toByteArray());
        } catch (Exception ignored) {
        }
    }

    private void sendServerStatusRequest(String server) {

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        try {
            out.writeUTF("ServerStatus");
            out.writeUTF(server);
            Bukkit.getServer().sendPluginMessage(getPlaceholderAPI(), STATUS_CHANNEL, out.toByteArray());
        } catch (Exception ignored) {
        }
    }

    /*
     * %identifier_count/status_server%
     * */

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        String[] args = params.split("_");

        if (args.length < 2) {
            return "not_enough_args";
        }

        String server = args[1];

        if (args[0].equalsIgnoreCase("status")) {
            return getServer(server).isOnline() ? "yes" : "no";
        } else if (args[0].equalsIgnoreCase("count")) {
            if (server.equalsIgnoreCase("total") || server.equalsIgnoreCase("all")) {
                return String.valueOf(total);
            } else
                return String.valueOf(getServer(server).getPlayerCount());
        }
        return "invalid_server";
    }

    private void sendUpdateRequests() {
        sendPlayerCountRequest("ALL");

        for (String server : serverInfo.keySet()) {
            sendPlayerCountRequest(server);
            sendServerStatusRequest(server);
        }
    }

    @Override
    public void start() {
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                sendUpdateRequests();
            }
        }.runTaskTimer(getPlaceholderAPI(), 100L, 20L * FETCH_INTERVAL);
    }

    @Override
    public void stop() {
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
            task = null;
        }
    }

    @Override
    public void clear() {
        serverInfo.clear();
        if (isRegistered()) {
            unregisterChannel(REDIS_CHANNEL);
            unregisterChannel(STATUS_CHANNEL);
        }
    }

    private void unregisterChannel(String channel) {
        if (Bukkit.getMessenger().isIncomingChannelRegistered(getPlaceholderAPI(), channel)) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(getPlaceholderAPI(), channel);
        }

        if (Bukkit.getMessenger().isOutgoingChannelRegistered(getPlaceholderAPI(), channel)) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(getPlaceholderAPI(), channel);
        }
        Bukkit.getLogger().info("Unregistered channel " + channel);
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] message) {

        if (!channel.equals(REDIS_CHANNEL) && !channel.equals(STATUS_CHANNEL)) {
            return;
        }

        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(message));

        try {
            String subChannel = inputStream.readUTF();

            if (subChannel.equals("PlayerCount")) {
                String server = inputStream.readUTF();

                if (inputStream.available() > 0) {
                    int count = inputStream.readInt();

                    if (server.equals("ALL")) {
                        total = count;
                    } else {
                        getServer(server).setPlayerCount(count);
                    }
                }
            } else if (subChannel.equals("GetServers")) {
                String[] serverList = inputStream.readUTF().split(", ");

                if (serverList.length == 0) {
                    return;
                }

                for (String server : serverList) {
                    if (!serverInfo.containsKey(server))
                        createServer(server);
                }
            } else if (subChannel.equals("ServerStatus")) {
                String server = inputStream.readUTF();

                if (inputStream.available() > 0) {
                    boolean state = inputStream.readBoolean();
                    getServer(server).setOnline(state);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
