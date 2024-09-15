package org.dark.customenderchest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ENDER_CHEST) {
            event.setCancelled(true);

            int lines = getEnderChestLines(player);
            Inventory customInventory = Bukkit.createInventory(player, lines * 9, plugin.getConfig().getString("inventory-title", "Custom EnderChest"));

            ItemStack[] items = databaseHandler.loadInventory(player.getUniqueId());
            if (items != null && items.length > 0) {
                customInventory.setContents(items);
            }

            player.openInventory(customInventory);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (event.getView().getTitle().equals(plugin.getConfig().getString("inventory-title", "Custom EnderChest"))) {
            databaseHandler.saveInventory(player.getUniqueId(), event.getInventory().getContents());
        }
    }

    private int getEnderChestLines(Player player) {
        for (int i = 6; i > 0; i--) {
            if (player.hasPermission("EnderChestBar." + i)) {
                return i;
            }
        }
        return plugin.getConfig().getInt("default-lines", 1);
    }
}