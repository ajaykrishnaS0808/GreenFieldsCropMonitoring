package com.greenfields.model;

public record Reminder(
        int id,
        int cropId,
        String cropName,
        String type,
        String dueDate,
        String notes,
        boolean completed
) {}
