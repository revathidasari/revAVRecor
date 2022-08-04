package com.example.avrecorder;

import static android.app.Activity.RESULT_OK;
import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

public class BGService extends Service {

    private static final String TAG  = "BGservice";
    private ServiceHandler mServiceHandler;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private int resultCode = 0;
    private Intent data= null;
    private BroadcastReceiver mScreenStateReceiver;
    boolean isInitialized=false;

    private final String EXTRA_RESULT_CODE = "resultcode";
    private final String EXTRA_DATA = "data";
    private final int ONGOING_NOTIFICATION_ID = 23;
    private class ServiceHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (resultCode == RESULT_OK) {
                startRecording(resultCode,data);
            }
            else {
                Log.d(TAG,"result code is not matched");
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent notificationIntent = new Intent(this, BGService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("DataRecorder")
                .setContentText("Your screen is being recorded and saved to your phone.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setTicker("Tickertext")
                .build();
        isInitialized=true;
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        /*mScreenStateReceiver = MyBroadcastReceiver()
        val screenStateFilter = IntentFilter()
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON)
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF)
        screenStateFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED)
        registerReceiver(mScreenStateReceiver, screenStateFilter)*/
        Thread thread = new HandlerThread(
                "ServiceStartArguments",
                THREAD_PRIORITY_BACKGROUND
        );
        thread.start();

       // mServiceHandler = new ServiceHandler(new Thread(thread.get))

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "started recording service", Toast.LENGTH_SHORT).show();
        resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        data = intent.getParcelableExtra(EXTRA_DATA);
      //  check(!(resultCode == 0 || data == null)) { "Result code or data missing." }
        createNotificationChannel();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, MainActivity.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("is running......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);
    }

    private void startRecording(Integer resultCode, Intent data) {
        mProjectionManager =
                (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mMediaRecorder = new MediaRecorder();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)
                getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int mScreenDensity = metrics.densityDpi;
        int displayWidth = metrics.widthPixels;
        int displayHeight = metrics.heightPixels;
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoEncodingBitRate(8 * 1000 * 1000);
        mMediaRecorder.setVideoFrameRate(15);
        mMediaRecorder.setVideoSize(displayWidth, displayHeight);
        String videoDir =
                Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES)
                        .getAbsolutePath();
        Long timestamp = System.currentTimeMillis();
        String orientation = "portrait";
        if (displayWidth > displayHeight) {
            orientation = "landscape";
        }
        String filePathAndName =
                videoDir + "/time_" + timestamp.toString() + "_mode_" + orientation + ".mp4";
        mMediaRecorder.setOutputFile(filePathAndName);
        try {
            mMediaRecorder.prepare();
        }  catch (IOException e)  {
            e.printStackTrace();
        }
        if(isInitialized){
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            Surface surface = mMediaRecorder.getSurface();
                    mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                    "MainActivity",
                    displayWidth,
                    displayHeight,
                    mScreenDensity,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                    surface,
                    null,
                    null
            );
            mMediaRecorder.start();
            Log.v(TAG, "Started recording");

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
