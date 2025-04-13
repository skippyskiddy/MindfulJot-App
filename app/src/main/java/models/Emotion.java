package models;

/**
 * Model class representing an emotion
 */
public class Emotion {
    public enum Category {
        HIGH_ENERGY_PLEASANT,
        HIGH_ENERGY_UNPLEASANT,
        LOW_ENERGY_PLEASANT,
        LOW_ENERGY_UNPLEASANT
    }

    private String name;
    private Category category;
    private String definition;
    private int energyLevel; // TODO: 1-10 scale, helps for ordering within a category

    // Empty constructor required for Firebase
    public Emotion() {
    }

    public Emotion(String name, Category category, String definition, int energyLevel) {
        this.name = name;
        this.category = category;
        this.definition = definition;
        this.energyLevel = energyLevel;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
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