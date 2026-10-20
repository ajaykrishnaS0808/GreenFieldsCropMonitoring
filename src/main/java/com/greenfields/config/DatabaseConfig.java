package com.greenfields.config;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DatabaseConfig {
    private final String jdbcUrl;

    public DatabaseConfig() {
        try {
            Path databaseFolder = Path.of("database");
            Files.createDirectories(databaseFolder);
            this.jdbcUrl = "jdbc:sqlite:" + databaseFolder.resolve("farm.db").toAbsolutePath();
            Class.forName("org.sqlite.JDBC");
            initializeSchema();
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize SQLite database.", e);
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void initializeSchema() throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("""
                CREATE TABLE IF NOT EXISTS crops (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    variety TEXT NOT NULL,
                    area_acres REAL NOT NULL CHECK(area_acres > 0),
                    season TEXT NOT NULL,
                    planted_on TEXT NOT NULL,
                    expected_harvest TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'Growing',
                    expected_yield_tons REAL NOT NULL DEFAULT 0
                )
            """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS activities (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    crop_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    details TEXT NOT NULL,
                    activity_date TEXT NOT NULL,
                    FOREIGN KEY(crop_id) REFERENCES crops(id) ON DELETE CASCADE
                )
            """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    crop_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    due_date TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    completed INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(crop_id) REFERENCES crops(id) ON DELETE CASCADE
                )
            """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS harvests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    crop_id INTEGER NOT NULL,
                    season TEXT NOT NULL,
                    harvest_date TEXT NOT NULL,
                    yield_tons REAL NOT NULL CHECK(yield_tons >= 0),
                    FOREIGN KEY(crop_id) REFERENCES crops(id) ON DELETE CASCADE
                )
            """);
            seedIfEmpty(connection);
        }
    }

    private void seedIfEmpty(Connection connection) throws SQLException {
        try (Statement check = connection.createStatement();
             var result = check.executeQuery("SELECT COUNT(*) FROM crops")) {
            if (result.next() && result.getInt(1) > 0) return;
        }

        try (var crop = connection.prepareStatement("""
                INSERT INTO crops(name,variety,area_acres,season,planted_on,expected_harvest,status,expected_yield_tons)
                VALUES(?,?,?,?,?,?,?,?)
                """)) {
            Object[][] seedCrops = {
                {"Paddy", "IR-64", 12.0, "Kharif 2026", "2026-06-18", "2026-10-25", "Growing", 30.0},
                {"Maize", "Hybrid 900M", 8.5, "Kharif 2026", "2026-06-25", "2026-10-15", "Growing", 25.5},
                {"Banana", "Grand Naine", 5.0, "Perennial 2026", "2026-02-10", "2026-11-20", "Growing", 42.0},
                {"Paddy", "Sona Masuri", 10.0, "Rabi 2025", "2025-11-12", "2026-03-20", "Harvested", 24.0}
            };
            for (Object[] row : seedCrops) {
                for (int i = 0; i < row.length; i++) crop.setObject(i + 1, row[i]);
                crop.executeUpdate();
            }
        }

        try (var activity = connection.prepareStatement("""
                INSERT INTO activities(crop_id,type,details,activity_date) VALUES(?,?,?,?)
                """)) {
            Object[][] rows = {
                {1, "Irrigation", "Field irrigation completed; soil moisture checked.", "2026-10-06"},
                {2, "Fertilizer", "Applied balanced NPK fertilizer according to field plan.", "2026-10-05"},
                {3, "Inspection", "Checked leaf colour and drainage around banana plants.", "2026-10-04"},
                {4, "Harvest", "Recorded previous season paddy harvest.", "2026-03-20"}
            };
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) activity.setObject(i + 1, row[i]);
                activity.executeUpdate();
            }
        }

        try (var reminder = connection.prepareStatement("""
                INSERT INTO reminders(crop_id,type,due_date,notes,completed) VALUES(?,?,?,?,0)
                """)) {
            Object[][] rows = {
                {1, "Irrigation", "2026-10-10", "Check field water level before the next irrigation."},
                {2, "Harvest", "2026-10-15", "Inspect maize maturity and prepare harvesting equipment."},
                {3, "Inspection", "2026-10-12", "Inspect banana bunch development and plant health."}
            };
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) reminder.setObject(i + 1, row[i]);
                reminder.executeUpdate();
            }
        }

        try (var harvest = connection.prepareStatement("""
                INSERT INTO harvests(crop_id,season,harvest_date,yield_tons) VALUES(?,?,?,?)
                """)) {
            Object[][] rows = {
                {4, "Rabi 2025", "2026-03-20", 23.4},
                {1, "Kharif 2025", "2025-10-28", 27.2},
                {2, "Kharif 2025", "2025-10-17", 22.1}
            };
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) harvest.setObject(i + 1, row[i]);
                harvest.executeUpdate();
            }
        }
    }
}
