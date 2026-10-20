package com.greenfields.controller;

import com.greenfields.model.Crop;
import com.greenfields.service.FarmService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FarmController {
    private final FarmService service;

    public FarmController(FarmService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "application", "GreenFields Crop Monitoring");
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() throws SQLException { return service.stats(); }

    @GetMapping("/crops")
    public List<Crop> crops() throws SQLException { return service.crops(); }

    @PostMapping("/crops")
    public ResponseEntity<?> createCrop(@RequestBody Crop crop) throws SQLException {
        try { return ResponseEntity.ok(service.createCrop(crop)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/crops/{id}")
    public ResponseEntity<?> deleteCrop(@PathVariable int id) throws SQLException {
        if (!service.deleteCrop(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activities")
    public Object activities() throws SQLException { return service.activities(); }

    @PostMapping("/activities")
    public ResponseEntity<?> createActivity(@RequestBody Map<String, Object> body) throws SQLException {
        try { service.createActivity(body); return ResponseEntity.ok(Map.of("message", "Activity saved.")); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/activities/{id}")
    public ResponseEntity<?> deleteActivity(@PathVariable int id) throws SQLException {
        if (!service.deleteActivity(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reminders")
    public Object reminders() throws SQLException { return service.reminders(); }

    @PostMapping("/reminders")
    public ResponseEntity<?> createReminder(@RequestBody Map<String, Object> body) throws SQLException {
        try { service.createReminder(body); return ResponseEntity.ok(Map.of("message", "Reminder saved.")); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/reminders/{id}")
    public ResponseEntity<?> deleteReminder(@PathVariable int id) throws SQLException {
        if (!service.deleteReminder(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reminders/{id}/complete")
    public Map<String, String> completeReminder(@PathVariable int id) throws SQLException {
        service.completeReminder(id);
        return Map.of("message", "Reminder marked complete.");
    }

    @GetMapping("/reports")
    public Object reports() throws SQLException { return service.reports(); }

    @PostMapping("/harvests")
    public ResponseEntity<?> createHarvest(@RequestBody Map<String, Object> body) throws SQLException {
        try { service.createHarvest(body); return ResponseEntity.ok(Map.of("message", "Harvest saved.")); }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() == null ? "Invalid harvest data." : e.getMessage()));
        }
    }

    @GetMapping(value = "/reports.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv() throws SQLException {
        StringBuilder csv = new StringBuilder("Harvest ID,Crop,Season,Harvest Date,Yield (tons),Area (acres),Yield per acre (tons)\n");
        for (var row : service.reports()) {
            csv.append(row.id()).append(',')
               .append(csvCell(row.cropName())).append(',')
               .append(csvCell(row.season())).append(',')
               .append(csvCell(row.harvestDate())).append(',')
               .append(row.yieldTons()).append(',')
               .append(row.areaAcres()).append(',')
               .append(row.yieldPerAcre()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"greenfields-harvest-report.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString());
    }

    private String csvCell(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
