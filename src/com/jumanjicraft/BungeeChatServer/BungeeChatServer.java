package com.jumanjicraft.BungeeChatServer;

import java.util.ArrayList;
import java.util.List;

import net.craftminecraft.bungee.bungeeyaml.pluginapi.ConfigurablePlugin;

public class BungeeChatServer extends ConfigurablePlugin
{
  private boolean whitelist;
  private List<String> channels = new ArrayList<String>();

  public void onEnable()
  {
    saveDefaultConfig();
    getProxy().registerChannel("BungeeChat");
    getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));
    this.whitelist = getConfig().getBoolean("whitelist");
    this.channels = getConfig().getStringList("channels");
  }

  public void onDisable()
  {
    getProxy().unregisterChannel("BungeeChat");
  }

  public boolean shouldBroadcast(String channel)
  {
    return this.whitelist ^ this.channels.contains(channel);
  }
}