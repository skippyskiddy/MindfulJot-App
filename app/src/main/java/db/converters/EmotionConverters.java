package db.converters;

import androidx.room.TypeConverter;

import models.Emotion;

public class EmotionConverters {

    @TypeConverter
    public static String fromCategory(Emotion.Category category) {
        return category.name();
    }

    @TypeConverter
    public static Emotion.Category toCategory(String categoryName) {
        return Emotion.Category.valueOf(categoryName);
    }
}
