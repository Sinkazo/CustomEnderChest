package org.dark.customenderchest.utilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.dark.customenderchest.CustomEnderChest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseHandler {
    private final Connection connection;
    private final CustomEnderChest plugin;

    public DatabaseHandler(CustomEnderChest plugin) {
        this.plugin = plugin;
        this.connection = initializeConnection();
        createTables();
    }

    private Connection initializeConnection() {
        try {
            if (plugin.getConfig().getBoolean("database.mysql", false)) {
                return connectMySQL();
            } else {
                return connectSQLite();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database connection: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
    }

    private Connection connectMySQL() throws SQLException {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "minecraft");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        return DriverManager.getConnection(url, username, password);
    }

    private Connection connectSQLite() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/enderchest.db";
            return DriverManager.getConnection(url);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS inventories (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16), " +
                    "inventory BLOB, " +
                    "migrated BOOLEAN DEFAULT 0, " +
                    "last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");


            stmt.execute("CREATE TABLE IF NOT EXISTS last_access (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "last_access TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database tables", e);
        }
    }

    private void migrateLegacyEnderChest(UUID uuid) {
        if (uuid == null) return;

        String migrationCheckQuery = "SELECT migrated FROM inventories WHERE uuid = ?";
        String updateMigrationQuery = "UPDATE inventories SET migrated = 1 WHERE uuid = ?";
        String insertMigrationQuery = "INSERT INTO inventories (uuid, player_name, inventory, migrated, last_modified) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP)";

        try {
            // First, check if migration is needed
            boolean needsMigration = true;
            try (PreparedStatement checkStmt = connection.prepareStatement(migrationCheckQuery)) {
                checkStmt.setString(1, uuid.toString());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getBoolean("migrated")) {
                        return; // Already migrated, do nothing
                    }
                }
            }

            // Get the player's current Ender Chest contents
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;

            // Check if the player already has data in the database
            boolean hasExistingData = false;
            try (PreparedStatement checkDataStmt = connection.prepareStatement("SELECT 1 FROM inventories WHERE uuid = ?")) {
                checkDataStmt.setString(1, uuid.toString());
                try (ResultSet rs = checkDataStmt.executeQuery()) {
                    hasExistingData = rs.next();
                }
            }

            ItemStack[] legacyContents = player.getEnderChest().getContents();

            // If no legacy contents, just mark as migrated if there's existing data
            if (legacyContents == null || legacyContents.length == 0) {
                if (hasExistingData) {
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateMigrationQuery)) {
                        updateStmt.setString(1, uuid.toString());
                        updateStmt.executeUpdate();
                    }
                }
                return;
            }

            // Serialize legacy contents
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {

                oos.writeObject(legacyContents);
                byte[] serializedInventory = bos.toByteArray();

                if (hasExistingData) {
                    // Just update the migration flag if data exists
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateMigrationQuery)) {
                        updateStmt.setString(1, uuid.toString());
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Insert new data with migrated flag
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertMigrationQuery)) {
                        insertStmt.setString(1, uuid.toString());
                        insertStmt.setString(2, player.getName());
                        insertStmt.setBytes(3, serializedInventory);
                        insertStmt.executeUpdate();
                    }
                }

                // Clear the vanilla enderchest after successful migration
                player.getEnderChest().clear();
            }
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to migrate legacy EnderChest for " + uuid, e);
        }
    }

    public synchronized void saveInventory(UUID uuid, ItemStack[] inventory) {
        if (uuid == null) return;

        String query = "INSERT OR REPLACE INTO inventories (uuid, inventory, last_modified) VALUES (?, ?, CURRENT_TIMESTAMP)";

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {

            oos.writeObject(inventory);
            byte[] serializedInventory = bos.toByteArray();

            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setBytes(2, serializedInventory);
                stmt.executeUpdate();
            }

            updateLastAccess(uuid);
        } catch (SQLException | IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save inventory for " + uuid, e);
            throw new RuntimeException("Failed to save inventory", e);
        }
    }

    public synchronized ItemStack[] loadInventory(UUID uuid) {
        if (uuid == null) return new ItemStack[54];

        // Check for migration if enabled in config
        if (plugin.getConfig().getBoolean("database.migrate-legacy-enderchest", false)) {
            migrateLegacyEnderChest(uuid);
        }

        String query = "SELECT inventory FROM inventories WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] serializedInventory = rs.getBytes("inventory");
                    updateLastAccess(uuid);

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

    public synchronized void deleteInventory(UUID uuid) {
        if (uuid == null) return;

        String query = "DELETE FROM inventories WHERE uuid = ?";
        String lastAccessQuery = "DELETE FROM last_access WHERE uuid = ?";

        try (PreparedStatement stmt = connection.prepareStatement(query);
             PreparedStatement lastAccessStmt = connection.prepareStatement(lastAccessQuery)) {

            connection.setAutoCommit(false);
            try {
                stmt.setString(1, uuid.toString());
                lastAccessStmt.setString(1, uuid.toString());

                stmt.executeUpdate();
                lastAccessStmt.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete inventory for " + uuid, e);
            throw new RuntimeException("Failed to delete inventory", e);
        }
    }

    private void updateLastAccess(UUID uuid) {
        String query = "INSERT OR REPLACE INTO last_access (uuid, last_access) VALUES (?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update last access for " + uuid, e);
        }
    }

    public UUID hasInventory(String playerName) {
        String query = "SELECT uuid FROM inventories WHERE player_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check inventory existence for " + playerName, e);
        }
        return null;
    }

    public Set<String> getStoredPlayerNames() {
        Set<String> playerNames = new HashSet<>();
        String query = "SELECT player_name FROM inventories WHERE player_name IS NOT NULL";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String playerName = rs.getString("player_name");
                if (playerName != null && !playerName.isEmpty()) {
                    playerNames.add(playerName);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get stored player names", e);
        }

        return playerNames;
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