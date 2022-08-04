package com.example.avrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class RecordActivity extends AppCompatActivity {

    private final int REQUEST_CODE=100;
    private final int REQUEST_PERMISSION=101;
    MediaProjectionManager mediaProjectionManager;
    MediaProjection mediaProjection;
    VirtualDisplay virtualDisplay;
    MediaProjectionCallBack mediaProjectionCallBack;
    MediaRecorder mediaRecorder;

    RelativeLayout rootlayout;
    Button toggleButton;
    boolean isChecked = false;
    private VideoView videoView;
    private String videoUri = "";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private int mScreenDensity =0;
    private static final int DISPLAY_WIDTH =720;
    private static final int DISPLAY_HEIGHT=1280;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        videoView = findViewById(R.id.videoView);
        toggleButton = findViewById(R.id.buttonVideo);
        rootlayout = findViewById(R.id.rootLayout);

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(RecordActivity.this, Manifest.permission.RECORD_AUDIO)
                        + ContextCompat.checkSelfPermission(RecordActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                    isChecked = false;
                    ActivityCompat.requestPermissions(RecordActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);

                } else {
                    toggleScreenShare(toggleButton);
                }
            }
        });
    }

    private void toggleScreenShare(Button toggleButton) {
        if (!isChecked) {
            initRecorder();
            recordScreen();
            isChecked = true;
            toggleButton.setText("OFF");
        } else{
            try {
                mediaRecorder.stop();
                mediaRecorder.reset();
                stopRecordingScreen();
            } catch (Exception e) {
                e.printStackTrace();
            }
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(Uri.parse(videoUri));
            videoView.start();
            isChecked = false;
            toggleButton.setText("ON");
        }
    }

    private void stopRecordingScreen() {
        if (virtualDisplay == null) {
            return;
        }
        virtualDisplay.release();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mediaProjection!=null){
            mediaProjection.unregisterCallback(mediaProjectionCallBack);
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    private void recordScreen() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        virtualDisplay = createVirtualDisplay();
        try {
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VirtualDisplay createVirtualDisplay() {
        Log.d("RecordActivity","virtual "+DISPLAY_WIDTH+" "+DISPLAY_HEIGHT+" "+mScreenDensity
        +" "+DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR + " "+mediaRecorder.getSurface());
        return mediaProjection.createVirtualDisplay("RecordActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT,
                mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(),
                null, null);
    }

    private void initRecorder() {
        try {
            String recordingFile = "ScreenREC"+System.currentTimeMillis()+".mp4";
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File folder = new File(path,"MyScreenRec/");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(folder, recordingFile);
            videoUri =file.getAbsolutePath();

            mediaRecorder.setOutputFile(videoUri);
            mediaRecorder.setVideoSize(2048,1024);//DISPLAY_WIDTH,DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512*1000);
            mediaRecorder.setVideoFrameRate(30);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation+90);

            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CODE) {
            Toast.makeText(this, "uri error", Toast.LENGTH_SHORT).show();
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
            isChecked = false;
            return;
        }
        mediaProjectionCallBack = new MediaProjectionCallBack(mediaRecorder,mediaProjection);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(mediaProjectionCallBack, null);
        virtualDisplay = createVirtualDisplay();
        try {
            mediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MediaProjectionCallBack extends MediaProjection.Callback {

        public MediaProjectionCallBack(MediaRecorder mediaRecorder, MediaProjection mediaProjection) {
        }

        @Override
        public void onStop() {
            if (isChecked) {
                isChecked = false;
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
            mediaProjection =null;
            stopRecordingScreen();
            super.onStop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
        if (grantResults.length>0 && grantResults[0]+grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            toggleScreenShare(toggleButton);
        } else {
            isChecked = false;
            ActivityCompat.requestPermissions(RecordActivity.this,new String[]{Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE);
        }
        }
    }
}