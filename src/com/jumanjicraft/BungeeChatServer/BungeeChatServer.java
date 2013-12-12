package com.jumanjicraft.BungeeChatServer;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.Jedis;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.JedisPool;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.JedisPoolConfig;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.JedisPubSub;
import com.imaginarycode.minecraft.redisbungee.internal.jedis.exceptions.JedisException;
import net.craftminecraft.bungee.bungeeyaml.pluginapi.ConfigurablePlugin;
import net.md_5.bungee.api.ProxyServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BungeeChatServer extends ConfigurablePlugin {
    private boolean whitelist;
    private List<String> channels = new ArrayList<String>();
    private JedisPool pool;
    private final String CHANNEL_NAME_SEND = "BungeeChatSend", CHANNEL_NAME_RECEIVE = "BungeeChatReceive";
    private PubSubListener psl;

    public void onEnable() {
        saveDefaultConfig();
        pool = new JedisPool(new JedisPoolConfig(), getConfig().getString("jedisAddress"));
        if (pool == null) {
            getLogger().severe("Unable to connect to Jedis! Plugin will be crippled in features!");
        }
        psl = new PubSubListener();
        getProxy().getScheduler().runAsync(this, psl);
        this.whitelist = getConfig().getBoolean("whitelist");
        this.channels = getConfig().getStringList("channels");
    }

    public void onDisable() {
        psl.poison();
    }

    public boolean shouldBroadcast(String channel) {
        if (whitelist) {
            return this.channels.contains(channel);
        } else {
            return !this.channels.contains(channel);
        }
    }

    public JedisPool getPool() {
        return pool;
    }

    private class PubSubListener implements Runnable {

        private Jedis rsc;
        private JedisPubSubHandler jpsh;

        @Override
        public void run() {
            try {
                rsc = pool.getResource();
                jpsh = new JedisPubSubHandler();
                rsc.subscribe(jpsh, CHANNEL_NAME_SEND);
            } catch (JedisException ignored) {
            }
        }

        public void poison() {
            jpsh.unsubscribe();
            pool.returnResource(rsc);
        }
    }

    private class JedisPubSubHandler extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            System.out.println("IS IS SEND");
            if (channel.equals(CHANNEL_NAME_SEND)) {
                System.out.println("SPLITTING");
                String[] messages = message.split(":", 4);
                System.out.println("THE ARRAY:" + Arrays.toString(messages));
                String channelName = messages[0];
                String rank = messages[1];
                String nickname = messages[2];
                String playerMessage = messages[3];
                System.out.println("SHOULD I BROADCAST?");
                if (shouldBroadcast(channelName)) {
                    System.out.println("BROADCASTING...");
                    Jedis rsc = pool.getResource();
                    rsc.publish(CHANNEL_NAME_RECEIVE, channelName + ":" + rank + ":" + nickname + ":" + playerMessage);
                    pool.returnResource(rsc);
                    System.out.println("FINISH BROADCAST");
                }
            }
        }

        @Override
        public void onPMessage(String s, String s2, String s3) {
        }

        @Override
        public void onSubscribe(String s, int i) {
        }

        @Override
        public void onUnsubscribe(String s, int i) {
        }

        @Override
        public void onPUnsubscribe(String s, int i) {
        }

        @Override
        public void onPSubscribe(String s, int i) {
        }
    }
}