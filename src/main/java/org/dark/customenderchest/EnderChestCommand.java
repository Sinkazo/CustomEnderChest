package org.dark.customenderchest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
            sender.sendMessage(getMessage("usage", "&cUso: /enderchest <open|view|reload>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("open")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("EnderChestBar.open")) {
                    // Abrir ender chest del propio jugador
                    player.openInventory(player.getEnderChest());

                    // Mensaje y sonido al abrir
                    String message = getMessage("open", "&aEnderChest abierto!");
                    player.sendMessage(message);
                    player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
                } else {
                    player.sendMessage(getMessage("no-permission-open", "&cNo tienes permiso para abrir tu EnderChest."));
                }
            } else {
                sender.sendMessage(getMessage("only-players", "&cEste comando solo puede ser usado por jugadores."));
            }
        } else if (args[0].equalsIgnoreCase("view")) {
            if (args.length < 2) {
                sender.sendMessage(getMessage("view-usage", "&cUso: /enderchest view <jugador>"));
                return true;
            }

            if (sender.hasPermission("EnderChestBar.view")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && target.isOnline()) {
                    // Ver el ender chest de otro jugador si está en línea
                    ((Player) sender).openInventory(target.getEnderChest());
                    sender.sendMessage(getMessage("viewing", "&aViendo el EnderChest de &f" + target.getName()));
                } else {
                    try {
                        UUID targetUUID = UUID.fromString(args[1]);
                        ItemStack[] items = databaseHandler.loadInventory(targetUUID);

                        if (items.length > 0) {
                            Inventory customInventory = Bukkit.createInventory(null, 27, "EnderChest de " + args[1]);
                            customInventory.setContents(items);
                            ((Player) sender).openInventory(customInventory);
                            sender.sendMessage(getMessage("viewing", "&aViendo el EnderChest de &f" + args[1]));
                        } else {
                            sender.sendMessage(getMessage("no-enderchest-found", "&cNo se pudo encontrar el EnderChest del jugador especificado."));
                        }
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(getMessage("invalid-uuid", "&cUUID del jugador no válido."));
                    }
                }
            } else {
                sender.sendMessage(getMessage("no-permission-view", "&cNo tienes permiso para ver EnderChests."));
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("EnderChestBar.reload")) {
                plugin.reloadConfig();
                sender.sendMessage(getMessage("config-reloaded", "&aConfiguración recargada correctamente."));
            } else {
                sender.sendMessage(getMessage("no-permission-reload", "&cNo tienes permiso para recargar la configuración."));
            }
        } else {
            sender.sendMessage(getMessage("usage", "&cUso: /enderchest <open|view|reload>"));
        }

        return true;
    }

    // Método auxiliar para obtener mensajes desde el config.yml
    private String getMessage(String path, String defaultMsg) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages." + path, defaultMsg));
    }
}
