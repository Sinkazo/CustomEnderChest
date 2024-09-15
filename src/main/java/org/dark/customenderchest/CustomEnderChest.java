package org.dark.customenderchest;

import org.bukkit.plugin.java.JavaPlugin;
import org.dark.customenderchest.commands.EnderChestCommand;
import org.dark.customenderchest.listeners.EnderChestListener;
import org.dark.customenderchest.utilities.DatabaseHandler;

public class CustomEnderChest extends JavaPlugin {

    private DatabaseHandler databaseHandler;

    @Override
    public void onEnable() {
        // Cargar config.yml
        saveDefaultConfig();

        // Inicializar base de datos
        databaseHandler = new DatabaseHandler(this);

        // Registrar eventos
        getServer().getPluginManager().registerEvents(new EnderChestListener(this, databaseHandler), this);

        // Registrar el comando y su ejecutor
        getCommand("enderchest").setExecutor(new EnderChestCommand(this, databaseHandler));
    }

    @Override
    public void onDisable() {
        // Cerrar la base de datos al apagar el servidor
        databaseHandler.closeConnection();
    }

    public DatabaseHandler getDatabaseHandler() {
        return databaseHandler;
    }
}