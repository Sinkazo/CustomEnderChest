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
			sendMessageExceptIfBlank(sender, getMessage("usage"));
			return true;
		}

		switch (args[0].toLowerCase()) {
		case "open":
			handleOpenCommand(sender);
			break;
		default:
			sendMessageExceptIfBlank(sender, getMessage("usage"));
		}

		return true;
	}

	private void handleOpenCommand(CommandSender sender) {
		if (!(sender instanceof Player)) {
			sendMessageExceptIfBlank(sender, getMessage("only-players"));
			return;
		}

		Player player = (Player) sender;
		if (!player.hasPermission("EnderChestBar.open")) {
			sendMessageExceptIfBlank(player, getMessage("no-permission-open"));
			return;
		}

		openCustomEnderChest(player, player.getUniqueId(), player.getName(), true);
	}

	private void openCustomEnderChest(Player viewer, UUID targetUUID, String targetName, boolean isPersonal) {
		int lines = getEnderChestLines(targetUUID);
		String title = plugin.getInventoryTitleForLines(lines) + (isPersonal ? "" : " - " + targetName);

		Inventory customInventory = Bukkit.createInventory(null, lines * 9, title);
		ItemStack[] items = databaseHandler.loadInventory(targetUUID);

		if (items != null && items.length > 0) {
			// Only set contents up to the current permission level
			customInventory.setContents(Arrays.copyOf(items, lines * 9));
		}

		openEnderChests.put(viewer.getUniqueId(), targetUUID);
		viewer.openInventory(customInventory);

		if (isPersonal) {
			sendMessageExceptIfBlank(viewer, getMessage("viewing-own")); //
		} else {
			sendMessageExceptIfBlank(viewer, getMessage("viewing").replace("%player%", targetName));
		}

		viewer.playSound(viewer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
	}

	private UUID getUUIDFromName(String playerName) {
		Player onlinePlayer = Bukkit.getPlayer(playerName);
		if (onlinePlayer != null) {
			return onlinePlayer.getUniqueId();
		} else {
			try {
				return Bukkit.getOfflinePlayer(playerName).getUniqueId();
			} catch (Exception e) {
				return null; //
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

	private void sendMessageExceptIfBlank(CommandSender sender, String message) {
		if (!message.equals("")) {
			sender.sendMessage(getMessage(message));
		}
	}


	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		UUID viewerUUID = player.getUniqueId();

		if (openEnderChests.containsKey(viewerUUID)) {
			UUID targetUUID = openEnderChests.get(viewerUUID);

			databaseHandler.saveInventory(targetUUID, event.getInventory().getContents());

			openEnderChests.remove(viewerUUID);
		}
	}
}