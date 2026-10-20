package com.greenfields.model;

public record HarvestReport(
        int id,
        int cropId,
        String cropName,
        String season,
        String harvestDate,
        double yieldTons,
        double areaAcres,
        double yieldPerAcre
) {}
