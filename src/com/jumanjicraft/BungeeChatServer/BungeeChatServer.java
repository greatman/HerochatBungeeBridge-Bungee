package com.jumanjicraft.BungeeChatServer;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import net.craftminecraft.bungee.bungeeyaml.pluginapi.ConfigurablePlugin;
import net.md_5.bungee.api.ProxyServer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class BungeeChatServer extends ConfigurablePlugin {
    private boolean whitelist;
    private List<String> channels = new ArrayList<String>();
    private final String CHANNEL_NAME_SEND = "BungeeChatSend", CHANNEL_NAME_RECEIVE = "BungeeChatReceive";
    private MongoClient client;
    private MongoMessage queue;
    private Announcer announcer;

    public void onEnable() {
        saveDefaultConfig();
        try {
            client = new MongoClient(getConfig().getString("mongoAddress"));
            queue = new MongoMessage(client.getDB("messages").getCollection("herochatmessages"), getConfig().getInt("serverID"));
            announcer = new Announcer();
            ProxyServer.getInstance().getScheduler().runAsync(this, announcer);
            this.whitelist = getConfig().getBoolean("whitelist");
            this.channels = getConfig().getStringList("channels");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            getLogger().severe("Unable to connect to MongoDB! Plugin will be crippled in features!");
        }

    }

    public void onDisable() {
        queue.stop();
        announcer.poison();
    }

    public boolean shouldBroadcast(String channel) {
        if (whitelist) {
            return this.channels.contains(channel);
        } else {
            return !this.channels.contains(channel);
        }
    }

    private class Announcer implements Runnable {

        private boolean end = false;

        @Override
        public void run() {
            while (!end) {
                BasicDBObject message = queue.get();
                if (message != null) {
                    queue.ack(message);
                    if (message.containsField(CHANNEL_NAME_SEND)) {
                        String[] messages = ((String)message.get(CHANNEL_NAME_SEND)).split(":", 5);
                        String server = messages[0];
                        if (getConfig().getStringList("handleServer").contains(server)) {
                            String channelName = messages[1];
                            String rank = messages[2];
                            String nickname = messages[3];
                            String playerMessage = messages[4];
                            if (shouldBroadcast(channelName)) {
                                queue.send(CHANNEL_NAME_RECEIVE, server + ":" + channelName + ":" + rank + ":" + nickname + ":" + playerMessage);
                            }
                        }
                    }
                }
            }
        }

        public void poison() {
            end = true;
        }
    }
}