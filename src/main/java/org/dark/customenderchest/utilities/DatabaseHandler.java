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

public class DatabaseHandler {

    private final Connection connection;

    public DatabaseHandler(CustomEnderChest plugin) {
        // Conectar a la base de datos SQLite
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/enderchest.db";
        try {
            connection = DriverManager.getConnection(url);
            createTable();
        } catch (SQLException e) {
            throw new RuntimeException("Error al conectar con la base de datos SQLite", e);
        }
    }

    private void createTable() {
        // Crear la tabla de inventarios
        String query = "CREATE TABLE IF NOT EXISTS inventories (" +
                "uuid TEXT PRIMARY KEY, " +
                "inventory BLOB)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveInventory(UUID uuid, ItemStack[] inventory) {
        // Serializar el inventario a un array de bytes
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {

            oos.writeObject(inventory);
            oos.flush();
            byte[] inventoryData = bos.toByteArray();

            // Insertar o actualizar el inventario en la base de datos
            String query = "REPLACE INTO inventories (uuid, inventory) VALUES (?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setBytes(2, inventoryData);
                stmt.executeUpdate();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public ItemStack[] loadInventory(UUID uuid) {
        // Cargar el inventario desde la base de datos
        String query = "SELECT inventory FROM inventories WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                byte[] inventoryData = rs.getBytes("inventory");
                try (ByteArrayInputStream bis = new ByteArrayInputStream(inventoryData);
                     BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {

                    return (ItemStack[]) ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ItemStack[0]; // Inventario vacío si no se encuentra nada
    }

    public void closeConnection() {
        // Cerrar la conexión a la base de datos
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}