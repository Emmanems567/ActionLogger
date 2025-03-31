package com.vch.actionLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ActionLogger extends JavaPlugin implements Listener {

    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private final int MAX_FILES = 30;
    private final int MAX_LOGS = 500;
    private final ArrayList<String> logs = new ArrayList<>();

    @Override
    public void onEnable() {
        
        Bukkit.getPluginManager().registerEvents(this, this);
        cleanOldLogs();

        ConsoleCommandSender console = Bukkit.getConsoleSender();
        String version = this.getDescription().getVersion();
        console.sendMessage("\n" +
            ChatColor.GOLD + "==========================" + ChatColor.RESET + "\n" +
            ChatColor.GREEN + "Action Logger " + version + ChatColor.RESET + "\n" +
            ChatColor.GOLD + "==========================" + ChatColor.RESET + "\n"
        );

    }

    @Override
    public void onDisable() {
        writeActions();
    }

    private File getLogFile() {
        String dateStr = fileDateFormat.format(new Date());
        File folder = new File(getDataFolder(), "ActionLogs");
        if(!folder.exists()) folder.mkdirs();
        return new File(folder, "action_logs_" + dateStr + ".txt");
    }

    private void writeActions() {

        File logFile = getLogFile();

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            for(String log : logs) {
                writer.write(log);
                writer.newLine();
            }
        } 
        catch(IOException e) {
            getLogger().severe("Error al escribir en el archivo de logs");
        }

        cleanOldLogs();
        logs.clear();

    }

    private void logAction(String dimension, String player, String eventType, String block, int amount, int x, int y, int z) {
        
        String timestamp = logDateFormat.format(new Date());
        String logEntry = String.format("%s,%s,%s,%s,%s,%d,%d,%d,%d", dimension, timestamp, player, eventType, block, amount, x, y, z);
        logs.add(logEntry);

        if(logs.size() >= MAX_LOGS)
            writeActions();

    }

    private void cleanOldLogs() {

        File folder = new File(getDataFolder(), "ActionLogs");
        if(!folder.exists()) return;

        File[] files = folder.listFiles((dir, name) -> name.startsWith("action_logs_") && name.endsWith(".txt"));
        if(files == null || files.length <= MAX_FILES) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        for(int i = 0; i < files.length - MAX_FILES; i++)
            if(!files[i].delete())
                getLogger().log(Level.WARNING, "No se pudo eliminar el archivo de log viejo: {0}", files[i].getName());

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        String player = event.getPlayer().getName();
        String blockType = block.getType().toString();
        String dimension = block.getWorld().getName();
        logAction(dimension, player, "BREAK", blockType, 1, x, y, z);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        String player = event.getPlayer().getName();
        String blockType = block.getType().toString();
        String dimension = block.getWorld().getName();
        logAction(dimension, player, "PLACE", blockType, 1, x, y, z);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {

        InventoryType type = event.getInventory().getType();
        if(event.getInventory().getLocation() == null) return;

        String player = event.getPlayer().getName();
        String dimension = event.getInventory().getLocation().getWorld().getName();
        int x = event.getInventory().getLocation().getBlockX();
        int y = event.getInventory().getLocation().getBlockY();
        int z = event.getInventory().getLocation().getBlockZ();

        String container = type.name();
        logAction(dimension, player, "OPEN", container, 1, x, y, z);

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        
        InventoryType type = event.getInventory().getType();
        if(event.getInventory().getLocation() == null) return;

        if(event.isShiftClick() || event.getAction().name().contains("PICKUP")) {

            ItemStack item = event.getCurrentItem();
            int amount = item == null ? 0 : item.getAmount();

            if (item != null && item.getType() != Material.AIR) {
                
                String player = event.getWhoClicked().getName();
                String dimension = event.getInventory().getLocation().getWorld().getName();
                int x = event.getInventory().getLocation().getBlockX();
                int y = event.getInventory().getLocation().getBlockY();
                int z = event.getInventory().getLocation().getBlockZ();

                String container = type.name();
                String itemName = item.getType().toString();

                if(item.getEnchantments().size() > 0) {
                    StringBuilder enchantments = new StringBuilder();
                    item.getEnchantments().forEach((enchantment, level) -> enchantments.append(enchantment.getKeyOrNull().getKey()).append(":").append(level).append(","));
                    itemName += "{" + enchantments.substring(0, enchantments.length() - 1) + "}";
                }

                logAction(dimension, player, "TAKE_" + container, itemName, amount, x, y, z);

            }

        }

    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        if(!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Entity entity = event.getEntity();
        if(!(entity instanceof LivingEntity)) return;

        LivingEntity living = (LivingEntity) entity;
        if(living.getHealth() - event.getFinalDamage() > 0) return;

        EntityType type = entity.getType();
        if(!isTrackedEntity(type)) return;

        int x = entity.getLocation().getBlockX();
        int y = entity.getLocation().getBlockY();
        int z = entity.getLocation().getBlockZ();
        String dimension = entity.getWorld().getName();
        logAction(dimension, player.getName(), "KILL", type.name(), 1, x, y, z);

    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        
        Player player = event.getEntity();
        String playerName = player.getName();
        
        String killerEntityName = "Environment";
        Player killerPlayer = player.getKiller();
        String dimension = player.getWorld().getName();

        if(killerPlayer != null) {
            killerEntityName = killerPlayer.getName();
        } 
        else if(player.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) player.getLastDamageCause();
            Entity damager = damageEvent.getDamager();
            killerEntityName = damager.getType().toString();
        }

        player.sendMessage(ChatColor.GOLD + "Tu lugar de muerte es " + ChatColor.BLUE + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ());

        getLogger().log(Level.INFO, "{0} was killed by {1} at {2}, {3}, {4}", new Object[]{playerName, killerEntityName, player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()});
        logAction(dimension, killerEntityName, "DEATH", playerName, 1, player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());

    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String dimension = player.getWorld().getName();
        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();
        logAction(dimension, playerName, "QUIT", "PLAYER", 1, x, y, z);
    }

    private boolean isTrackedEntity(EntityType type) {
        return switch (type) {
            case VILLAGER, WOLF, CAT, PARROT, BEE, AXOLOTL, HORSE, FROG -> true;
            default -> false;
        };
    }

}