package com.evilmidget38.fearfulmounts;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class FearfulMounts extends JavaPlugin implements Listener {
    static String[] consolePig;
    static {
        // The stuff you find on the internet...
        consolePig = new String[12];
        consolePig[0] = "         ^,    ,^";
        consolePig[1] = "        /  ----  \\ ";
        consolePig[2] = "       / _\\    /_ \\  Ful";
        consolePig[3] = "       |  / __ \\  |";
        consolePig[4] = "       |   /oo\\   |            ,-.";
        consolePig[5] = "       |   \\__/   |____________.:'";
        consolePig[6] = "       \\   .__.   /            \\ '";
        consolePig[7] = "        '.______.'              \\";
        consolePig[8] = "            \\                   |";
        consolePig[9] = "             |  /____..._____\\  |";
        consolePig[10] = "             |  |            |  |";
        consolePig[11] = "             |^^|            |^^|";
    }

    int health;
    boolean cacheMount;
    boolean despawnMount;
    Map<String, UUID> mounts = new HashMap<String, UUID>();
    // Mounts that were dismounted.
    Map<String, Integer> mountCache = new HashMap<String, Integer>();

    public void onEnable() {
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    public void loadConfig() {
        health = getConfig().getInt("mount-health", 20);
        getConfig().set("mount-health", health);
        cacheMount = getConfig().getBoolean("persist-mount-health", true);
        getConfig().set("persist-mount-health", cacheMount);
        despawnMount = getConfig().getBoolean("remove-mount-on-dismount", false);
        getConfig().set("remove-mount-on-dismount", despawnMount);
        if (getConfig().contains("player-data")) {
            for (String s : getConfig().getConfigurationSection("player-data").getKeys(false)) {
                mountCache.put(s, getConfig().getConfigurationSection("player-data").getInt(s));
            }
        }
        saveConfig();
    }
    
    public void save() {
        getConfig().set("player-data", mountCache);
        saveConfig();
    }

    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            Pig pig = getMount(mounts.get(player.getName()), player.getWorld());
            if (pig != null && cacheMount) {
                mountCache.put(player.getName(), pig.getHealth());
            }
        }
        save();
    }
    

    public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
        if (!(sender instanceof Player)) {
            for(String s : consolePig) {
                sender.sendMessage(s);
            }
            sender.sendMessage("There's your mount.");
            return true;
        }
        Player player = (Player)sender;
        Pig mount = getMount(mounts.get(player.getName()), player.getWorld());
        if (cmd.getName().equalsIgnoreCase("unmount")) {
            if (mount == null) {
                player.sendMessage(ChatColor.RED+"Unable to unmount- Your mount is either dead or already cached.");
                return true;
            }
            mountCache.put(player.getName(), mount.getHealth());
            mounts.remove(player.getName());
            mount.remove();
            return true;
        }
        if (mount == null) {
            mount = (Pig) player.getWorld().spawnEntity(getRandomNearby(player.getLocation()), EntityType.PIG);
            mount.setSaddle(true);
            mounts.put(player.getName(), mount.getUniqueId());
            mount.setMaxHealth(health);
            mount.setHealth(health);
            if (mountCache.containsKey(player.getName())) {
                int cacheValue = mountCache.get(player.getName());
                mount.setHealth(cacheValue);
            }
        } else {
            mount.teleport(getRandomNearby(player.getLocation()));
        }
        if (!player.getInventory().contains(Material.CARROT_STICK)) {
            player.getInventory().addItem(new ItemStack(Material.CARROT_STICK));
        }
        player.sendMessage(ChatColor.GREEN+"Ride on!");
        return true;
    }
    
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (isRidingMount(e.getPlayer()) && e.getRightClicked().getUniqueId().equals(mounts.get(e.getPlayer().getName()))) {
            // They're dismounting.
            if (despawnMount) {
                Pig pig = (Pig)e.getRightClicked();
                mountCache.put(e.getPlayer().getName(), pig.getHealth());
                mounts.remove(e.getPlayer().getName());
                pig.remove();
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Pig) {
            for (Entry<String, UUID> entry : mounts.entrySet()) {
                if (entry.getValue().equals(e.getEntity().getUniqueId())) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        player.sendMessage(ChatColor.RED+"Your valiant mount has died!");
                    }
                    mountCache.remove(entry.getKey());
                    mounts.remove(entry.getKey());
                    return;
                }
            }

        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
        Pig pig = getMount(mounts.get(e.getPlayer().getName()), e.getPlayer().getWorld());
        if (pig != null) {
            if (cacheMount) {
                mountCache.put(e.getPlayer().getName(), pig.getHealth());
            }
            pig.remove();
            e.getPlayer().sendMessage(ChatColor.RED+"You're leaving the world, so we removed your mount.");
            mounts.remove(e.getPlayer().getName());
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Pig pig = getMount(mounts.get(e.getPlayer().getName()), e.getPlayer().getWorld());
        if (pig != null) {
            if (cacheMount) {
                mountCache.put(e.getPlayer().getName(), pig.getHealth());
            }
            pig.remove();
            mounts.remove(e.getPlayer().getName());
        }
    }

    public Pig getMount(UUID id, World world) {
        for (Pig p : world.getEntitiesByClass(Pig.class)) {
            if (p.getUniqueId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public Location getRandomNearby(Location loc) {
        Random rand = new Random();
        int x = (int) (rand.nextInt(4)-2 + loc.getX());
        int z = (int) (rand.nextInt(4)-2 + loc.getZ());
        Location highestAt = loc.getWorld().getHighestBlockAt(loc).getLocation();
        Location random = new Location(loc.getWorld(), x, loc.getWorld().getHighestBlockYAt(x, z), z);
        // This can only happen if the pig is too high or too low from the player.
        if (random.distanceSquared(highestAt) > 36) {
            return getRandomNearby(loc);
        }
        return random;
    }
    
    public boolean isRidingMount(Player player) {
        return (player.getVehicle() != null && player.getVehicle().equals(getMount(mounts.get(player.getName()), player.getWorld())));
    }

}