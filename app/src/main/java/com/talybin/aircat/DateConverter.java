package com.talybin.aircat;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import java.util.Date;

class DateConverter {

    @TypeConverter
    public static Date toDate(long dateLong){
        return new Date(dateLong);
    }

    @TypeConverter
    public static long fromDate(@NonNull Date date){
        return date.getTime();
    }
}
