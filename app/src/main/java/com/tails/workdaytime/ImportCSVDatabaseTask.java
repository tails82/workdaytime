package com.tails.workdaytime;

import android.app.ProgressDialog;
import android.content.Context;

import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ImportCSVDatabaseTask extends AsyncTask<String, Void, Boolean> {

    private Context context;
    private String selectedFileRealPath;

    public ImportCSVDatabaseTask(Context context, String selectedFileRealPath) {
        super();

        this.context = context;
        this.selectedFileRealPath = selectedFileRealPath;
    }

    private ProgressDialog dialog;

    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        this.dialog.setMessage("正在导入数据...");
        this.dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            CsvReader csvReader = new CsvReader(new FileReader(new File(selectedFileRealPath)));
            csvReader.readHeaders();

            DbConnection.dropDataBase((AppCompatActivity) context);

            while (csvReader.readRecord()) {
                String arriveTime = csvReader.get("arriveTime");
                Long arriveTimeLong = DateTimeUtils.convertStringDateTimeToLong(arriveTime, CommonUtils.ORIGINAL_DATETIME_FORMAT);
                String leaveTime = csvReader.get("leaveTime");
                Long leaveTimeLong = DateTimeUtils.convertStringDateTimeToLong(leaveTime, CommonUtils.ORIGINAL_DATETIME_FORMAT);
                String imgPaths = csvReader.get("imgPaths");

                CommonUtils.insertWorkdayTimeRecord((AppCompatActivity) context, arriveTimeLong, leaveTimeLong, imgPaths);
            }

            csvReader.close();
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (this.dialog.isShowing()) {
            this.dialog.dismiss();
        }

        if (success) {
            Toast.makeText(context, "导入成功！", Toast.LENGTH_SHORT).show();
            ((MainActivity) context).loadWorkdayTimes();
        } else {
            Toast.makeText(context, "导入失败！", Toast.LENGTH_SHORT).show();
        }
    }
}
