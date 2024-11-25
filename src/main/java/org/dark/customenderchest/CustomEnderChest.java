package org.dark.customenderchest;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.dark.customenderchest.commands.AdminEnderChestCommand;
import org.dark.customenderchest.commands.EnderChestCommand;
import org.dark.customenderchest.listeners.EnderChestListener;
import org.dark.customenderchest.utilities.DatabaseHandler;

public class CustomEnderChest extends JavaPlugin {
    private DatabaseHandler databaseHandler;
    private EnderChestListener enderChestListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            databaseHandler = new DatabaseHandler(this);
            enderChestListener = new EnderChestListener(this, databaseHandler);

            // Register events
            getServer().getPluginManager().registerEvents(enderChestListener, this);

            // Register commands
            EnderChestCommand enderChestCommand = new EnderChestCommand(this, databaseHandler);
            getCommand("enderchest").setExecutor(enderChestCommand);

            AdminEnderChestCommand adminCommand = new AdminEnderChestCommand(this, databaseHandler);
            getCommand("achest").setExecutor(adminCommand);
            getCommand("achest").setTabCompleter(adminCommand);

            getLogger().info("CustomEnderChest has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable CustomEnderChest: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (databaseHandler != null) {
            databaseHandler.closeConnection();
        }
        getLogger().info("CustomEnderChest has been disabled!");
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }

    public EnderChestListener getEnderChestListener() {
        return enderChestListener;
    }

    // New method to get custom inventory title for each permission level
    public String getInventoryTitleForLines(int lines) {
        String customTitle = getConfig().getString("inventory-titles.Level " + lines, getConfig().getString("inventory-title", "Custom EnderChest"));
        return ChatColor.translateAlternateColorCodes('&', customTitle);
    }
}