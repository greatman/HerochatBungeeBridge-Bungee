package com.jumanjicraft.BungeeChatServer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import net.craftminecraft.bungee.bungeeyaml.pluginapi.ConfigurablePlugin;
import net.md_5.bungee.api.ProxyServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BungeeChatServer extends ConfigurablePlugin {
    private boolean whitelist;
    private List<String> channels = new ArrayList<String>();
    private final String CHANNEL_NAME_SEND = "BungeeChatSend", CHANNEL_NAME_RECEIVE = "BungeeChatReceive";
    private Announcer announcer;
    private Connection connection;
    private Channel channelSend;
    private Channel channelReceive;
    private QueueingConsumer consumer;

    public void onEnable() {
        saveDefaultConfig();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(getConfig().getString("amqpServer"));
        try {
            connection = factory.newConnection();
            channelSend = connection.createChannel();
            channelSend.exchangeDeclare(CHANNEL_NAME_RECEIVE, "fanout");
            channelReceive = factory.newConnection().createChannel();
            channelReceive.exchangeDeclare(CHANNEL_NAME_SEND, "fanout");
            String queueName = channelReceive.queueDeclare().getQueue();
            channelReceive.queueBind(queueName, CHANNEL_NAME_SEND, "");
            consumer = new QueueingConsumer(channelReceive);
            channelReceive.basicConsume(queueName, true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        announcer = new Announcer();
        ProxyServer.getInstance().getScheduler().runAsync(this, announcer);
        this.whitelist = getConfig().getBoolean("whitelist");
        this.channels = getConfig().getStringList("channels");

    }

    public void onDisable() {
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                try {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    String message = new String(delivery.getBody());
                    String[] messages = message.split(":", 5);
                    String server = messages[0];
                    if (getConfig().getStringList("handleServer").contains(server)) {
                        String channelName = messages[1];
                        String rank = messages[2];
                        String nickname = messages[3];
                        String playerMessage = messages[4];
                        if (shouldBroadcast(channelName)) {
                            try {
                                channelSend.basicPublish(CHANNEL_NAME_SEND, "", null, (server + ":" + channelName + ":" + rank + ":" + nickname + ":" + playerMessage).getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void poison() {
            end = true;
        }
    }
}