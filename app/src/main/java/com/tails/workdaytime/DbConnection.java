package com.tails.workdaytime;

import android.database.sqlite.SQLiteDatabase;

import androidx.appcompat.app.AppCompatActivity;

public class DbConnection {

    private final static String DATA_BASE_NAME = "WorkdayTime";

    private static SQLiteDatabase myDataBase;

    public static void initialDataBase(AppCompatActivity activity) {
        if (myDataBase == null) {
            myDataBase = activity.openOrCreateDatabase(DATA_BASE_NAME, activity.MODE_PRIVATE, null);
        }

        myDataBase.execSQL("CREATE TABLE IF NOT EXISTS workdaytimes (id INTEGER PRIMARY KEY, arriveTime INTEGER , leaveTime INTEGER, imgPaths TEXT)");
    }

    public static SQLiteDatabase getDbConnection(AppCompatActivity activity) {
        if (myDataBase == null) {
            initialDataBase(activity);
        }

        return myDataBase;
    }

    public static void dropDataBase(AppCompatActivity activity) {
        activity.deleteDatabase(DATA_BASE_NAME);
        myDataBase = null;

        myDataBase = getDbConnection(activity);
    }

}
