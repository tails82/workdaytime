package com.tails.workdaytime;

import android.database.Cursor;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CommonUtils {

    public static final String imageFolderName = "workdaytime";

    public static String strSeparator = "__,__";

    public static String convertArrayToString(String[] array){
        String str = "";
        for (int i = 0;i<array.length; i++) {
            str = str+array[i];
            // Do not append comma at the end of last element
            if(i<array.length-1){
                str = str+strSeparator;
            }
        }
        return str;
    }

    public static String[] convertStringToArray(String str){
        if (str == null || str.isEmpty()) {
            return new String [] {};
        }

        String[] arr = str.split(strSeparator);
        return arr;
    }

    public static String[] appendStringArray(String[] originalArray, String newItem)
    {
        int currentSize = originalArray.length;
        int newSize = currentSize + 1;
        String[] tempArray = new String[ newSize ];

        for (int i=0; i < currentSize; i++)
        {
            tempArray[i] = originalArray [i];
        }

        tempArray[newSize- 1] = newItem;

        return tempArray;
    }

    public static File generateOutputImageFileFolderForDate(Date date) {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + imageFolderName + "/" + DateTimeUtils.generateDateString(date, "yyyyMMdd"));
    }

    public static File generateOutputImageFileForDate(Date date, String fileName) {
        File dir = generateOutputImageFileFolderForDate(date);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        File mediaFile = new File(dir, fileName);

        return mediaFile;
    }

    public static File generateOutputImageFileForDate(Date date) {
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String timeStamp = sdf.format(currentDate);

        return generateOutputImageFileForDate(date, timeStamp + ".jpg");
    }

    public static boolean hasRecordForDate(AppCompatActivity activity, Date date) {

        return getRecordForDate(activity, date).getCount() > 0;
    }

    public static Cursor getRecordForDate(AppCompatActivity activity, Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        cal.set(Calendar.HOUR_OF_DAY, 00);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Long dayStartTime = cal.getTime().getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        Long dayEndTime = cal.getTime().getTime();

        String sql = "SELECT * FROM workdaytimes WHERE (arriveTime > ? and arriveTime < ?) or (leaveTime > ? and leaveTime < ?)";

        return DbConnection.getDbConnection(activity).rawQuery(sql , new String[] {String.valueOf(dayStartTime), String.valueOf(dayEndTime), String.valueOf(dayStartTime), String.valueOf(dayEndTime)});
    }

}
