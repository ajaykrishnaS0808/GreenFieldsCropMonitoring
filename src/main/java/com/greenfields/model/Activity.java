package com.greenfields.model;

public record Activity(
        int id,
        int cropId,
        String cropName,
        String type,
        String details,
        String activityDate
) {}
