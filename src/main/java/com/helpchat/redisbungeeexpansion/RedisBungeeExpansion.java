package com.helpchat.redisbungeeexpansion;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

    private final String CHANNEL = "legacy:redisbungee";

    public RedisBungeeExpansion() {
        if (Bukkit.getMessenger().isIncomingChannelRegistered(getPlaceholderAPI(), CHANNEL)) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(getPlaceholderAPI(), CHANNEL);
        }

        if (Bukkit.getMessenger().isOutgoingChannelRegistered(getPlaceholderAPI(), CHANNEL)) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(getPlaceholderAPI(), CHANNEL);
        }

        Bukkit.getMessenger().registerOutgoingPluginChannel(getPlaceholderAPI(), CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(getPlaceholderAPI(), CHANNEL, this);
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

    private void sendPlayerCountRequest(String server) {

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        try {
            out.writeUTF("PlayerCount");
            out.writeUTF(server);
            Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(getPlaceholderAPI(), CHANNEL, out.toByteArray());
        } catch (Exception ignored) {
        }
    }

    private void sendServerStatusRequest(String server) {

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        try {
            out.writeUTF("ServerStatus");
            out.writeUTF(server);
            Bukkit.getServer().sendPluginMessage(getPlaceholderAPI(), CHANNEL, out.toByteArray());
        } catch (Exception ignored) {
        }
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {

        if (params.equalsIgnoreCase("total") || params.equalsIgnoreCase("all")) {
            return String.valueOf(total);
        }

        if (serverInfo.containsKey(params)) {
            return String.valueOf(serverInfo.get(params));
        }

        serverInfo.put(params, 0);
        return "invalid_server";
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        if (params.equalsIgnoreCase("total") || params.equalsIgnoreCase("all")) {
            return String.valueOf(total);
        }

        if (serverInfo.containsKey(params)) {
            return String.valueOf(serverInfo.get(params));
        }

        serverInfo.put(params, 0);
        return "invalid_server";
    }


    @Override
    public void start() {
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                sendPlayerCountRequest("ALL");

                for (String server : serverInfo.keySet()) {
                    sendPlayerCountRequest(server);
                    sendServerStatusRequest(server);
                }
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
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(getPlaceholderAPI(), CHANNEL);
            Bukkit.getMessenger().unregisterIncomingPluginChannel(getPlaceholderAPI(), CHANNEL, this);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] message) {

        if (!channel.equals(CHANNEL)) {
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
                        serverInfo.put(server, count);
                    }
                }
            } else if (subChannel.equals("GetServers")) {
                String[] serverList = inputStream.readUTF().split(", ");

                if (serverList.length == 0) {
                    return;
                }

                for (String server : serverList) {
                    if (!serverInfo.containsKey(server)) {
                        serverInfo.put(server, 0);
                    }
                }
            } else if (subChannel.equals("ServerStatus")) {
                String server = inputStream.readUTF();

                if (inputStream.available() > 0) {
                    boolean state = inputStream.readBoolean();
                    serverStatuses.put(server, state);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
