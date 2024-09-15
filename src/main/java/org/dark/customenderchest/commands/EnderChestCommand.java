package org.dark.customenderchest.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.dark.customenderchest.CustomEnderChest;
import org.dark.customenderchest.utilities.DatabaseHandler;

import java.util.UUID;

public class EnderChestCommand implements CommandExecutor {

    private final CustomEnderChest plugin;
    private final DatabaseHandler databaseHandler;

    public EnderChestCommand(CustomEnderChest plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getMessage("usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open":
                handleOpenCommand(sender);
                break;
            case "view":
                handleViewCommand(sender, args);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            default:
                sender.sendMessage(getMessage("usage"));
        }

        return true;
    }

    private void handleOpenCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-players"));
            return;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("EnderChestBar.open")) {
            player.sendMessage(getMessage("no-permission-open"));
            return;
        }

        openCustomEnderChest(player, player.getUniqueId(), player.getName());
    }

    private void handleViewCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getMessage("view-usage"));
            return;
        }

        if (!sender.hasPermission("EnderChestBar.view")) {
            sender.sendMessage(getMessage("no-permission-view"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID;
        String targetName;

        if (target != null && target.isOnline()) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            try {
                targetUUID = UUID.fromString(args[1]);
                targetName = args[1];
            } catch (IllegalArgumentException e) {
                sender.sendMessage(getMessage("invalid-uuid"));
                return;
            }
        }

        if (sender instanceof Player) {
            openCustomEnderChest((Player) sender, targetUUID, targetName);
        } else {
            sender.sendMessage(getMessage("only-players"));
        }
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("EnderChestBar.reload")) {
            sender.sendMessage(getMessage("no-permission-reload"));
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage(getMessage("config-reloaded"));
    }

    private void openCustomEnderChest(Player viewer, UUID targetUUID, String targetName) {
        int lines = getEnderChestLines(targetUUID);
        Inventory customInventory = Bukkit.createInventory(null, lines * 9,
                plugin.getConfig().getString("inventory-title", "Custom EnderChest") + " - " + targetName);

        ItemStack[] items = databaseHandler.loadInventory(targetUUID);
        if (items != null && items.length > 0) {
            customInventory.setContents(items);
        }

        viewer.openInventory(customInventory);
        viewer.sendMessage(getMessage("viewing").replace("%player%", targetName));
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
    }

    private int getEnderChestLines(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            for (int i = 6; i > 0; i--) {
                if (player.hasPermission("EnderChestBar." + i)) {
                    return i;
                }
            }
        }
        return plugin.getConfig().getInt("default-lines", 1);
    }

    private String getMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages." + path, "Message not found: " + path));
    }
}