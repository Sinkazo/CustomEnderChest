package org.dark.customenderchest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
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
                    // Obtener el número de líneas basadas en los permisos
                    int lines = getEnderChestLines(player);

                    // Crear un inventario personalizado con el número de líneas según los permisos
                    Inventory customInventory = Bukkit.createInventory(player, lines * 9, plugin.getConfig().getString("inventory-title", "Custom EnderChest"));

                    // Cargar el inventario desde la base de datos o usar el ender chest normal
                    customInventory.setContents(player.getEnderChest().getContents());

                    // Abrir el inventario personalizado
                    player.openInventory(customInventory);

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
                    // Obtener el número de líneas basadas en los permisos del jugador objetivo
                    int lines = getEnderChestLines(target);

                    // Crear un inventario personalizado con el número de líneas según los permisos
                    Inventory customInventory = Bukkit.createInventory((InventoryHolder) sender, lines * 9, "EnderChest de " + target.getName());
                    customInventory.setContents(target.getEnderChest().getContents());

                    // Abrir el inventario personalizado
                    ((Player) sender).openInventory(customInventory);
                    sender.sendMessage(getMessage("viewing", "&aViendo el EnderChest de &f" + target.getName()));
                } else {
                    // Si el jugador no está conectado, buscar en la base de datos
                    try {
                        UUID targetUUID = UUID.fromString(args[1]);
                        ItemStack[] items = databaseHandler.loadInventory(targetUUID);

                        if (items.length > 0) {
                            int lines = getEnderChestLinesFromDatabase(targetUUID);

                            // Crear inventario con las líneas según permisos
                            Inventory customInventory = Bukkit.createInventory(null, lines * 9, "EnderChest de " + args[1]);
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

    // Metodo para obtener el número de líneas según los permisos
    private int getEnderChestLines(Player player) {
        for (int i = 6; i > 0; i--) {
            if (player.hasPermission("EnderChestBar." + i)) {
                return i;
            }
        }
        return plugin.getConfig().getInt("default-lines", 1); // Valor por defecto si no tiene permisos
    }

    // Metodo para obtener las líneas según el jugador objetivo (de la base de datos si está desconectado)
    private int getEnderChestLinesFromDatabase(UUID uuid) {
        // Aquí puedes implementar tu lógica para extraer las líneas según permisos del jugador desde la base de datos
        // Si no tienes esa lógica aún, puedes devolver un valor por defecto
        return plugin.getConfig().getInt("default-lines", 1);
    }

    // Metodo auxiliar para obtener mensajes desde el config.yml
    private String getMessage(String path, String defaultMsg) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages." + path, defaultMsg));
    }
}
