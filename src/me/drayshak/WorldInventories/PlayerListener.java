package me.drayshak.WorldInventories;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener
{

    private final WorldInventories plugin;
    
    PlayerListener(final WorldInventories plugin)
    {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        Player player = event.getEntity().getPlayer();
        
        if(WorldInventories.exempts.contains(player.getName().toLowerCase()))
        {
            WorldInventories.logDebug("Ignoring exempt player death: " + player.getName());
            return;
        }
        
        Group group = WorldInventories.findFirstGroupThenDefault(player.getWorld().getName());
        
        plugin.savePlayerInventory(player.getName(), group, new PlayerInventoryHelper(new ItemStack[36], new ItemStack[4]));
        if (plugin.getConfig().getBoolean("dostats"))
        {
            plugin.savePlayerStats(player.getName(), group, new PlayerStats(20, 20, 0, 0, 0, 0F, null));
        }   

        if (plugin.getConfig().getBoolean("donotifications"))
        {
            player.sendMessage(ChatColor.GREEN + "You died! Wiped inventory and stats for group: " + group.getName());
        }     
    }
    
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
    {
        Player player = event.getPlayer();
        
        String fromworld = event.getFrom().getName();
        String toworld = player.getLocation().getWorld().getName();
        
        if(WorldInventories.exempts.contains(player.getName().toLowerCase()))
        {
            WorldInventories.logDebug("Ignoring exempt player world switch: " + player.getName());
            return;
        }           
        
        if (!fromworld.equals(toworld))
        {
            WorldInventories.logDebug("Player " + player.getName() + " moved from world " + fromworld + " to " + toworld);            
            
            Group fromgroup = WorldInventories.findFirstGroupThenDefault(fromworld);
            Group togroup = WorldInventories.findFirstGroupThenDefault(toworld);
            
            plugin.savePlayerInventory(player.getName(), fromgroup, plugin.getPlayerInventory(player));
            if (plugin.getConfig().getBoolean("dostats"))
            {
                plugin.savePlayerStats(player, fromgroup);
            }    
            
            if (!fromgroup.getName().equals(togroup.getName()))
            {
                plugin.setPlayerInventory(player, plugin.loadPlayerInventory(player, togroup));
                if (plugin.getConfig().getBoolean("dostats"))
                {
                    plugin.setPlayerStats(player, plugin.loadPlayerStats(player, togroup));
                }
                
                if(plugin.getConfig().getBoolean("dogamemodeswitch"))
                {
                    player.setGameMode(togroup.getGameMode());
                }                
                
                if (plugin.getConfig().getBoolean("donotifications"))
                {
                    player.sendMessage(ChatColor.GREEN + "Changed player information to match group: " + togroup.getName());
                }
            }
            else
            {
                if (plugin.getConfig().getBoolean("donotifications"))
                {
                    player.sendMessage(ChatColor.GREEN + "No player information change needed to match group: " + togroup.getName());
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        
        String world = player.getLocation().getWorld().getName();
        
        WorldInventories.logDebug("Player " + player.getName() + " quit from world: " + world);
        
        if(WorldInventories.exempts.contains(player.getName().toLowerCase()))
        {
            WorldInventories.logDebug("Ignoring exempt player logout: " + player.getName());
            return;
        }           
        
        Group tGroup = WorldInventories.findFirstGroupThenDefault(world);

        // Don't save if we don't care where we are (default group)
        //if (tGroup != null)
        //{            
            WorldInventories.logDebug("Saving inventory of " + player.getName());
            plugin.savePlayerInventory(player.getName(), tGroup, plugin.getPlayerInventory(player));
            
            if (plugin.getConfig().getBoolean("dostats"))
            {
                plugin.savePlayerStats(player, tGroup);
            }
        //}
        
        // Save the Ender Chest contents
        if(player.getOpenInventory().getType() == InventoryType.ENDER_CHEST)
        {
            plugin.savePlayerEnderChest(player.getName(), tGroup, new EnderChestHelper(player.getOpenInventory().getTopInventory().getContents()));
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (plugin.getConfig().getBoolean("loadinvonlogin"))
        {
            Player player = event.getPlayer();
            String world = player.getLocation().getWorld().getName();
            
            WorldInventories.logDebug("Player " + player.getName() + " join world: " + world);
            
            if(WorldInventories.exempts.contains(player.getName().toLowerCase()))
            {
                WorldInventories.logDebug("Ignoring exempt player join: " + player.getName());
                return;
            }            
            
            Group tGroup = WorldInventories.findFirstGroupThenDefault(world);
            
            //WorldInventories.logDebug("Loading inventory of " + player.getName());
            plugin.setPlayerInventory(player, plugin.loadPlayerInventory(player, tGroup));            
            
            if (plugin.getConfig().getBoolean("dostats"))
            {
                plugin.setPlayerStats(player, plugin.loadPlayerStats(player, tGroup));
            }
            
            if(plugin.getConfig().getBoolean("dogamemodeswitch"))
            {
                //WorldInventories.logDebug("Should change gamemode to " + tGroup.getGameMode().toString() + " for " + player.getName());
                event.getPlayer().setGameMode(tGroup.getGameMode());
            }              
            
            if(plugin.getConfig().getBoolean("donotifications"))
            {
                player.sendMessage(ChatColor.GREEN + "Player information loaded for group: " + tGroup.getName());
            }
        }
    }
}
