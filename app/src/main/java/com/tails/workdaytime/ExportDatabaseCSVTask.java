package com.tails.workdaytime;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class ExportDatabaseCSVTask extends AsyncTask<String, Void, Boolean> {

    private static final String EXPORT_FOLDER_NAME = "workdaytime";

    private Context context;

    public ExportDatabaseCSVTask(Context context) {
        super();

        this.context = context;
    }

    private ProgressDialog dialog;

    @Override
    protected void onPreExecute() {
        dialog = new ProgressDialog(context);
        this.dialog.setMessage("正在导出数据...");
        this.dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        Date nowDate = new Date();
        String dateString = DateTimeUtils.generateDateString(nowDate, "yyyy-MM-dd-HH-mm-ss");
        File exportDir = new File(Environment.getExternalStorageDirectory(), "/" + EXPORT_FOLDER_NAME + "/");

        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "backup-" + dateString + ".csv");

        try {
            file.createNewFile();
            CsvWriter csvWrite = new CsvWriter(new FileWriter(file), ',');
            CsvReader csvReader = new CsvReader(new FileReader(file), ',');
            Cursor curCSV = CommonUtils.getAllRecords((AppCompatActivity) context);
            csvWrite.writeRecord(curCSV.getColumnNames());

            while (curCSV.moveToNext()) {
                String arrStr[] = null;
                String[] mySecondStringArray = new String[curCSV.getColumnNames().length];

                for (int i = 0; i < curCSV.getColumnNames().length; i++) {
                    String currentColumnName = curCSV.getColumnNames()[i];

                    if (currentColumnName.equalsIgnoreCase("arriveTime") || currentColumnName.equalsIgnoreCase("leaveTime")) {
                        mySecondStringArray[i] = DateTimeUtils.convertLongToString(curCSV.getLong(i), CommonUtils.ORIGINAL_DATETIME_FORMAT);
                    } else {
                        mySecondStringArray[i] = curCSV.getString(i);
                    }
                }

                csvWrite.writeRecord(mySecondStringArray);
            }

            csvWrite.close();
            curCSV.close();
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (this.dialog.isShowing()) {
            this.dialog.dismiss();
        }

        if (success) {
            Toast.makeText(context, "导出成功！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "导出失败！", Toast.LENGTH_SHORT).show();
        }
    }
}
