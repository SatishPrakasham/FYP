package com.example.smartdiet;

public class Recipe {
    private String dishName;
    private String proteinType;
    private String description;
    private String link;
    private String imageUrl;

    public Recipe() {
        // No-arg constructor required for Firestore deserialization
    }

    public Recipe(String dishName, String proteinType, String description, String link, String imageUrl) {
        this.dishName = dishName;
        this.proteinType = proteinType;
        this.description = description;
        this.link = link;
        this.imageUrl = imageUrl;
    }

    public String getDishName() {
        return dishName;
    }

    public String getProteinType() {
        return proteinType;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
