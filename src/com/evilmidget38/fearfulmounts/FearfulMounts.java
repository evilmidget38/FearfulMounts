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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
 
public class FearfulMounts extends JavaPlugin implements Listener {
    static String[] consolePig;
    static {
        // The stuff you find on the internet...
        consolePig = new String[13];
        consolePig[0] = "         ^,    ,^";
        consolePig[1] = "        /  ----  \\ ";
        consolePig[2] = "       / _\\    /_ \\  Ful";
        consolePig[3] = "       |  / __ \\  |";
        consolePig[4] = "       |   /oo\\   |            ,-.";
        consolePig[5] = "       |   \\__/   |____________.:'";
        consolePig[6] = "       \\   .__.   /            \\ '";
        consolePig[7] = "        '.______.'              \\";
        consolePig[8] = "            \\                   |";
        consolePig[9] = "             |  /____...-----\\  |";
        consolePig[10] = "             |  |            |  |";
        consolePig[12] = "             |^^|            |^^| ";
    }
 
    int health;
    Map<String, UUID> mounts = new HashMap<String, UUID>();
 
    public void onEnable() {
        health = getConfig().getInt("mount-health", 20);
        getConfig().set("mount-health", health);
        saveConfig();
    }
 
    public void onDisable() {
 
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
        if (mount == null) {
            mount = (Pig) player.getWorld().spawnEntity(getRandomNearby(player.getLocation()), EntityType.PIG);
            mount.setSaddle(true);
            mounts.put(player.getName(), mount.getUniqueId());
            mount.setMaxHealth(health);
            mount.setHealth(health);
        }
        if (!player.getInventory().contains(Material.CARROT_STICK)) {
            player.getInventory().addItem(new ItemStack(Material.CARROT_STICK));
        }
        player.sendMessage(ChatColor.GREEN+"Ride on!");
        return true;
    }
 
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Pig) {
            String owner = null;
            for (Entry<String, UUID> entry : mounts.entrySet()) {
                if (entry.getValue().equals(e.getEntity().getUniqueId())) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        player.sendMessage(ChatColor.RED+"Your valiant mount has died!");
                    }
                    owner = entry.getKey();
                }
            }
            if (owner != null) {
                mounts.remove(owner);
            }
        }
    }
 
    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent e) {
        Pig pig = getMount(mounts.get(e.getPlayer().getName()), e.getPlayer().getWorld());
        if (pig != null) {
            pig.remove();
            e.getPlayer().sendMessage(ChatColor.RED+"You're leaving the world, so we removed your mount.");
            mounts.remove(e.getPlayer().getName());
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Pig pig = getMount(mounts.get(e.getPlayer().getName()), e.getPlayer().getWorld());
        pig.remove();
        mounts.remove(e.getPlayer().getName());
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
        int x = rand.nextInt(4)-2;
        int z = rand.nextInt(4)-2;
        Location random = loc.add(x, loc.getWorld().getHighestBlockYAt(x, z), z);
        // This can only happen if the pig is too high or too low from the player.
        if (random.distanceSquared(loc) > 16) {
            return getRandomNearby(loc);
        }
        return random;
    }
 
}