package com.example.avrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
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

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenRecordService extends Service {
    public ScreenRecordService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    SharedPreferences p;
    WindowManager window;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private boolean isRecord = false;
    private String videoUri = "";

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;

    private int mScreenDensity;
    private static int DISPLAY_WIDTH;
    private static int DISPLAY_HEIGHT;
    private int resultCode;
    private Intent resultData;


/*    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }*/

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("revathi", "oncreate called");
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "notification_id")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Record")
                .setAutoCancel(true);


/*
        // create notification channel
        new NotificationManager(this,11111);
        // Create notification builder.
        Intent notificationIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getService(context, 1212, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationManager.CHANNEL_ID);
        builder.setSmallIcon(R.drawable.notification);
        builder.setColor(ContextCompat.getColor(this, R.color.white));
        builder.setContentTitle(getString(title));
        builder.setContentIntent(pendingIntent);
        Notification notification = builder.build();*/
        Notification notification = builder.build();

        startForeground(123, notification);

        p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        window.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        DISPLAY_WIDTH = metrics.widthPixels;
        DISPLAY_HEIGHT = metrics.heightPixels;
      //  ImageReader imageReader = ImageReader.newInstance(DISPLAY_WIDTH, DISPLAY_HEIGHT,PixelFormat.RGBA_8888, 2);
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
Log.d("revathi","media projection through context"+DISPLAY_WIDTH);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction() == null) {
            resultCode = intent.getIntExtra("code", 1337);
            resultData = intent.getParcelableExtra("data");
        }
Log.d("revathi","on start cmd :"+resultCode+" data "+resultData);
        onStartRecord();
        return (START_NOT_STICKY);
    }

    @Override
    public void onDestroy() {
        onStopRecord();
        super.onDestroy();
    }
    private void onStartRecord() {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
       // mediaProjection.registerCallback(new MediaProjectionCallback(), resultData);
        Log.d("revathi","get media projection "+resultCode+"   "+resultData);
/*
        int quality = p.getInt("quality", 1080);
        boolean micro = p.getBoolean("micro", false);
        int fps = p.getInt("FPS", 60);*/

    //    initRecorder(quality, micro, fps);
        virtualDisplay = createVirtualDisplay();

        isRecord = true;
/*        p.edit().putBoolean("isRecord", isRecord).apply();
        mediaRecorder.start();*/
    }

    private void onStopRecord() {
      /*  mediaRecorder.stop();
        mediaRecorder.reset();
*/
        stopRecordScreen();
        isRecord = false;
       // p.edit().putBoolean("isRecord", isRecord).apply();
    }

    private VirtualDisplay createVirtualDisplay() {
        Log.d("revathi","create virtual display called");
        SurfaceView surfaceView = new SurfaceView(this);
                      surfaceView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                              LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                   //   surfaceView.getHolder().addCallback(this);
        return mediaProjection.createVirtualDisplay("MainFragment", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surfaceView.getHolder().getSurface()/*mediaRecorder.getSurface()*/, null, null);
    }

    private void initRecorder(int QUALITY, boolean isMicro, int fps) {
        try {
            int bitrateVideo = 0;

            switch (QUALITY) {
                case 1080:
                    bitrateVideo = 7000000;
                    break;
                case 720:
                    bitrateVideo = 4000000;
                    break;
                default:
                    bitrateVideo = 2000000;
                    break;
            }

            if (isMicro) {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }

            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            if (isMicro) {
                mediaRecorder.setAudioSamplingRate(44100);
                mediaRecorder.setAudioEncodingBitRate(16 * 44100);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            }


            videoUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    + new StringBuilder("/FreeRecord_").append(new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss")
                    .format(new Date())).append(".mp4").toString();

            mediaRecorder.setOutputFile(videoUri);
            mediaRecorder.setVideoSize(720, 1300);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoEncodingBitRate(bitrateVideo);
            mediaRecorder.setCaptureRate(fps);
            mediaRecorder.setVideoFrameRate(fps);

            int rotation = window.getDefaultDisplay().getRotation();
            int orientation = 0;//ORIENTATIONS.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordScreen() {
        if (virtualDisplay == null) {
            return;
        }
        Log.d("revathi","stop record "+virtualDisplay);
        virtualDisplay.release();
        virtualDisplay = null;
        destroyMediaProject();
    }

    private void destroyMediaProject() {
        Log.d("revathi","destroy media"+mediaProjection);
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}