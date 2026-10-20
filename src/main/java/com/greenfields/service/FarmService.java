package com.greenfields.service;

import com.greenfields.model.Activity;
import com.greenfields.model.Crop;
import com.greenfields.model.HarvestReport;
import com.greenfields.model.Reminder;
import com.greenfields.repository.FarmRepository;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class FarmService {
    private final FarmRepository repository;

    public FarmService(FarmRepository repository) {
        this.repository = repository;
    }

    public List<Crop> crops() throws SQLException { return repository.getCrops(); }
    public List<Activity> activities() throws SQLException { return repository.getActivities(); }
    public List<Reminder> reminders() throws SQLException { return repository.getReminders(); }
    public List<HarvestReport> reports() throws SQLException { return repository.getHarvestReports(); }
    public Map<String, Object> stats() throws SQLException { return repository.getStats(); }

    public Crop createCrop(Crop crop) throws SQLException {
        validateText(crop.name(), "Crop name");
        validateText(crop.variety(), "Variety");
        validateText(crop.season(), "Season");
        if (crop.areaAcres() <= 0) throw new IllegalArgumentException("Area must be greater than zero.");
        if (crop.expectedYieldTons() < 0) throw new IllegalArgumentException("Expected yield cannot be negative.");
        int id = repository.addCrop(crop);
        repository.addActivity(id, "Planting", "Crop record created for " + crop.name() + ".", LocalDate.now().toString());
        return repository.getCrop(id);
    }

    public void createActivity(Map<String, Object> body) throws SQLException {
        int cropId = integer(body, "cropId");
        String type = required(body, "type");
        String details = required(body, "details");
        String date = optional(body, "activityDate", LocalDate.now().toString());
        if (repository.getCrop(cropId) == null) throw new IllegalArgumentException("Selected crop does not exist.");
        repository.addActivity(cropId, type, details, date);
    }

    public void createReminder(Map<String, Object> body) throws SQLException {
        int cropId = integer(body, "cropId");
        String type = required(body, "type");
        String date = required(body, "dueDate");
        String notes = required(body, "notes");
        if (repository.getCrop(cropId) == null) throw new IllegalArgumentException("Selected crop does not exist.");
        repository.addReminder(cropId, type, date, notes);
    }

    public void completeReminder(int id) throws SQLException { repository.completeReminder(id); }
    public boolean deleteCrop(int id) throws SQLException { return repository.deleteCrop(id); }
    public boolean deleteReminder(int id) throws SQLException { return repository.deleteReminder(id); }
    public boolean deleteActivity(int id) throws SQLException { return repository.deleteActivity(id); }

    public void createHarvest(Map<String, Object> body) throws SQLException {
        int cropId = integer(body, "cropId");
        String season = required(body, "season");
        String date = required(body, "harvestDate");
        double yield = Double.parseDouble(String.valueOf(body.get("yieldTons")));
        if (yield < 0) throw new IllegalArgumentException("Yield cannot be negative.");
        if (repository.getCrop(cropId) == null) throw new IllegalArgumentException("Selected crop does not exist.");
        repository.addHarvest(cropId, season, date, yield);
    }

    private void validateText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
    }

    private String required(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) throw new IllegalArgumentException(key + " is required.");
        return String.valueOf(value).trim();
    }

    private String optional(Map<String, Object> body, String key, String fallback) {
        Object value = body.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private int integer(Map<String, Object> body, String key) {
        return Integer.parseInt(required(body, key));
    }
}
