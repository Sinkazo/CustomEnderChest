package org.dark.customenderchest;

import org.bukkit.plugin.java.JavaPlugin;
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
}