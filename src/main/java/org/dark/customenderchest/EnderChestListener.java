package org.dark.customenderchest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.block.Action;

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

        // Verificar si el jugador interactúa con un EnderChest
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            event.setCancelled(true); // Cancelar la apertura normal

            int lines = getEnderChestLines(player);
            Inventory customInventory = Bukkit.createInventory(player, lines * 9, plugin.getConfig().getString("inventory-title", "Custom EnderChest"));

            // Cargar el inventario del jugador desde la base de datos
            ItemStack[] items = databaseHandler.loadInventory(player.getUniqueId());
            customInventory.setContents(items);

            // Abrir el inventario personalizado
            player.openInventory(customInventory);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(plugin.getConfig().getString("inventory-title", "Custom EnderChest"))) {
            event.setCancelled(false); // Permitir mover items en el inventario personalizado
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (event.getView().getTitle().equals(plugin.getConfig().getString("inventory-title", "Custom EnderChest"))) {
            // Guardar el inventario del jugador cuando lo cierra
            databaseHandler.saveInventory(player.getUniqueId(), event.getInventory().getContents());
        }
    }

    private int getEnderChestLines(Player player) {
        for (int i = 6; i > 0; i--) {
            if (player.hasPermission("EnderChestBar." + i)) {
                return i;
            }
        }
        return plugin.getConfig().getInt("default-lines", 1); // Valor por defecto si no tiene permisos
    }
}