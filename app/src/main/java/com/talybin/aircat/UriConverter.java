package com.talybin.aircat;

import android.net.Uri;

import androidx.room.TypeConverter;

public class UriConverter {

    @TypeConverter
    public static Uri toUri(String str) {
        return Uri.parse(str);
    }

    @TypeConverter
    public static String toString(Uri uri) {
        return uri.toString();
    }
}
