package com.tails.workdaytime;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DetailActivity extends AppCompatActivity {
    private static final String ORIGINAL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final int NUMBER_OF_IMAGERS_PER_ROW = 4;

    private String currentDate;
    private TextView tvWorkDate;
    private EditText edArriveTime;
    private EditText edLeaveTime;
    private EditText edWorkHour;
    private Button btUpload;
    private Button btSave;
    private Button btDelete;
    private String arriveDateTime;
    private String leaveDateTime;
    private String workHour;
    private String id;

    private CommonListeners commonListeners;
    private UploadUtils uploadUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        commonListeners = new CommonListeners(this);
        uploadUtils = new UploadUtils(this, commonListeners);

        edArriveTime = findViewById(R.id.edArriveTime);
        edLeaveTime = findViewById(R.id.edLeaveTime);
        edWorkHour = findViewById(R.id.edWorkHour);
        tvWorkDate = findViewById(R.id.tvWorkDate);
        btUpload = findViewById(R.id.btUploadOnDetailActivity);
        btSave = findViewById(R.id.btSave);
        btDelete = findViewById(R.id.btDelete);

        edArriveTime.setOnClickListener(new TimeEditTextListener());
        edLeaveTime.setOnClickListener(new TimeEditTextListener());
        btUpload.setOnClickListener(commonListeners.getUploadButtonOnClickListener());
        btSave.setOnClickListener(new SaveButtonListener());
        btDelete.setOnClickListener(new DeleteButtonListener());

        loadWorkDayTime();
        loadImage();
    }

    public void loadWorkDayTime() {
        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        arriveDateTime = intent.getStringExtra("arriveTime");
        leaveDateTime = intent.getStringExtra("leaveTime");
        String expectedDateFormat = "yyyy-MM-dd";
        String expectedTimeFormat = "HH:mm:ss";
        String arriveDate = DateTimeUtils.formatDateTime(arriveDateTime, ORIGINAL_DATETIME_FORMAT, expectedDateFormat);
        String arriveTime = DateTimeUtils.formatDateTime(arriveDateTime, ORIGINAL_DATETIME_FORMAT, expectedTimeFormat);
        String leaveTime = DateTimeUtils.formatDateTime(leaveDateTime, ORIGINAL_DATETIME_FORMAT, expectedTimeFormat);
        workHour = intent.getStringExtra("workHour");

        currentDate = arriveDate;
        tvWorkDate.setText(arriveDate);
        edArriveTime.setText(arriveTime);
        edLeaveTime.setText(leaveTime);
        edWorkHour.setText(workHour);
    }

    public double calculateWorkHour() {
        String arriveDateTime = currentDate + " " + edArriveTime.getText();
        String leaveDateTime = currentDate + " " + edLeaveTime.getText();

        return DateTimeUtils.getHourInterval(ORIGINAL_DATETIME_FORMAT, arriveDateTime, leaveDateTime);
    }

    private Date getDateFromTextEdit(EditText timeTextEdit) {
        String dateTimeString = currentDate + " " + timeTextEdit.getText();
        SimpleDateFormat sdf = new SimpleDateFormat(ORIGINAL_DATETIME_FORMAT);
        Date dateTime = null;

        try {
            dateTime = sdf.parse(dateTimeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dateTime;
    }

    private int getHourFromTextEdit(EditText timeTextEdit) {
        return getDateFromTextEdit(timeTextEdit).getHours();
    }

    private int getMinuteFromTextEdit(EditText timeTextEdit) {
        return getDateFromTextEdit(timeTextEdit).getMinutes();
    }

    private void updateDBData() {
        arriveDateTime = currentDate + " " + edArriveTime.getText().toString();
        leaveDateTime = currentDate + " " + edLeaveTime.getText().toString();
        workHour = edWorkHour.getText().toString();
        long arriveDateTimeLong = DateTimeUtils.convertStringDateTimeToLong(arriveDateTime, ORIGINAL_DATETIME_FORMAT);
        long leaveDateTimeLong = DateTimeUtils.convertStringDateTimeToLong(leaveDateTime, ORIGINAL_DATETIME_FORMAT);

        DbConnection.getDbConnection(this).execSQL("UPDATE workdaytimes SET arriveTime = ?, leaveTime = ? WHERE id = ?",
                new Long[] {arriveDateTimeLong, leaveDateTimeLong, Long.valueOf(id)});
    }

    private void loadImage() {
        Cursor c = DbConnection.getDbConnection(this).rawQuery("SELECT imgPaths FROM workdaytimes WHERE id = ?", new String[] {id});
        c.moveToFirst();

        if (c.getCount() == 0) {
            return;
        }

        String imgPaths = c.getString(0);
        String[] imgPathsArray = CommonUtils.convertStringToArray(imgPaths);

        GridLayout gridLayout = findViewById(R.id.imgGrid);
        gridLayout.setColumnCount(NUMBER_OF_IMAGERS_PER_ROW);
        WindowManager wm = this.getWindowManager();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidthInPixels = displayMetrics.widthPixels;

        gridLayout.removeAllViews();

        for (String path : imgPathsArray) {
            Bitmap bmImg = BitmapFactory.decodeFile(path);
            ImageView imageView = new ImageView(DetailActivity.this);
            int marginInPixels = (int) (5 * this.getResources().getDisplayMetrics().density);
            int imageWidth = screenWidthInPixels / NUMBER_OF_IMAGERS_PER_ROW - 2 * marginInPixels;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageWidth, imageWidth);
            params.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels);
            imageView.setLayoutParams(params);
            imageView.setImageBitmap(bmImg);
            gridLayout.addView(imageView);
        }
    }

    private void deleteRecordById(String id) {
        DbConnection.getDbConnection(this).execSQL("DELETE FROM workdaytimes where id = ?", new Long[]{Long.valueOf(id)});
    }

    private void deleteImagesByDate(String dateString) {
        Date date = DateTimeUtils.convertStringToDateTime(dateString, "yyyy-MM-dd");
        File dir = CommonUtils.generateOutputImageFileFolderForDate(date);
        FileUtils.deleteDirectory(dir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == uploadUtils.CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            uploadUtils.appendPictureForDate(commonListeners.getUploadDate(), commonListeners.getImageFileUri().getPath());
            loadImage();
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
            loadImage();
        }
    }

    public class TimeEditTextListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            TimePickerDialog timePickerDialog = new TimePickerDialog(DetailActivity.this, AlertDialog.THEME_HOLO_LIGHT, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int i, int i1) {
                    String timeString = timePicker.getHour() + ":" + timePicker.getMinute() + ":00";
                    timeString = DateTimeUtils.formatDateTime(timeString, "H:m:00", "HH:mm:00");
                    ((EditText) view).setText(timeString);
                    edWorkHour.setText(String.valueOf(calculateWorkHour()));
                }
            }, getHourFromTextEdit((EditText) view), getMinuteFromTextEdit((EditText) view), true);

            timePickerDialog.show();
        }
    }

    public class SaveButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            updateDBData();
        }
    }

    public class DeleteButtonListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            new androidx.appcompat.app.AlertDialog.Builder(DetailActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("确定删除？")
                    .setMessage("确定删除记录吗？")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deleteRecordById(id);
                            deleteImagesByDate(currentDate);
                            finish();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }
    }
}

