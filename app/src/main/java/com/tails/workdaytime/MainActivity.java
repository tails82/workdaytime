package com.tails.workdaytime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private static final int CHECK_PERMISSION_REQUEST_CODE = 1;
    private static final int CHOOSE_FILE_CODE = 5;
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    CommonListeners commonListeners;
    UploadUtils uploadUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CHECK_PERMISSION_REQUEST_CODE);
        }

        DbConnection.initialDataBase(this);

        Button btArrive = findViewById(R.id.btArrive);
        Button btLeave = findViewById(R.id.btLeave);
        Button btUpload = findViewById(R.id.btUpload);
        Button btEdit = findViewById(R.id.btEdit);

        commonListeners = new CommonListeners(this);
        uploadUtils = new UploadUtils(this, commonListeners);

        btArrive.setOnClickListener(new ArriveButtonOnClickListener());
        btLeave.setOnClickListener(new LeaveButtonOnClickListener());
        btUpload.setOnClickListener(commonListeners.getUploadButtonOnClickListener());
        btEdit.setOnClickListener(new EditButtonOnClickListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadWorkdayTimes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.itemExportCSV:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    new ExportDatabaseCSVTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    new ExportDatabaseCSVTask(this).execute();
                }

                return true;
            case R.id.itemImportCSV:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    startActivityForResult(Intent.createChooser(intent, "Choose File"), CHOOSE_FILE_CODE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "该手机没有文件选择器", Toast.LENGTH_LONG).show();
                }

                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == uploadUtils.CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            uploadUtils.appendPictureForDate(commonListeners.getUploadDate(), commonListeners.getImageFileUri().getPath());
        } else if (requestCode == uploadUtils.ALBUM_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            String selectedFileRealPath = FileUtils.getRealPathFromUri(getApplicationContext(), selectedFileUri);
            File selectedFile = new File(selectedFileRealPath);
            File destFile = CommonUtils.generateOutputImageFileForDate(commonListeners.getUploadDate(), FileUtils.extractFileName(selectedFileUri.getPath()));

            try {
                FileUtils.copyFile(selectedFile, destFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            uploadUtils.appendPictureForDate(commonListeners.getUploadDate(), destFile.getAbsolutePath());
        } else if (requestCode == CHOOSE_FILE_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            String selectedFileRealPath = FileUtils.getRealPathFromUri(getApplicationContext(), selectedFileUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    new ImportCSVDatabaseTask(this, selectedFileRealPath).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    new ImportCSVDatabaseTask(this, selectedFileRealPath).execute();
                }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CHECK_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

            }
        }
    }

    public long stringDateTimeToLong(String dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT);
        Date date = null;
        try {
            date = sdf.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date.getTime();
    }

    public String longToDateTimeString(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT);
        Date date = new Date(time);

        return sdf.format(date);
    }

    public void loadWorkdayTimes() {
        ListView listView = findViewById(R.id.listView);
        List<Map<String, Object>> workdayTimes = new ArrayList<>();

        Cursor c = CommonUtils.getAllRecords(this);
        c.moveToFirst();

        while(!c.isAfterLast()) {

            HashMap<String, Object> workDayTime = new HashMap<>();
            int idIndex = c.getColumnIndex("id");
            int arriveTimeIndex = c.getColumnIndex("arriveTime");
            int leaveTimeIndex = c.getColumnIndex("leaveTime");

            workDayTime.put("id", c.getInt(idIndex));
            workDayTime.put("arriveTime", longToDateTimeString(c.getLong(arriveTimeIndex)));
            workDayTime.put("leaveTime", longToDateTimeString(c.getLong(leaveTimeIndex)));
            double workHour = DateTimeUtils.getHourInterval(c.getLong(arriveTimeIndex), c.getLong(leaveTimeIndex));
            workDayTime.put("workHour", workHour);

            workdayTimes.add(workDayTime);

            c.moveToNext();
        }

        String[] fromArray = {"id", "arriveTime", "leaveTime", "workHour"};
        int[] to = {R.id.workdayTimeId, R.id.arriveTime, R.id.leaveTime, R.id.workHour};
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, workdayTimes, R.layout.workday_time_row, fromArray, to);

        listView.setAdapter(simpleAdapter);
        listView.setOnItemClickListener(new ListViewItemClickListener());
    }

    public void saveOrUpdateArriveTime(Date dateTime) {
        if (CommonUtils.hasRecordForDate(this, dateTime)) {
            updateArriveTime(dateTime);
            loadWorkdayTimes();
        } else {
            CommonUtils.insertWorkdayTimeRecord(this, dateTime.getTime(), null);
        }
    }

    public void saveOrUpdateLeaveTime(Date dateTime) {
        if (CommonUtils.hasRecordForDate(this, dateTime)) {
            updateLeaveTime(dateTime);
            loadWorkdayTimes();
        } else {
            CommonUtils.insertWorkdayTimeRecord(this, dateTime.getTime(), null);
        }
    }

    private void updateArriveTime(Date arriveDateTime) {
        Cursor c = CommonUtils.getRecordForDate(this, arriveDateTime);
        c.moveToFirst();
        int idIndex = c.getColumnIndex("id");
        Long id = c.getLong(idIndex);

        String sql = "UPDATE workdaytimes SET arriveTime = ? where id = ?";
        DbConnection.getDbConnection(this).execSQL(sql, new Long[] {arriveDateTime.getTime(), id});
    }

    private void updateLeaveTime(Date leaveDateTime) {
        Cursor c = CommonUtils.getRecordForDate(this, leaveDateTime);
        c.moveToFirst();
        int idIndex = c.getColumnIndex("id");
        Long id = c.getLong(idIndex);

        String sql = "UPDATE workdaytimes SET leaveTime = ? where id = ?";
        DbConnection.getDbConnection(this).execSQL(sql, new Long[] {leaveDateTime.getTime(), id});
    }

    class ArriveButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("记录上班时间？")
                    .setMessage("确定记录上班时间吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            saveOrUpdateArriveTime(new Date());
                            loadWorkdayTimes();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    class LeaveButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("记录下班时间？")
                    .setMessage("确定记录下班时间吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            saveOrUpdateLeaveTime(new Date());
                            loadWorkdayTimes();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    class ListViewItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            TextView tvId = view.findViewById(R.id.workdayTimeId);

            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("id", tvId.getText());

            startActivity(intent);
        }
    }

    class EditButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                    Calendar targetCalendar = Calendar.getInstance();
                    targetCalendar.set(year, month, day, 0, 0);
                    final Date targetDate = targetCalendar.getTime();

                    long id = CommonUtils.getRecordIdForDate(MainActivity.this, targetDate);

                    if (CommonUtils.getRecordIdForDate(MainActivity.this, targetDate) == -1) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("记录不存在！")
                                .setMessage("新建记录吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        CommonUtils.insertWorkdayTimeRecord(MainActivity.this, targetDate.getTime(), targetDate.getTime());
                                        loadWorkdayTimes();
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } else {
                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.putExtra("id", String.valueOf(id));

                        startActivity(intent);
                    }
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

            datePickerDialog.show();
        }
    }

}
