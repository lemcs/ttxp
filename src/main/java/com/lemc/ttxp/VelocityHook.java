package com.lemc.ttxp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VelocityHook {

    private static final String VELOCITY_CHANNEL = "BungeeCord";
    private static Plugin plugin;

    public static void setup(Plugin pluginInstance) {
        plugin = pluginInstance;
        if (!Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, VELOCITY_CHANNEL)) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, VELOCITY_CHANNEL);
        }
    }

    public static void connectToHub(Player player) {
        if (plugin == null) {
            player.sendMessage(ChatColor.RED + "插件未正确初始化，无法连接到目标服务器！");
            return;
        }

        String targetServer = "lobby";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        try {
            dataOutputStream.writeUTF("Connect");
            dataOutputStream.writeUTF(targetServer);
        } catch (IOException ex) {
            player.sendMessage(ChatColor.RED + "发送跨服消息时发生错误！");
            return;
        }

        player.sendPluginMessage(plugin, VELOCITY_CHANNEL, byteArrayOutputStream.toByteArray());
    }
}
