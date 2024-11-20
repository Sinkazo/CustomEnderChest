package org.dark.customenderchest.utilities;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.dark.customenderchest.CustomEnderChest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseHandler {
    private Connection connection;
    private final CustomEnderChest plugin;
    private final boolean useMySQLConfig;

    public DatabaseHandler(CustomEnderChest plugin) {
        this.plugin = plugin;
        this.useMySQLConfig = plugin.getConfig().getBoolean("database.mysql", false);
        initializeConnection();
    }

    private void initializeConnection() {
        try {
            if (useMySQLConfig) {
                connectMySQL();
            } else {
                connectSQLite();
            }
            createTable();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to connect to primary database. Falling back to SQLite.");
            connectSQLite();
            try {
                createTable();
            } catch (SQLException sqlEx) {
                plugin.getLogger().severe("Failed to create database table: " + sqlEx.getMessage());
            }
        }
    }

    private void connectMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "minecraft");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        connection = DriverManager.getConnection(url, username, password);
    }

    private void connectSQLite() {
        try {
            String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/enderchest.db";
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to connect to SQLite: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS inventories (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "inventory BLOB NOT NULL)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
        }
    }

    public synchronized void saveInventory(UUID uuid, ItemStack[] inventory) {
        if (uuid == null) return;

        String query = "INSERT OR REPLACE INTO inventories (uuid, inventory) VALUES (?, ?)";

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos);
             PreparedStatement stmt = connection.prepareStatement(query)) {

            oos.writeObject(inventory);
            byte[] serializedInventory = bos.toByteArray();

            stmt.setString(1, uuid.toString());
            stmt.setBytes(2, serializedInventory);
            stmt.executeUpdate();

        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save inventory for " + uuid, e);
        }
    }

    public synchronized ItemStack[] loadInventory(UUID uuid) {
        if (uuid == null) return new ItemStack[54];

        String query = "SELECT inventory FROM inventories WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] serializedInventory = rs.getBytes("inventory");

                    try (ByteArrayInputStream bis = new ByteArrayInputStream(serializedInventory);
                         BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {

                        return (ItemStack[]) ois.readObject();
                    }
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load inventory for " + uuid, e);
        }

        return new ItemStack[54];
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
        }
    }
}