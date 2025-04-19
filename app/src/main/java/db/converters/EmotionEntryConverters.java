package db.converters;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

import models.Emotion;

/**
 * Type converters for EmotionEntry fields: List<Emotion>, List<String>, and Date.
 */
public class EmotionEntryConverters {
    private static final Gson gson = new Gson();

    // Converting between Date & Long

    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date toDate(Long millis) {
        return millis == null ? null : new Date(millis);
    }

    // Converting between List<String> & JSON

    @TypeConverter
    public static String fromStringList(List<String> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<String> toStringList(String json) {
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // Converting between List<Emotion> & JSON

    @TypeConverter
    public static String fromEmotionList(List<Emotion> emotions) {
        return gson.toJson(emotions);
    }

    @TypeConverter
    public static List<Emotion> toEmotionList(String json) {
        Type type = new TypeToken<List<Emotion>>() {}.getType();
        return gson.fromJson(json, type);
    }
}
