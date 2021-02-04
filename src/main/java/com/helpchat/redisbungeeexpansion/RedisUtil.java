package com.helpchat.redisbungeeexpansion;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

@UtilityClass
public class RedisUtil {

    public void registerChannel(JavaPlugin plugin, String channel, PluginMessageListener listener) {
        if (Bukkit.getMessenger().isIncomingChannelRegistered(plugin, channel)) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel);
        }

        if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, channel)) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
        }

        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
        PluginMessageListenerRegistration registration = Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, listener);

        Bukkit.getLogger().info("Registered channel " + channel + " valid: " + registration.isValid() + " listener: " + registration.getListener().getClass().getSimpleName());
    }

    public void unregisterChannel(JavaPlugin plugin, String channel) {
        if (Bukkit.getMessenger().isIncomingChannelRegistered(plugin, channel)) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, channel);
        }

        if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, channel)) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
        }
        Bukkit.getLogger().info("Unregistered channel " + channel);
    }
}
