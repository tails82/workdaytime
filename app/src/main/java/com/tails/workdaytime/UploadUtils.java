package com.tails.workdaytime;

import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;

public class UploadUtils {

    public static final int CAMERA_REQUEST_CODE = 2;
    public static final int ALBUM_REQUEST_CODE = 3;

    private AppCompatActivity activity;
    private CommonListeners commonListeners;

    public UploadUtils(AppCompatActivity activity, CommonListeners commonListeners) {
        this.activity = activity;
        this.commonListeners = commonListeners;
    }

    public void appendPictureForDate(Date date, String path) {
        Cursor c = CommonUtils.getRecordForDate(activity, date);
        c.moveToFirst();
        int imgPathsIndex = c.getColumnIndex("imgPaths");
        String[] imgPathsArray = CommonUtils.convertStringToArray(c.getString(imgPathsIndex));
        String[] updatedImgPathsArray = CommonUtils.appendStringArray(imgPathsArray, path);
        String updatedImgPaths = CommonUtils.convertArrayToString(updatedImgPathsArray);
        updateTodayImgPaths(updatedImgPaths);
    }

    private void updateTodayImgPaths(String imgPaths) {
        Cursor c = CommonUtils.getRecordForDate(activity, commonListeners.getUploadDate());
        c.moveToFirst();
        int idIndex = c.getColumnIndex("id");
        Long id = c.getLong(idIndex);

        String sql = "UPDATE workdaytimes SET imgPaths = ? where id = ?";
        DbConnection.getDbConnection(activity).execSQL(sql, new Object[] {imgPaths, id});
    }

}
