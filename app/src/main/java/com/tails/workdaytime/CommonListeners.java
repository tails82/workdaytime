package com.tails.workdaytime;

import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;

public class CommonListeners {
    private static final int CAMERA_REQUEST_CODE = 2;
    private static final int ALBUM_REQUEST_CODE = 3;

    private AppCompatActivity activity;
    private Uri imageFileUri;
    private Date uploadDate;
    private UploadButtonOnClickListener uploadButtonOnClickListener;

    public Uri getImageFileUri() {
        return imageFileUri;
    }

    public void setImageFileUri(Uri imageFileUri) {
        this.imageFileUri = imageFileUri;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public CommonListeners(AppCompatActivity activity) {
        this.activity = activity;
    }

    public UploadButtonOnClickListener getUploadButtonOnClickListener() {
        if (uploadButtonOnClickListener == null) {
            uploadButtonOnClickListener = new UploadButtonOnClickListener();
        }

        return uploadButtonOnClickListener;
    }

    class UploadButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btUpload:
                    Date nowDate = new Date();

                    if (!CommonUtils.hasRecordForDate(activity, nowDate)) {
                        Toast.makeText(activity, "今天还没有记录上下班时间！", Toast.LENGTH_LONG).show();
                        return;
                    }

                    uploadDate = nowDate;

                    break;
                case R.id.btUploadOnDetailActivity:
                    TextView dateTextView = ((AppCompatActivity) view.getContext()).findViewById(R.id.tvWorkDate);

                    uploadDate = DateTimeUtils.convertStringToDateTime(dateTextView.getText().toString(), "yyyy-MM-dd");

                    break;
            }


            showUploadPopup();
        }

        private void showUploadPopup() {
            View imageUploadPopup = View.inflate(activity, R.layout.image_upload_popup, null);
            Button bt_album = imageUploadPopup.findViewById(R.id.btn_pop_album);
            Button bt_camera = imageUploadPopup.findViewById(R.id.btn_pop_camera);
            Button bt_cancle = imageUploadPopup.findViewById(R.id.btn_pop_cancel);

            int weight = activity.getResources().getDisplayMetrics().widthPixels;
            int height = activity.getResources().getDisplayMetrics().heightPixels * 1/3;

            final PopupWindow popupWindow = new PopupWindow(imageUploadPopup,weight,height);
            popupWindow.setFocusable(true);

            popupWindow.setOutsideTouchable(true);

            bt_album.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activity.startActivityForResult(intent, ALBUM_REQUEST_CODE);
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
                    WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
                    lp.alpha = 1.0f;
                    activity.getWindow().setAttributes(lp);
                }
            });

            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            lp.alpha = 0.5f;
            activity.getWindow().setAttributes(lp);
            popupWindow.showAtLocation(imageUploadPopup, Gravity.BOTTOM,0,50);
        }

        public void capturePhoto() {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            imageFileUri = getOutputImageFileUri();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageFileUri);
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            activity.startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }

        private Uri getOutputImageFileUri() {
            return Uri.fromFile(CommonUtils.generateOutputImageFileForDate(uploadDate));
        }
    }

}
