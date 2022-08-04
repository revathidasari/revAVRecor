package com.example.avrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Button audioButton, videoButton;
    private NotificationManager notificationManager;
    private String channel_id = "Channel";

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;


    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";

    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private int mScreenDensity;

    private int mResultCode;
    private Intent mResultData;

    private Surface mSurface;
    private SurfaceView mSurfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }
        timerForLogs();
/*        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                storeFile();
            }
        },60000);*/
//storeFile();
        collectLogs();
       /* mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurface = mSurfaceView.getHolder().getSurface();*/
        audioButton = findViewById(R.id.audioRecord);
        videoButton = findViewById(R.id.videoRecord);
        if ((ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ) &&
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED  ) {
            Toast.makeText(this, "check", Toast.LENGTH_SHORT).show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.PROCESS_OUTGOING_CALLS}, 0);
        }

        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("revathi"," screen record called");
                executeOnCommandLine();
                //ContextCompat.startForegroundService(getApplicationContext(),new Intent(MainActivity.this,ScreenService.class));
/*                if (virtualDisplay == null) {
                    screenRecord();
                } else {
                    screenRecordStop();
                }*/
            }
        });
        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("revathi"," audio record called");

                Intent serviceIntent = new Intent(MainActivity.this, CustomService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                   /* NotificationChannel notificationChannel = new NotificationChannel(channel_id,
                            "AVRecorder", NotificationManager.IMPORTANCE_DEFAULT);
                    notificationManager.createNotificationChannel(notificationChannel);

                    createNotification();*/
                    ContextCompat.startForegroundService(getApplicationContext(),serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                finish();
            }
        });

    }

    private void executeOnCommandLine() {
        String command = "screenrecord — bit rate 8000000 --time-limit 30 /sdcard/Record.mp4";
                //"screenrecord /sdcard/demo.mp4";
        try {
            Process process = Runtime.getRuntime().exec(command);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("revathi","process "+process.isAlive());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void screenRecordStop() {
        if (virtualDisplay == null) {
            return;
        }
        virtualDisplay.release();
        virtualDisplay = null;
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResultData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode);
            outState.putParcelable(STATE_RESULT_DATA, mResultData);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "grant", Toast.LENGTH_SHORT).show();
                //https://gist.github.com/Venryx/e1f772b4c05b2da08e118ccd5cc162ff
                //https://stackoverflow.com/questions/36895433/recording-audio-in-background-service
                
            } else {
                Toast.makeText(this, " not grant", Toast.LENGTH_SHORT).show();

                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    public void createNotification(){

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),1,intent,0);
        Notification notification = new NotificationCompat.Builder(this,channel_id)
                .setContentTitle("AVRecorder...")
                .setContentText("Starting Audio recording")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .build();

        notificationManager.notify(1, notification);

    }

    public void timerForLogs() {
        CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long l) {
                Log.d("revathi","for every sec");
            }

            @Override
            public void onFinish() {
                storeFile();
            }
        };
        countDownTimer.start();

    }
    public void collectLogs() {
        try {


            //Runtime.getRuntime().exec("su");
            Runtime.getRuntime().exec("logcat -c");
            //Process process = Runtime.getRuntime().exec("logcat | grep -iE 'FATAL|Exception|app' > logfiletime.txt");
            Process process = Runtime.getRuntime().exec("logcat -d");
Log.d("revathi","collect logs started");
            //text shown without file storing
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            File fi = new File(Environment.getExternalStorageDirectory()+"/collectLogs.txt");
            FileWriter fileWriter = new FileWriter(fi);
            while ((line = bufferedReader.readLine()) !=null) {
                builder.append(line);
                fileWriter.append(line);
            }
            //fileWriter.append(builder);
            fileWriter.flush();
            fileWriter.close();
Log.d("revathi","collect logs after builder");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void storeFile() {
        try {
            File file = new File(Environment.getExternalStorageDirectory()+"/logfile.txt");//this.getExternalCacheDir()
            if (file.exists()) {
                file.delete();
            } else {
                file.createNewFile();
            }
            Log.d("revathi","store file path :"+file.getAbsolutePath());
            Runtime.getRuntime().exec("logcat -c");
            String command = "logcat -d -f "+file.getAbsolutePath();
            Runtime.getRuntime().exec(command);
//            InputStream inputStream = new FileInputStream(file);
//            Log.d("revathi","store file :"+inputStream.available());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        screenRecordStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tearDownMediaProjection();
    }

    private void setUpMediaProjection() {
        mediaProjection = mediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    private void tearDownMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
    public void screenRecord() {

       // mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Log.d("screen_rec", "MediaProjection service - capture"+ getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH));
        if (mediaProjection != null) {
            setUpVirtualDisplay();
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            setUpVirtualDisplay();
        } else {
            Log.i("screen_rec", "Requesting confirmation");
            // This initiates a prompt dialog for the user to confirm screen projection.
            startActivityForResult(
                    mediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
     //   startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),0);
    }
    private void setUpVirtualDisplay() {
        Log.i("screen_rec", "Setting up a VirtualDisplay: " +
                mSurfaceView.getWidth() + "x" + mSurfaceView.getHeight() +
                " (" + mScreenDensity + ")");
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                mSurfaceView.getWidth(), mSurfaceView.getHeight(), mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null, null);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i("screen_rec", "User cancelled");
                Toast.makeText(this,"can", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.i("screen_rec", "Starting screen capture");
            mResultCode = resultCode;
            mResultData = data;
            Log.i("screen_rec", "Starting screen capture"+mResultCode+" data "+mResultData);
            setUpMediaProjection();
            setUpVirtualDisplay();
        }
       /* Log.d("screen_rec", "on " + data + "activity result" + resultCode + "   " + resultCode);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        mediaProjection = mediaProjectionManager.getMediaProjection(0, null);
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
               *//* Display display = getWindow().getDecorView().getDisplay();
                Point size = new Point();
                DisplayMetrics metrics = new DisplayMetrics();
                display.getRealSize(size);
                display.getMetrics(metrics);

                View decorView = getWindow().getDecorView();
                Rect boundsToCheck = new Rect(0, 0, decorView.getWidth(), decorView.getHeight());
                int[] topLeft = new int[2];
                decorView.getLocationOnScreen(topLeft);
                boundsToCheck.offset(topLeft[0], topLeft[1]);

                if (boundsToCheck.width() < 90 || boundsToCheck.height() < 90) {
                    Log.d("screen_rec", "capture bounds too small to be a fullscreen activity: " + boundsToCheck);
                }
                Log.d("MediaProjection", "Size is " + size.toString()
                        + ", bounds are " + boundsToCheck.toShortString());
                virtualDisplay = mediaProjection.createVirtualDisplay("CtsCapturedActivity",
                        size.x, size.y,
                        metrics.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        null,
                        null *//**//*Callbacks*//**//*,
                        null *//**//*Handler*//**//*);*//*
            }
        }, 1000 * 60);

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("screen_rec", "run" + mediaProjection);

                if (mediaProjection != null) {
                    Log.d("screen_rec", "stop");
                    mediaProjection.stop();
                    mediaProjection = null;
                }

            }
        }, 60000);

*/
      //  mediaProjection.registerCallback(new MediaProjectionCallback(), null);
        // super.onActivityResult(requestCode, resultCode, data);

    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
          @Override
          public void onStop() {
             Log.d("screen_rec", "MediaProjectionCallback#onStop");
             if (virtualDisplay != null) {
                    virtualDisplay.release();
                    virtualDisplay = null;
             }
          }
    }
}



//https://stackoverflow.com/questions/10025824/how-to-record-audio-voice-in-background-continuously-in-android
//https://stackoverflow.com/questions/24405208/record-audio-as-an-android-service-and-stop-it-using-a-timer-recording-length-v
