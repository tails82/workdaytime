package com.tails.workdaytime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {

    public static String formatDateTime(String time, String originalFormat, String targetFormat) {
        SimpleDateFormat sdf = new SimpleDateFormat(originalFormat);
        Date dateTime = null;
        try {
            dateTime = sdf.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        sdf = new SimpleDateFormat(targetFormat);

        return sdf.format(dateTime);
    }

    public static double getHourInterval(long fromDateTime, long toDateTime) {
        double diff = (double) (toDateTime - fromDateTime) / (1000 * 60 * 60);

        return (double)Math.round(diff*100)/100;
    }

    public static double getHourInterval(String dateTimeFormat, String fromDateTime, String toDateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat);
        long longFromDateTime = 0;
        long longToDateTime = 0;

        try {
            longFromDateTime = sdf.parse(fromDateTime).getTime();
            longToDateTime = sdf.parse(toDateTime).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return getHourInterval(longFromDateTime, longToDateTime);
    }

    public static long convertStringDateTimeToLong(String stringDateTime, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date dateTime = null;
        try {
            dateTime = sdf.parse(stringDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dateTime.getTime();
    }

    public static String generateDateString(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    public static Date convertStringToDateTime(String dateTimeString, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date dateTime = null;
        try {
            dateTime = sdf.parse(dateTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dateTime;
    }

    public static String convertLongToString(long dateLong, String format) {
        Date date = new Date(dateLong);
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        return sdf.format(date);
    }
}
