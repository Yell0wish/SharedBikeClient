package com.example.sharedbikeclient.ui.charity;

public class Charity {
    private String name;
    private String gender;
    private String birthdate;
    private String height;
    private String missingDate;
    private String missingLocation;
    private String description;
    private String imageUrl;

    public Charity(String name, String gender, String birthdate, String height, String missingDate, String missingLocation, String description, String imageUrl) {
        this.name = name;
        this.gender = gender;
        this.birthdate = birthdate;
        this.height = height;
        this.missingDate = missingDate;
        this.missingLocation = missingLocation;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public String getHeight() {
        return height;
    }

    public String getMissingDate() {
        return missingDate;
    }

    public String getMissingLocation() {
        return missingLocation;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
