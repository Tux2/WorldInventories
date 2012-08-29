package me.drayshak.WorldInventories;

import com.thoughtworks.xstream.XStream;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldInventories extends JavaPlugin
{
    public static final Logger log = Logger.getLogger("Minecraft");
    public static PluginManager pluginManager = null;
    public static Server bukkitServer = null;
    public static ArrayList<Group> groups = null;
    public static Timer saveTimer = new Timer();
    public static String fileVersion = "v4";

    public PlayerInventoryHelper getPlayerInventory(Player player)
    {
        return new PlayerInventoryHelper(player.getInventory().getContents(), player.getInventory().getArmorContents());
    }

    public void setPlayerInventory(Player player, PlayerInventoryHelper playerInventory)
    {
        if (playerInventory != null)
        {
            player.getInventory().setContents(playerInventory.getItems());
            player.getInventory().setArmorContents(playerInventory.getArmour());
        }
    }

    public void setPlayerStats(Player player, PlayerStats playerstats)
    {
        // Never kill a player - must be a bug if it was 0
        player.setHealth(Math.max(playerstats.getHealth(), 1));
        player.setFoodLevel(playerstats.getFoodLevel());
        player.setExhaustion(playerstats.getExhaustion());
        player.setSaturation(playerstats.getSaturation());
        player.setLevel(playerstats.getLevel());
        player.setExp(playerstats.getExp());
    }

    public void savePlayers()
    {
        WorldInventories.logStandard("Saving player information...");

        for (Player player : WorldInventories.bukkitServer.getOnlinePlayers())
        {
            String world = player.getLocation().getWorld().getName();

            Group tGroup = WorldInventories.findFirstGroupForWorld(world);

            // Don't save if we don't care where we are (default group)
            if (tGroup != null)
            {
                savePlayerInventory(player.getName(), WorldInventories.findFirstGroupForWorld(world), getPlayerInventory(player));
                if (getConfig().getBoolean("dostats"))
                {
                    savePlayerStats(player, WorldInventories.findFirstGroupForWorld(world));
                }
            }
        }

        WorldInventories.logStandard("Done.");
    }

    public void savePlayerInventory(String player, Group group, PlayerInventoryHelper toStore)
    {
        if (!this.getDataFolder().exists())
        {
            this.getDataFolder().mkdir();
        }

        String path = File.separator;

        // Use default group
        if (group == null)
        {
            path += "default";
        }
        else
        {
            path += group.getName();
        }

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }

        path += File.separator + player + ".inventory." + fileVersion + ".xml";

        try
        {
            XStream xstream = new XStream();
            xstream.toXML(toStore.inventories, new FileOutputStream(path));
        }        
        catch (Exception e)
        {
            WorldInventories.logError("Failed to save inventory for player: " + player + ": " + e.getMessage());
        }
        
        WorldInventories.logDebug("Saved inventory for player: " + player + " " + path);
    }

   public void savePlayerEnderChest(String player, Group group, EnderChestHelper toStore)
    {
        if (!this.getDataFolder().exists())
        {
            this.getDataFolder().mkdir();
        }

        String path = File.separator;

        // Use default group
        if (group == null)
        {
            path += "default";
        }
        else
        {
            path += group.getName();
        }

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }

        path += File.separator + player + ".enderchest." + fileVersion + ".xml";

        try
        {
            XStream xstream = new XStream();
            xstream.toXML(toStore.inventories, new FileOutputStream(path));
        }        
        catch (Exception e)
        {
            WorldInventories.logError("Failed to save Ender Chest for player: " + player + ": " + e.getMessage());
        }
        
        WorldInventories.logDebug("Saved Ender Chest for player: " + player + " " + path);
    }    
    
    public EnderChestHelper loadPlayerEnderChest(String player, Group group)
    {
        InventoriesLists playerInventory = null;
        
        String path = File.separator;

        // Use default group
        if (group == null)
        {
            path += "default";
        }
        else
        {
            path += group.getName();
        }

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }

        path += File.separator + player + ".enderchest." + fileVersion + ".xml";

        try
        {
            XStream xstream = new XStream();
            playerInventory = (InventoriesLists) xstream.fromXML(new FileInputStream(path));
        }
        catch (FileNotFoundException e)
        {
            ItemStack[] items = new ItemStack[27];
            for (int i = 0; i < 27; i++)
            {
                items[i] = new ItemStack(Material.AIR);
            }
            
            return new EnderChestHelper(items);
        }
        catch (Exception e)
        {
            WorldInventories.logDebug("Failed to load Ender Chest for player: " + player + ", showing empty inventory: " + e.getMessage());
            ItemStack[] items = new ItemStack[27];
            for (int i = 0; i < 27; i++)
            {
                items[i] = new ItemStack(Material.AIR);
            }
            
            return new EnderChestHelper(items);            
        }

        WorldInventories.logDebug("Loaded Ender Chest for player: " + player + " " + path);
        
        return new EnderChestHelper(playerInventory);        
    }
    
    public PlayerInventoryHelper loadPlayerInventory(Player player, Group group)
    {
        InventoriesLists playerInventory = null;

        String path = File.separator;

        // Use default group
        if (group == null)
        {
            path += "default";
        }
        else
        {
            path += group.getName();
        }

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }

        path += File.separator + player.getName() + ".inventory." + fileVersion + ".xml";

        try
        {
            XStream xstream = new XStream();
            playerInventory = (InventoriesLists) xstream.fromXML(new FileInputStream(path));
        }
        catch (FileNotFoundException e)
        {
            WorldInventories.logDebug("Player " + player.getName() + " will get a new item file on next save (clearing now).");
            player.getInventory().clear();
            ItemStack[] armour = new ItemStack[4];
            for (int i = 0; i < 4; i++)
            {
                armour[i] = new ItemStack(Material.AIR);
            }

            player.getInventory().setArmorContents(armour);
            
            return new PlayerInventoryHelper(player.getInventory().getContents(), player.getInventory().getArmorContents());
        }
        catch (Exception e)
        {
            WorldInventories.logDebug("Failed to load inventory for player: " + player.getName() + ", giving empty inventory: " + e.getMessage());
        }
        
        WorldInventories.logDebug("Loaded inventory for player: " + player + " " + path);

        return new PlayerInventoryHelper(playerInventory);
    }

    public PlayerStats loadPlayerStats(Player player, Group group)
    {
        PlayerStats playerstats = null;

        String path = File.separator;

        // Use default group
        if (group == null)
        {
            path += "default";
        }
        else
        {
            path += group.getName();
        }

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }

        path += File.separator + player.getName() + ".stats.xml";

        try
        {
            XStream xstream = new XStream();
            playerstats = (PlayerStats) xstream.fromXML(new FileInputStream(path));
        }
        catch (FileNotFoundException e)
        {
            WorldInventories.logDebug("Player " + player.getName() + " will get a new stats file on next save (clearing now).");
            playerstats = new PlayerStats(20, 20, 0, 0, 0, 0F);
            this.setPlayerStats(player, playerstats);
        }
        catch (Exception e)
        {
            WorldInventories.logDebug("Failed to load stats for player: " + player.getName() + ", giving defaults: " + e.getMessage());
        }
        
        WorldInventories.logDebug("Loaded stats for player: " + player + " " + path);

        return playerstats;
    }

    public void savePlayerStats(String player, Group group, PlayerStats playerstats)
    {
        if (!this.getDataFolder().exists())
        {
            this.getDataFolder().mkdir();
        }

        String path = File.separator;

        // Use default group
        if (group == null)
        {
            path += "default";
        }
        else
        {
            path += group.getName();
        }

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }

        path += File.separator + player + ".stats." + fileVersion + ".xml";

        try
        {
            XStream xstream = new XStream();
            xstream.toXML(playerstats, new FileOutputStream(path));
        }    
        catch (Exception e)
        {
            WorldInventories.logError("Failed to save stats for player: " + player + ": " + e.getMessage());
        }
        
        WorldInventories.logDebug("Saved stats for player: " + player + " " + path);
    }

    public void savePlayerStats(Player player, Group group)
    {
        PlayerStats playerstats = new PlayerStats(player.getHealth(), player.getFoodLevel(), player.getExhaustion(), player.getSaturation(), player.getLevel(), player.getExp());
        
        if (!this.getDataFolder().exists())
        {
            this.getDataFolder().mkdir();
        }

        String path = File.separator;

        // Use default group
        if (group == null)
        {
            path += "default";
        }
        else
        {
            path += group.getName();
        }

        path = this.getDataFolder().getAbsolutePath() + path;

        File file = new File(path);
        if (!file.exists())
        {
            file.mkdir();
        }

        path += File.separator + player.getName() + ".stats." + fileVersion + ".xml";

        try
        {
            XStream xstream = new XStream();
            xstream.toXML(playerstats, new FileOutputStream(path));
        }    
        catch (Exception e)
        {
            WorldInventories.logError("Failed to save stats for player: " + player + ": " + e.getMessage());
        }
        
        WorldInventories.logDebug("Saved stats for player: " + player + " " + path);
    }

    public boolean importMultiInvData()
    {
        Plugin pMultiInv = WorldInventories.pluginManager.getPlugin("MultiInv");
        if (pMultiInv == null)
        {
            WorldInventories.logError("Failed to import MultiInv shares - Bukkit couldn't find MultiInv. Make sure it is installed and enabled whilst doing the import, then when successful remove it.");
        }

        File MISharesLocation = new File(pMultiInv.getDataFolder(), "Worlds" + File.separator);
        if (!MISharesLocation.exists())
        {
            WorldInventories.logError("Failed to import MultiInv shares - " + MISharesLocation.toString() + " doesn't seem to exist.");
            return false;
        }

        File fMIConfig = new File(WorldInventories.pluginManager.getPlugin("MultiInv").getDataFolder(), "shares.yml");
        if (!fMIConfig.exists())
        {
            WorldInventories.logError("Failed to import MultiInv shares - shares file doesn't seem to exist.");
            return false;
        }

        FileConfiguration MIConfig = YamlConfiguration.loadConfiguration(fMIConfig);

        for (String sGroup : MIConfig.getConfigurationSection("").getKeys(false))
        {
            List<String> sWorlds = MIConfig.getStringList(sGroup);
            if (sWorlds != null)
            {
                Group group = new Group(sGroup, sWorlds, false);
                WorldInventories.groups.add(group);
                getConfig().set("groups." + sGroup, sWorlds);
            }
            else
            {
                WorldInventories.logDebug("Skipping import of group because it is empty: " + sGroup);
            }
        }

        this.saveConfig();

        ArrayList<String> sMIShares = new ArrayList(Arrays.asList(MISharesLocation.list()));

        if (sMIShares.size() <= 0)
        {
            WorldInventories.logError("Failed to import MultiInv shares - there weren't any shares found!");
            return false;
        }
        else
        {
            for (int i = 0; i < sMIShares.size(); i++)
            {
                String sWorld = sMIShares.get(i);

                File fWorld = new File(MISharesLocation, sWorld);
                if (fWorld.isDirectory() && fWorld.exists())
                {
                    Group group = findFirstGroupForWorld(sWorld);
                    if (group == null)
                    {
                        group = new Group(sWorld, Arrays.asList(sWorld), false);
                        WorldInventories.groups.add(group);
                        getConfig().set("groups." + sWorld, Arrays.asList(sWorld));
                        this.saveConfig();

                        WorldInventories.logError("A world was found that doesn't belong to any groups! It was saved as its own group. To put it in a group, edit the WorldInventories config.yml: " + sWorld);
                    }

                    //List<String> sPlayer = Arrays.asList(fWorld.list());

                    for (File shareFile : fWorld.listFiles())
                    {
                        if (shareFile.getAbsolutePath().endsWith(".yml"))
                        {
                            String sFilename = shareFile.getName();
                            String playerName = sFilename.substring(0, sFilename.length() - 4);

                            Configuration playerConfig = YamlConfiguration.loadConfiguration(shareFile);

                            String sPlayerInventory = playerConfig.getString("survival");
                            PlayerInventoryHelper playerInventory = MultiInvImportHelper.playerInventoryFromMIString(sPlayerInventory);
                            if (playerInventory == null)
                            {
                                sPlayerInventory = playerConfig.getString("creative");
                            }
                            if (playerInventory == null)
                            {
                                logError("Failed to load MultiInv data - found player file but failed to convert it: " + playerName);
                            }
                            else
                            {
                                this.savePlayerInventory(playerName, group, playerInventory);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }
   
    public boolean import78Data()
    {
        boolean allImported = true;
        int groupsFound = 0;
        int inventoriesFound = 0;
        
        WorldInventories.logStandard("Starting pre 78 build inventory import...");
        
        for(File fGroup : this.getDataFolder().listFiles())
        {
            if(fGroup.isDirectory() && fGroup.exists())
            {
                groupsFound++;
                
                for(File fInventory : new File(this.getDataFolder(), fGroup.getName()).listFiles())
                {
                    if(fInventory.isFile())
                    {
                        boolean is78Inventory = fInventory.getName().endsWith(".inventory");
                        if(is78Inventory)
                        {
                            inventoriesFound++;
                            
                            WIPlayerInventory oldinventory = Import78Helper.load78PlayerInventory(fInventory);
                            if(oldinventory == null)
                            {
                                WorldInventories.logError("Failed to convert " + fInventory.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerInventory(fInventory.getName().split("\\.")[0], new Group(fGroup.getName(), null, false), new PlayerInventoryHelper(oldinventory.getItems(), oldinventory.getArmour()));
                            }
                        }
                    }
                }                
            }            
        }
        
        WorldInventories.logStandard("Attempted conversion of " + Integer.toString(groupsFound) + " groups and " + Integer.toString(inventoriesFound) + " associated inventories");
        
        return allImported;
    }    
    
    public boolean import141Data()
    {
        boolean allImported = true;
        int groupsFound = 0;
        int inventoriesFound = 0;
        int statsFound = 0;
        int enderChestsFound = 0;
        
        WorldInventories.logStandard("Starting pre 141 build inventory import...");
        
        for(File fGroup : this.getDataFolder().listFiles())
        {
            if(fGroup.isDirectory() && fGroup.exists())
            {
                groupsFound++;
                
                for(File fFile : new File(this.getDataFolder(), fGroup.getName()).listFiles())
                {
                    if(fFile.isFile())
                    {
                        boolean is141Inventory = fFile.getName().endsWith(".inventory.v3");
                        if(is141Inventory)
                        {
                            inventoriesFound++;
                            
                            PlayerInventoryHelper oldinventory = Import141Helper.load141PlayerInventory(fFile);
                            if(oldinventory == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerInventory(fFile.getName().split("\\.")[0], new Group(fGroup.getName(), null, false), oldinventory);
                            }
                        }
                        
                        boolean is141EnderChest = fFile.getName().endsWith(".enderchest.v3");
                        if(is141EnderChest)
                        {
                            enderChestsFound++;
                            
                            EnderChestHelper oldinventory = Import141Helper.load141EnderChest(fFile);
                            if(oldinventory == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerEnderChest(fFile.getName().split("\\.")[0], new Group(fGroup.getName(), null, false), oldinventory);
                            }
                        }
                        
                        boolean is141Stats = fFile.getName().endsWith(".stats");
                        if(is141Stats)
                        {
                            statsFound++;
                            
                            PlayerStats oldstats = Import141Helper.load141PlayerStats(fFile);
                            if(oldstats == null)
                            {
                                WorldInventories.logError("Failed to convert " + fFile.getName() + " in group " + fGroup.getName());
                                allImported = false;
                            }
                            else
                            {
                                savePlayerStats(fFile.getName().split("\\.")[0], new Group(fGroup.getName(), null, false), oldstats);
                            }
                        }                        
                    }
                }                
            }            
        }
        
        WorldInventories.logStandard("Attempted conversion of " + Integer.toString(groupsFound) + " groups including: " + Integer.toString(inventoriesFound) + " inventories, " + Integer.toString(enderChestsFound) + " Ender Chests and " + Integer.toString(statsFound) + " player stats.");
        
        return allImported;
    }

    // NetBeans complains about these log lines but message formatting breaks for me
    public static void logStandard(String line)
    {
        log.log(Level.INFO, "[WorldInventories] " + line);
    }

    public static void logError(String line)
    {
        log.log(Level.SEVERE, "[WorldInventories] " + line);
    }

    public static void logDebug(String line)
    {
        log.log(Level.FINE, "[WorldInventories] " + line);
    }

    private void loadConfigAndCreateDefaultsIfNecessary()
    {
        saveDefaultConfig();

        //getConfig().options().copyDefaults(true);
        saveConfig();
    }

    public List<Group> getGroups()
    {
        return groups;
    }

    private boolean loadConfiguration()
    {
        WorldInventories.groups = new ArrayList<Group>();

        Set<String> nodes = getConfig().getConfigurationSection("groups").getKeys(false);
        for (String group : nodes)
        {
            List<String> worldnames = getConfig().getStringList("groups." + group);
            if (worldnames != null)
            {
                WorldInventories.groups.add(new Group(group, worldnames, getConfig().getBoolean("groups." + group + ".dokeepinv", false)));
                for (String world : worldnames)
                {
                    WorldInventories.logDebug("Adding " + group + ":" + world);
                }
            }
        }

        return true;
    }

    public static Group findFirstGroupForWorld(String world)
    {
        for (Group tGroup : WorldInventories.groups)
        {
            for (String tWorld : tGroup.getWorlds())
            {
                if (tWorld.equals(world))
                {
                    return tGroup;
                }
            }
        }

        return null;
    }

    @Override
    public void onEnable()
    {
        WorldInventories.logStandard("Initialising...");

        boolean bInitialised = true;

        WorldInventories.bukkitServer = this.getServer();
        WorldInventories.pluginManager = WorldInventories.bukkitServer.getPluginManager();

        WorldInventories.logStandard("Loading configuration...");
        this.loadConfigAndCreateDefaultsIfNecessary();

        boolean bConfiguration = this.loadConfiguration();

        if (!bConfiguration)
        {
            WorldInventories.logError("Failed to load configuration.");
            bInitialised = false;
        }
        else
        {
            WorldInventories.logStandard("Loaded configuration successfully");
        }

        if (bInitialised)
        {
            if (getConfig().getBoolean("domiimport"))
            {
                boolean bSuccess = this.importMultiInvData();

                this.getConfig().set("domiimport", false);
                this.saveConfig();

                if (bSuccess)
                {
                    WorldInventories.logStandard("MultiInv data import was a success!");
                }
            }
            
            if(getConfig().getBoolean("do78import") || !getConfig().getBoolean("auto78updated"))
            {
                if(!getConfig().getBoolean("auto78updated"))
                {
                    WorldInventories.logStandard("This appears to be the first time you've run WorldInventories after build 78, automatically trying to import pre-78 data.");
                }
                
                boolean bSuccess = this.import78Data();
                
                this.getConfig().set("do78import", false);
                this.saveConfig();
                
                if(bSuccess)
                {
                    WorldInventories.logStandard("Pre 78 build saves import was a success!");
                    getConfig().set("auto78updated", true);
                    this.saveConfig();
                }
            }

            if(getConfig().getBoolean("do141import") || !getConfig().getBoolean("auto141updated"))
            {
                if(!getConfig().getBoolean("auto141updated"))
                {
                    WorldInventories.logStandard("This appears to be the first time you've run WorldInventories after version 141, automatically trying to import version 141 data.");
                }
                
                boolean bSuccess = this.import141Data();
                
                this.getConfig().set("do141import", false);
                this.saveConfig();
                
                if(bSuccess)
                {
                    WorldInventories.logStandard("Pre 141 build saves import was a success!");
                    getConfig().set("auto141updated", true);
                    this.saveConfig();
                }
            }            
            
            getServer().getPluginManager().registerEvents(new EntityListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (IOException e) {
                WorldInventories.logDebug("Failed to submit Metrics statistics.");
            }            
            
            WorldInventories.logStandard("Initialised successfully!");

            if (getConfig().getInt("saveinterval") >= 30)
            {
                saveTimer.scheduleAtFixedRate(new SaveTask(this), getConfig().getInt("saveinterval") * 1000, getConfig().getInt("saveinterval") * 1000);
            }

        }
        else
        {
            WorldInventories.logError("Failed to initialise.");
        }

    }

    @Override
    public void onDisable()
    {
        savePlayers();

        WorldInventories.logStandard("Plugin disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        String command = cmd.getName();

        if (command.equalsIgnoreCase("wireload"))
        {
            if (args.length == 0)
            {
                if (sender.hasPermission("worldinventories.reload"))
                {
                    WorldInventories.logStandard("Reloading configuration...");
                    reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "Reloaded WorldInventories configuration successfully");
                }
            }

            return true;
        }

        return false;
    }
}
