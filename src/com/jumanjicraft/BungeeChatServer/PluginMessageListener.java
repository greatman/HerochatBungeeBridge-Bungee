package com.jumanjicraft.BungeeChatServer;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.InetSocketAddress;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessageListener
  implements Listener
{
  private BungeeChatServer plugin;

  public PluginMessageListener(BungeeChatServer plugin)
  {
    this.plugin = plugin;
  }

  @EventHandler
  public void receievePluginMessage(PluginMessageEvent event) throws IOException {
    if (!event.getTag().equalsIgnoreCase("BungeeChat"))
    {
      return;
    }
    if (!this.plugin.shouldBroadcast(ByteStreams.newDataInput(event.getData()).readUTF()))
    {
      return;
    }
    InetSocketAddress addr = event.getSender().getAddress();
    for (ServerInfo server : this.plugin.getProxy().getServers().values())
    {
      if (!server.getAddress().equals(addr))
      {
        if (server.getPlayers().size() != 0)
        {
          server.sendData("BungeeChat", event.getData());
        }
      }
    }
  }
}