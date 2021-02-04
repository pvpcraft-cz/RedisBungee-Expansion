package com.helpchat.redisbungeeexpansion.system;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.helpchat.redisbungeeexpansion.RedisBungeeExpansion;
import com.helpchat.redisbungeeexpansion.RedisUtil;
import com.helpchat.redisbungeeexpansion.struct.ServerInfo;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class ServerStatusListener implements PluginMessageListener {

    public final static String STATUS_CHANNEL = "BungeeCord";

    private final PlaceholderAPIPlugin plugin;

    private final RedisBungeeExpansion expansion;

    public ServerStatusListener(RedisBungeeExpansion expansion) {
        this.plugin = expansion.getPlaceholderAPI();
        this.expansion = expansion;
    }

    public void register() {
        RedisUtil.registerChannel(plugin, STATUS_CHANNEL, this);
    }

    public void unregister() {
        RedisUtil.unregisterChannel(plugin, ServerStatusListener.STATUS_CHANNEL);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void sendServerStatusRequest(String server) {

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        try {
            out.writeUTF("ServerStatus");
            out.writeUTF(server);
            Bukkit.getServer().sendPluginMessage(plugin, STATUS_CHANNEL, out.toByteArray());
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] message) {

        if (!channel.equals(STATUS_CHANNEL)) {
            return;
        }

        ByteArrayDataInput inputStream = ByteStreams.newDataInput(message);

        try {
            String subChannel = inputStream.readUTF();

            if (subChannel.equals("ServerStatus")) {
                String server = inputStream.readUTF();

                boolean state = inputStream.readBoolean();
                ServerInfo info = expansion.getRedisManager().getCache().get(server);

                if (info != null)
                    info.setOnline(state);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Could not receive message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
