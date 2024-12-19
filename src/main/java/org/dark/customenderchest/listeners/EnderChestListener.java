package org.dark.customenderchest.listeners;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.dark.customenderchest.CustomEnderChest;
import org.dark.customenderchest.utilities.DatabaseHandler;

import java.util.Arrays;

public class EnderChestListener implements Listener {
    private final CustomEnderChest plugin;
    private final DatabaseHandler databaseHandler;

    public EnderChestListener(CustomEnderChest plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnderChestOpen(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                event.getClickedBlock() != null &&
                event.getClickedBlock().getType() == Material.ENDER_CHEST) {

            event.setCancelled(true);
            Player player = event.getPlayer();

            // Use the same method as the command to open the custom EnderChest
            openEnderChest(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        // Intercept any Ender Chest inventory opening
        if (event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            openEnderChest(player);
        }
    }

    public void openEnderChest(Player player) {
        int lines = getEnderChestLines(player);
        String title = plugin.getInventoryTitleForLines(lines);

        Inventory enderChest = Bukkit.createInventory(player, lines * 9, title);
        ItemStack[] items = databaseHandler.loadInventory(player.getUniqueId());

        if (items != null) {
            enderChest.setContents(Arrays.copyOf(items, lines * 9));
        }

        player.openInventory(enderChest);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);

        String message = getMessage("viewing-own");
        if (!message.isEmpty()) {
            sendMessageExceptIfBlank(player, message);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        if (event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.ENDER_CHEST ||
                event.getView().getTitle().equals(plugin.getInventoryTitleForLines(getEnderChestLines(player)))) {

            ItemStack[] contents = event.getInventory().getContents();

            Bukkit.getScheduler().runTask(plugin, () ->
                    databaseHandler.saveInventory(player.getUniqueId(), contents));

            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1.0f);
        }
    }

    private int getEnderChestLines(Player player) {
        for (int i = 6; i > 0; i--) {
            if (player.hasPermission("EnderChestBar." + i)) {
                return Math.min(i, 6);
            }
        }
        return plugin.getConfig().getInt("default-lines", 1);
    }

    private String getMessage(String path) {
        String message = plugin.getConfig().getString("messages." + path);
        return message != null ? ChatColor.translateAlternateColorCodes('&', message) : "";
    }

    private void sendMessageExceptIfBlank(CommandSender sender, String message) {
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }
}