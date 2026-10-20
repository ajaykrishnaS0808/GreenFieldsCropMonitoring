package com.greenfields.model;

public record Crop(
        int id,
        String name,
        String variety,
        double areaAcres,
        String season,
        String plantedOn,
        String expectedHarvest,
        String status,
        double expectedYieldTons
) {}
