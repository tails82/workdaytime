package com.tails.workdaytime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
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
    private static final int CAMERA_REQUEST_CODE = 2;
    private static final int ALBUM_REQUEST_CODE = 3;
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private Uri imageFileUri;
    private Date uploadDate;

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

        btArrive.setOnClickListener(new ArriveButtonOnClickListener());
        btLeave.setOnClickListener(new LeaveButtonOnClickListener());
        btUpload.setOnClickListener(new UploadButtonOnClickListener());
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadWorkdayTimes();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            appendPictureForDate(uploadDate, imageFileUri.getPath());
        } else if (requestCode == ALBUM_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            String selectedFileRealPath = FileUtils.getRealPathFromUri(getApplicationContext(), selectedFileUri);
            File selectedFile = new File(selectedFileRealPath);
            File destFile = generateOutputImageFileForDate(uploadDate, FileUtils.extractFileName(selectedFileUri.getPath()));
            try {
                FileUtils.copyFile(selectedFile, destFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            appendPictureForDate(uploadDate, destFile.getAbsolutePath());
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

    public void insertWorkdayTimeRecord(Long arriveTime, Long leaveTime) {
        if (arriveTime == null && leaveTime == null) {
            return;
        } else if (leaveTime == null) {
            leaveTime = arriveTime;
        } else if ( arriveTime == null) {
            arriveTime = leaveTime;
        }
        String sql = "INSERT INTO workdaytimes(arriveTime, leaveTime) values (?, ?)";
        DbConnection.getDbConnection(this).execSQL(sql, new Long[]{arriveTime, leaveTime});
    }

    public void loadWorkdayTimes() {
        ListView listView = findViewById(R.id.listView);
        List<Map<String, Object>> workdayTimes = new ArrayList<>();

        Cursor c = DbConnection.getDbConnection(this).rawQuery("SELECT * FROM workdaytimes ORDER BY arriveTime DESC", null);
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
        if (hasRecordForDate(dateTime)) {
            updateArriveTime(dateTime);
            loadWorkdayTimes();
        } else {
            insertWorkdayTimeRecord(dateTime.getTime(), null);
        }
    }

    public void saveOrUpdateLeaveTime(Date dateTime) {
        if (hasRecordForDate(dateTime)) {
            updateLeaveTime(dateTime);
            loadWorkdayTimes();
        } else {
            insertWorkdayTimeRecord(dateTime.getTime(), null);
        }
    }

    private void updateArriveTime(Date arriveDateTime) {
        Cursor c = getRecordForDate(arriveDateTime);
        c.moveToFirst();
        int idIndex = c.getColumnIndex("id");
        Long id = c.getLong(idIndex);

        String sql = "UPDATE workdaytimes SET arriveTime = ? where id = ?";
        DbConnection.getDbConnection(this).execSQL(sql, new Long[] {arriveDateTime.getTime(), id});
    }

    private void updateLeaveTime(Date leaveDateTime) {
        Cursor c = getRecordForDate(leaveDateTime);
        c.moveToFirst();
        int idIndex = c.getColumnIndex("id");
        Long id = c.getLong(idIndex);

        String sql = "UPDATE workdaytimes SET leaveTime = ? where id = ?";
        DbConnection.getDbConnection(this).execSQL(sql, new Long[] {leaveDateTime.getTime(), id});
    }

    private void updateTodayImgPaths(String imgPaths) {
        Cursor c = getRecordForDate(uploadDate);
        c.moveToFirst();
        int idIndex = c.getColumnIndex("id");
        Long id = c.getLong(idIndex);

        String sql = "UPDATE workdaytimes SET imgPaths = ? where id = ?";
        DbConnection.getDbConnection(this).execSQL(sql, new Object[] {imgPaths, id});
    }

    public boolean hasRecordForDate(Date date) {

        return getRecordForDate(date).getCount() > 0;
    }

    public Cursor getRecordForDate(Date date) {
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

        return DbConnection.getDbConnection(this).rawQuery(sql , new String[] {String.valueOf(dayStartTime), String.valueOf(dayEndTime), String.valueOf(dayStartTime), String.valueOf(dayEndTime)});
    }

    public void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageFileUri = getOutputImageFileUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    private File generateOutputImageFileForDate(Date date) {
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String timeStamp = sdf.format(currentDate);

        return generateOutputImageFileForDate(date, timeStamp + ".jpg");
    }

    private File generateOutputImageFileForDate(Date date, String fileName) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/workdaytime/" + generateDateString(date));

        if (!dir.exists()) {
            dir.mkdirs();
        }

        File mediaFile = new File(dir, fileName);

        return mediaFile;
    }

    private Uri getOutputImageFileUri() {
        return Uri.fromFile(generateOutputImageFileForDate(uploadDate));
    }

    public void appendPictureForDate(Date date, String path) {
        Cursor c = getRecordForDate(date);
        c.moveToFirst();
        int imgPathsIndex = c.getColumnIndex("imgPaths");
        String[] imgPathsArray = CommonUtils.convertStringToArray(c.getString(imgPathsIndex));
        String[] updatedImgPathsArray = CommonUtils.appendStringArray(imgPathsArray, path);
        String updatedImgPaths = CommonUtils.convertArrayToString(updatedImgPathsArray);
        updateTodayImgPaths(updatedImgPaths);
    }

    public String generateDateString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(date);
    }

    private void showUploadPopup() {
        View imageUploadPopup = View.inflate(this, R.layout.image_upload_popup, null);
        Button bt_album = imageUploadPopup.findViewById(R.id.btn_pop_album);
        Button bt_camera = imageUploadPopup.findViewById(R.id.btn_pop_camera);
        Button bt_cancle = imageUploadPopup.findViewById(R.id.btn_pop_cancel);

        int weight = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels*1/3;

        final PopupWindow popupWindow = new PopupWindow(imageUploadPopup,weight,height);
        popupWindow.setFocusable(true);

        popupWindow.setOutsideTouchable(true);

        bt_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, ALBUM_REQUEST_CODE);
                popupWindow.dismiss();
            }
        });

        bt_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capturePhoto();
                popupWindow.dismiss();
            }
        });

        bt_cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1.0f;
                getWindow().setAttributes(lp);
            }
        });

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.5f;
        getWindow().setAttributes(lp);
        popupWindow.showAtLocation(imageUploadPopup, Gravity.BOTTOM,0,50);
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

    class UploadButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Date nowDate = new Date();
            if (!hasRecordForDate(nowDate)) {
                Toast.makeText(MainActivity.this, "今天还没有记录上下班时间！", Toast.LENGTH_LONG).show();
                return;
            }
            uploadDate = nowDate;
            showUploadPopup();
        }
    }

    class ListViewItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            TextView tvId = view.findViewById(R.id.workdayTimeId);
            TextView tvArriveTime = view.findViewById(R.id.arriveTime);
            TextView tvLeaveTime = view.findViewById(R.id.leaveTime);
            TextView tvWorkHour = view.findViewById(R.id.workHour);

            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("id", tvId.getText());
            intent.putExtra("arriveTime", tvArriveTime.getText());
            intent.putExtra("leaveTime", tvLeaveTime.getText());
            intent.putExtra("workHour", tvWorkHour.getText());

            startActivity(intent);
        }
    }

}
