package com.example.avrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CustomService extends Service {

    private MediaRecorder mediaRecorder;
    private String outputFile = null;
    private Timer timer;
    private NotificationManager notificationManager;
    private String CHANNEL_ID = "channel_ID";

    public CustomService() {
    }

    @Override
    public void onCreate() {

        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel notificationChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ID,
                    "AVRecorder", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("AAAAA")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("AAAAA").build();
            Log.d("revathi"," before start foreground audio record called");

            startForeground(1, notification);

        }


        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyy_hhmmss");
        String format = s.format(new Date());
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath()+"/filename.mp3";

        mediaRecorder = new MediaRecorder();
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