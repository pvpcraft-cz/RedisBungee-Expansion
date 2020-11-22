package com.helpchat.redisbungeeexpansion;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.jetbrains.annotations.NotNull;

public class ServerStatusListener implements PluginMessageListener {

    private final static String STATUS_CHANNEL = "BungeeCord";

    private final PlaceholderAPIPlugin plugin;

    private final RedisBungeeExpansion expansion;

    public ServerStatusListener(RedisBungeeExpansion expansion) {
        this.plugin = expansion.getPlaceholderAPI();
        this.expansion = expansion;

        registerChannel(STATUS_CHANNEL);
    }

    public void sendServerStatusRequest(String server) {

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        try {
            out.writeUTF("ServerStatus");
            out.writeUTF(server);
            Bukkit.getServer().sendPluginMessage(plugin, STATUS_CHANNEL, out.toByteArray());
            Bukkit.getLogger().info("Sent status request for " + server);
        } catch (Exception ignored) {
        }
    }

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
                Bukkit.getLogger().info("Received status message for " + server + " state: " + state);
                expansion.getServer(server).setOnline(state);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Could not receive message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void registerChannel(String channel) {
        if (Bukkit.getMessenger().isIncomingChannelRegistered(plugin, channel)) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel);
        }

        if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, channel)) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
        }

        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
        PluginMessageListenerRegistration registration = Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);

        Bukkit.getLogger().info("Registered channel " + channel + " valid: " + registration.isValid() + " listener: " + registration.getListener().getClass().getSimpleName());
    }

    public void unregisterChannel() {
        if (Bukkit.getMessenger().isIncomingChannelRegistered(plugin, STATUS_CHANNEL)) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, STATUS_CHANNEL);
        }

        if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, STATUS_CHANNEL)) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, STATUS_CHANNEL);
        }
        Bukkit.getLogger().info("Unregistered channel " + STATUS_CHANNEL);
    }
}
