package com.vch.actionLogger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public final class ActionLogger extends JavaPlugin implements Listener {
    private final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        cleanOldLogs();  // Limpiar archivos antiguos al iniciar
    }

    private File getLogFile() {
        String dateStr = fileDateFormat.format(new Date());
        File folder = new File(getDataFolder(), "ActionLogs");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return new File(folder, "action_logs_" + dateStr + ".txt");
    }

    private void logAction(String player, String eventType, String block, int x, int y, int z) {
        String timestamp = logDateFormat.format(new Date());
        String logEntry = String.format("%s,%s,%s,%s,%d,%d,%d", timestamp, player, eventType, block, x, y, z);

        File logFile = getLogFile();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            getLogger().severe("Error al escribir en el archivo de logs");
            e.printStackTrace();
        }

        cleanOldLogs();  // Revisa si se debe borrar algo después de escribir
    }

    private void cleanOldLogs() {
        File folder = new File(getDataFolder(), "ActionLogs");

        if (!folder.exists()) return;

        File[] files = folder.listFiles((dir, name) -> name.startsWith("action_logs_") && name.endsWith(".txt"));

        if (files == null || files.length <= 15) return;

        // Ordenar por fecha de modificación ascendente (el más viejo primero)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        for (int i = 0; i < files.length - 15; i++) {
            if (!files[i].delete()) {
                getLogger().warning("No se pudo eliminar el archivo de log viejo: " + files[i].getName());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        String player = event.getPlayer().getName();
        String blockType = block.getType().toString();

        logAction(player, "BREAK", blockType, x, y, z);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        String player = event.getPlayer().getName();
        String blockType = block.getType().toString();

        logAction(player, "PLACE", blockType, x, y, z);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryType type = event.getInventory().getType();

        // Asegurarnos que el inventario tiene una ubicación en el mundo (evita virtuales)
        if (event.getInventory().getLocation() == null) return;

        String player = event.getPlayer().getName();
        int x = event.getInventory().getLocation().getBlockX();
        int y = event.getInventory().getLocation().getBlockY();
        int z = event.getInventory().getLocation().getBlockZ();

        // Ahora lo dejamos genérico
        String container = type.name();

        logAction(player, "OPEN", container, x, y, z);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryType type = event.getInventory().getType();

        if (event.getInventory().getLocation() == null) return;

        // Solo si el jugador saca cosas (Shift-click o pickup normal)
        if (event.isShiftClick() || event.getAction().name().contains("PICKUP")) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
                String player = event.getWhoClicked().getName();
                int x = event.getInventory().getLocation().getBlockX();
                int y = event.getInventory().getLocation().getBlockY();
                int z = event.getInventory().getLocation().getBlockZ();

                String container = type.name();
                String itemName = item.getType().toString();

                // Queda registrado el TAKE con el nombre del ítem y tipo de contenedor
                logAction(player, "TAKE_" + container, itemName, x, y, z);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Solo si el que daña es un jugador
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Entity entity = event.getEntity();

        // Solo nos interesan entidades vivientes
        if (!(entity instanceof LivingEntity)) return;

        LivingEntity living = (LivingEntity) entity;

        // Si el daño no la mata, salimos
        if (living.getHealth() - event.getFinalDamage() > 0) return;

        EntityType type = entity.getType();

        // Validamos si es una entidad que queremos rastrear
        if (!isTrackedEntity(type)) return;

        int x = entity.getLocation().getBlockX();
        int y = entity.getLocation().getBlockY();
        int z = entity.getLocation().getBlockZ();

        logAction(player.getName(), "KILL", type.name(), x, y, z);
    }

    private boolean isTrackedEntity(EntityType type) {
        return switch (type) {
            case VILLAGER, WOLF, CAT, PARROT, BEE, AXOLOTL, HORSE, FROG -> true;
            default -> false;
        };
    }
}
