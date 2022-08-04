package com.example.avrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenService extends Service {

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;

    private MediaProjectionCallback mediaProjectionCallback;

    private int mScreenDensity;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private int resultCode =0;
    private Intent data = null;
/*
    static
    {
        ORIENTAIONS.append(Surface.ROTATION_0, 90);
        ORIENTAIONS.append(Surface.ROTATION_90, 0);
        ORIENTAIONS.append(Surface.ROTATION_180, 270);
        ORIENTAIONS.append(Surface.ROTATION_270, 180);
    }
    private VideoView videoView;*/
    private String videoUri = "";
//    private RelativeLayout rootLayout;


    public ScreenService() {
    }

        private String outputFile = null;
        private Timer timer;
        private NotificationManager notificationManager;
        private String CHANNEL_ID = "channel_ID";

        @Override
        public void onCreate() {

            notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel notificationChannel = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                notificationChannel = new NotificationChannel(CHANNEL_ID,
                        "AVRecorder", NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(notificationChannel);

                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Screening")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentText("record").build();
                Log.d("revathi"," before start foreground screen record called");

                startForeground(1, notification);

            }

            DisplayMetrics metrics = new DisplayMetrics();
           // getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mScreenDensity = metrics.densityDpi;
            mediaRecorder = new MediaRecorder();
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


            SimpleDateFormat s = new SimpleDateFormat("ddMMyyyy_hhmmss");
            String format = s.format(new Date());
            outputFile = Environment.getExternalStorageDirectory().getAbsolutePath()+"/videofile.mp4";
            mediaProjectionCallback = new MediaProjectionCallback();
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            mediaProjection.registerCallback(mediaProjectionCallback, null);
            virtualDisplay = createVirtualDisplay();
            mediaRecorder.start();
            initRecorder();
            screenRecord();


           /* mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(outputFile);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                Log.d("revathi"," audio record started");

                Toast.makeText(this, "started", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
            }

            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    try {
                        mediaRecorder.stop();
                        mediaRecorder.reset();
                        mediaRecorder.release();
                        mediaRecorder = null;
                        Log.d("revathi"," audio record released");

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    stopSelf();
                    stopForeground(true);
                    Log.d("revathi","stopped");
                }
            }, 60 *1000);
*/
        }
    private void initRecorder() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            videoUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    + new StringBuilder("/Video_Screen").append(new SimpleDateFormat("dd-MM-yyyy-hh_mm_ss")
                    .format(new Date())).append(".mp4").toString();

            mediaRecorder.setOutputFile(videoUri);
            mediaRecorder.setVideoSize(DISPLAY_WIDTH,DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512*1000);
            mediaRecorder.setVideoFrameRate(30);
/*
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTAIONS.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);*/
            mediaRecorder.prepare();
        } catch (IOException e) {

            e.printStackTrace();
        }


    }
private void screenRecord() {
    if (mediaRecorder == null)
    {
        data = mediaProjectionManager.createScreenCaptureIntent();
        Log.d("revathi ","screen record intent "+data);
        return;
    }
    virtualDisplay = createVirtualDisplay();
    mediaRecorder.start();
}

    private VirtualDisplay createVirtualDisplay() {
Log.d("revathi"," vi "+mediaProjection+" "+DISPLAY_WIDTH+" "+DISPLAY_HEIGHT+" "+mScreenDensity+" "+DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
+" "+mediaRecorder.getSurface());
        return mediaProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);

    }
    private void stopRecordScreen() {

        if (virtualDisplay == null)
            return;
        virtualDisplay.release();
        destroyMediaProjection();
    }
    private class MediaProjectionCallback extends MediaProjection.Callback {

        @Override
        public void onStop() {
            super.onStop();

            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    try {
                        mediaRecorder.stop();
                        mediaRecorder.reset();
                        mediaRecorder.release();
                        mediaRecorder = null;
                        mediaProjection = null;
                        stopRecordScreen();
                        Log.d("revathi"," screen record released");

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    stopSelf();
                    stopForeground(true);
                    Log.d("revathi","stopped");
                }
            }, 60 *1000);


        }

    }
    private void destroyMediaProjection() {
        if (mediaProjection != null)
        {
            mediaProjection.unregisterCallback(mediaProjectionCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            //return super.onStartCommand(intent, flags, startId);
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }