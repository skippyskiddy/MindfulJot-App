package models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.io.Serializable;

import db.converters.EmotionConverters;

/**
 * Model class representing an emotion
 */
@Entity(tableName = "emotions")
public class Emotion implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Category {
        HIGH_ENERGY_PLEASANT,
        HIGH_ENERGY_UNPLEASANT,
        LOW_ENERGY_PLEASANT,
        LOW_ENERGY_UNPLEASANT
    }

    @PrimaryKey
    @NonNull
    private String name;
    @TypeConverters(EmotionConverters.class)
    private Category category;
    private String definition;
    private int energyLevel; // TODO: 1-10 scale, helps for ordering within a category

    // Empty constructor required for Firebase and Room
    public Emotion() {
    }

    public Emotion(@NonNull String name, Category category, String definition, int energyLevel) {
        this.name = name;
        this.category = category;
        this.definition = definition;
        this.energyLevel = energyLevel;
    }

    // Getters and setters
    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public int getEnergyLevel() {
        return energyLevel;
    }

    public void setEnergyLevel(int energyLevel) {
        this.energyLevel = energyLevel;
    }
}