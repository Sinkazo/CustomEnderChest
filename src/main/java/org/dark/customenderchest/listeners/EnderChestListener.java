package org.dark.customenderchest.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;
import org.dark.customenderchest.CustomEnderChest;
import org.dark.customenderchest.utilities.DatabaseHandler;

public class EnderChestListener implements Listener {

    private final CustomEnderChest plugin;
    private final DatabaseHandler databaseHandler;

    public EnderChestListener(CustomEnderChest plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
    }

    @EventHandler
    public void onEnderChestOpen(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            event.setCancelled(true);

            int currentLines = getEnderChestLines(player);
            if (currentLines * 9 > 54) {
                currentLines = 6;  // Ajustar a un máximo de 6 líneas
            }

            Inventory customInventory = Bukkit.createInventory(player, currentLines * 9, plugin.getConfig().getString("inventory-title", "Custom EnderChest"));

            ItemStack[] items = databaseHandler.loadInventory(player.getUniqueId());

            // Recortar el inventario si el jugador ahora tiene menos líneas
            if (items != null && items.length > 0) {
                if (items.length > currentLines * 9) {
                    // Recortar los ítems que no caben
                    ItemStack[] trimmedItems = new ItemStack[currentLines * 9];
                    System.arraycopy(items, 0, trimmedItems, 0, currentLines * 9);
                    customInventory.setContents(trimmedItems);
                } else {
                    customInventory.setContents(items);
                }
            }

            player.openInventory(customInventory);

            // Reproducir sonido
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);

            // Enviar mensaje
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.viewing-own", "&aEstás viendo tu EnderChest")));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (event.getView().getTitle().equals(plugin.getConfig().getString("inventory-title", "Custom EnderChest"))) {
            databaseHandler.saveInventory(player.getUniqueId(), event.getInventory().getContents());
        }
    }

    // Obtener las líneas de acuerdo a los permisos actuales del jugador
    private int getEnderChestLines(Player player) {
        for (int i = 6; i > 0; i--) {
            if (player.hasPermission("EnderChestBar." + i)) {
                return i;
            }
        }
        // Si no tiene permisos para más líneas, devuelve las líneas por defecto
        return plugin.getConfig().getInt("default-lines", 1);
    }
}
