package org.dark.customenderchest.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.dark.customenderchest.CustomEnderChest;
import org.dark.customenderchest.utilities.DatabaseHandler;

import java.util.*;

public class EnderChestCommand implements CommandExecutor, Listener {


    private final CustomEnderChest plugin;
    private final DatabaseHandler databaseHandler;

    // Mapa para asociar jugadores con inventarios que se están viendo
    private final Map<UUID, UUID> openEnderChests = new HashMap<>();

    public EnderChestCommand(CustomEnderChest plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;

        // Registrar el listener para capturar el evento de cierre de inventario
        Bukkit.getPluginManager().registerEvents(this, plugin);
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

        // Abre el EnderChest personal y envía el mensaje del config para su propio EnderChest
        openCustomEnderChest(player, player.getUniqueId(), player.getName(), true);
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

        String targetName = args[1];
        UUID targetUUID = getUUIDFromName(targetName);

        if (targetUUID == null) {
            sender.sendMessage(getMessage("invalid-uuid"));
            return;
        }

        if (sender instanceof Player) {
            openCustomEnderChest((Player) sender, targetUUID, targetName, false);
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

    // Método para abrir el EnderChest del jugador objetivo o el propio
    private void openCustomEnderChest(Player viewer, UUID targetUUID, String targetName, boolean isPersonal) {
        int lines = getEnderChestLines(targetUUID);
        Inventory customInventory = Bukkit.createInventory(null, lines * 9,
                plugin.getConfig().getString("inventory-title", "Custom EnderChest") + (isPersonal ? "" : " - " + targetName));

        ItemStack[] items = databaseHandler.loadInventory(targetUUID);
        if (items != null && items.length > 0) {
            customInventory.setContents(items);
        }

        // Almacenar que este jugador está viendo el EnderChest de otro (o el suyo)
        openEnderChests.put(viewer.getUniqueId(), targetUUID);

        viewer.openInventory(customInventory);

        // Enviar el mensaje adecuado (personal o para ver el de otro jugador)
        if (isPersonal) {
            viewer.sendMessage(getMessage("viewing-own")); // Mensaje personalizado para su propio EnderChest
        } else {
            viewer.sendMessage(getMessage("viewing").replace("%player%", targetName));
        }

        // Reproducir sonido
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
    }

    // Obtener el UUID del jugador a partir del nombre (online u offline)
    private UUID getUUIDFromName(String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        } else {
            try {
                return Bukkit.getOfflinePlayer(playerName).getUniqueId();
            } catch (Exception e) {
                return null; // Si no se encuentra el jugador ni se puede obtener el UUID
            }
        }
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

    // Guardar el inventario cuando el jugador cierra el EnderChest
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID viewerUUID = player.getUniqueId();

        // Verificar si el jugador estaba viendo su propio EnderChest o el de otro
        if (openEnderChests.containsKey(viewerUUID)) {
            UUID targetUUID = openEnderChests.get(viewerUUID);

            // Guardar el inventario de la base de datos del jugador que estaba viendo
            databaseHandler.saveInventory(targetUUID, event.getInventory().getContents());

            // Eliminar la referencia de la visualización
            openEnderChests.remove(viewerUUID);
        }
    }
}