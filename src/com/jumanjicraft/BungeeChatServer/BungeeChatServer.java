package com.jumanjicraft.BungeeChatServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.craftminecraft.bungee.bungeeyaml.pluginapi.ConfigurablePlugin;
import net.md_5.bungee.api.config.ServerInfo;

public class BungeeChatServer extends ConfigurablePlugin
{
  private boolean whitelist;
  private List<String> channels = new ArrayList<String>();
  protected Map<InetSocketAddress, String> servers = new HashMap<InetSocketAddress, String>();

  public void onEnable()
  {
	for (ServerInfo server : getProxy().getServers().values())
	{
		servers.put(server.getAddress(), server.getName().substring(0, 1).toUpperCase());
	}
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