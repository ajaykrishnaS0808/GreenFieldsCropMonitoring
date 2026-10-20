package com.greenfields.repository;

import com.greenfields.config.DatabaseConfig;
import com.greenfields.model.Activity;
import com.greenfields.model.Crop;
import com.greenfields.model.HarvestReport;
import com.greenfields.model.Reminder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FarmRepository {
    private final DatabaseConfig database;

    public FarmRepository(DatabaseConfig database) {
        this.database = database;
    }

    public List<Crop> getCrops() throws SQLException {
        List<Crop> crops = new ArrayList<>();
        String sql = "SELECT * FROM crops ORDER BY id DESC";
        try (Connection c = database.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) crops.add(mapCrop(r));
        }
        return crops;
    }

    public Crop getCrop(int id) throws SQLException {
        try (Connection c = database.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM crops WHERE id=?")) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? mapCrop(r) : null;
            }
        }
    }

    public int addCrop(Crop crop) throws SQLException {
        String sql = """
            INSERT INTO crops(name,variety,area_acres,season,planted_on,expected_harvest,status,expected_yield_tons)
            VALUES(?,?,?,?,?,?,?,?)
            """;
        try (Connection c = database.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, crop.name());
            p.setString(2, crop.variety());
            p.setDouble(3, crop.areaAcres());
            p.setString(4, crop.season());
            p.setString(5, crop.plantedOn());
            p.setString(6, crop.expectedHarvest());
            p.setString(7, crop.status() == null || crop.status().isBlank() ? "Growing" : crop.status());
            p.setDouble(8, crop.expectedYieldTons());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public List<Activity> getActivities() throws SQLException {
        List<Activity> activities = new ArrayList<>();
        String sql = """
            SELECT a.id,a.crop_id,c.name AS crop_name,a.type,a.details,a.activity_date
            FROM activities a JOIN crops c ON c.id=a.crop_id
            ORDER BY a.activity_date DESC,a.id DESC LIMIT 100
            """;
        try (Connection c = database.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) activities.add(new Activity(r.getInt("id"), r.getInt("crop_id"),
                    r.getString("crop_name"), r.getString("type"), r.getString("details"), r.getString("activity_date")));
        }
        return activities;
    }

    public void addActivity(int cropId, String type, String details, String date) throws SQLException {
        try (Connection c = database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO activities(crop_id,type,details,activity_date) VALUES(?,?,?,?)")) {
            p.setInt(1, cropId); p.setString(2, type); p.setString(3, details); p.setString(4, date);
            p.executeUpdate();
        }
    }

    public List<Reminder> getReminders() throws SQLException {
        List<Reminder> list = new ArrayList<>();
        String sql = """
            SELECT r.id,r.crop_id,c.name AS crop_name,r.type,r.due_date,r.notes,r.completed
            FROM reminders r JOIN crops c ON c.id=r.crop_id
            ORDER BY r.completed ASC,r.due_date ASC
            """;
        try (Connection c = database.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) list.add(new Reminder(r.getInt("id"), r.getInt("crop_id"), r.getString("crop_name"),
                    r.getString("type"), r.getString("due_date"), r.getString("notes"), r.getInt("completed") == 1));
        }
        return list;
    }

    public void addReminder(int cropId, String type, String dueDate, String notes) throws SQLException {
        try (Connection c = database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO reminders(crop_id,type,due_date,notes,completed) VALUES(?,?,?,?,0)")) {
            p.setInt(1, cropId); p.setString(2, type); p.setString(3, dueDate); p.setString(4, notes);
            p.executeUpdate();
        }
    }

    public void completeReminder(int id) throws SQLException {
        try (Connection c = database.getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE reminders SET completed=1 WHERE id=?")) {
            p.setInt(1, id); p.executeUpdate();
        }
    }


    public boolean deleteCrop(int id) throws SQLException {
        try (Connection c = database.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Explicit child cleanup keeps this safe even if SQLite foreign keys are not enabled.
                for (String table : List.of("activities", "reminders", "harvests")) {
                    try (PreparedStatement p = c.prepareStatement("DELETE FROM " + table + " WHERE crop_id=?")) {
                        p.setInt(1, id); p.executeUpdate();
                    }
                }
                int changed;
                try (PreparedStatement p = c.prepareStatement("DELETE FROM crops WHERE id=?")) {
                    p.setInt(1, id); changed = p.executeUpdate();
                }
                c.commit();
                return changed > 0;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public boolean deleteReminder(int id) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM reminders WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        }
    }

    public boolean deleteActivity(int id) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM activities WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        }
    }

    public Map<String, Object> getStats() throws SQLException {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection c = database.getConnection(); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("""
                    SELECT COUNT(*) crop_count,
                           COALESCE(SUM(area_acres),0) total_acres,
                           COALESCE(SUM(CASE WHEN status='Growing' THEN 1 ELSE 0 END),0) active_crops,
                           COALESCE(SUM(expected_yield_tons),0) expected_yield
                    FROM crops
                    """)) {
                if (r.next()) {
                    stats.put("cropCount", r.getInt("crop_count"));
                    stats.put("totalAcres", r.getDouble("total_acres"));
                    stats.put("activeCrops", r.getInt("active_crops"));
                    stats.put("expectedYield", r.getDouble("expected_yield"));
                }
            }
            try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM reminders WHERE completed=0 AND due_date <= date('now','+7 days')")) {
                stats.put("upcomingReminders", r.next() ? r.getInt(1) : 0);
            }
            try (ResultSet r = s.executeQuery("""
                    SELECT c.name, SUM(h.yield_tons) total_yield
                    FROM harvests h JOIN crops c ON c.id=h.crop_id
                    GROUP BY c.name ORDER BY total_yield DESC
                    """)) {
                List<Map<String, Object>> labels = new ArrayList<>();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("crop", r.getString("name"));
                    row.put("yieldTons", r.getDouble("total_yield"));
                    labels.add(row);
                }
                stats.put("harvestByCrop", labels);
            }
        }
        return stats;
    }

    public List<HarvestReport> getHarvestReports() throws SQLException {
        List<HarvestReport> list = new ArrayList<>();
        String sql = """
            SELECT h.id,h.crop_id,c.name crop_name,h.season,h.harvest_date,h.yield_tons,c.area_acres,
                   CASE WHEN c.area_acres > 0 THEN h.yield_tons/c.area_acres ELSE 0 END yield_per_acre
            FROM harvests h JOIN crops c ON c.id=h.crop_id
            ORDER BY h.harvest_date DESC
            """;
        try (Connection c = database.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) list.add(new HarvestReport(r.getInt("id"), r.getInt("crop_id"), r.getString("crop_name"),
                    r.getString("season"), r.getString("harvest_date"), r.getDouble("yield_tons"),
                    r.getDouble("area_acres"), r.getDouble("yield_per_acre")));
        }
        return list;
    }

    public void addHarvest(int cropId, String season, String date, double yieldTons) throws SQLException {
        try (Connection c = database.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO harvests(crop_id,season,harvest_date,yield_tons) VALUES(?,?,?,?)")) {
                p.setInt(1, cropId); p.setString(2, season); p.setString(3, date); p.setDouble(4, yieldTons);
                p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement("UPDATE crops SET status='Harvested' WHERE id=?")) {
                p.setInt(1, cropId); p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO activities(crop_id,type,details,activity_date) VALUES(?,?,?,?)")) {
                p.setInt(1, cropId); p.setString(2, "Harvest");
                p.setString(3, "Harvest recorded: " + yieldTons + " tons for " + season + ".");
                p.setString(4, date); p.executeUpdate();
            }
            c.commit();
        }
    }

    private Crop mapCrop(ResultSet r) throws SQLException {
        return new Crop(r.getInt("id"), r.getString("name"), r.getString("variety"), r.getDouble("area_acres"),
                r.getString("season"), r.getString("planted_on"), r.getString("expected_harvest"),
                r.getString("status"), r.getDouble("expected_yield_tons"));
    }
}
