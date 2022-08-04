package com.example.avrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

public class LocalMediaProjectionService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private Bitmap mTestBitmap;

    private static final String NOTIFICATION_CHANNEL_ID = "Surfacevalidator";
    private static final String CHANNEL_NAME = "ProjectionService";

    public class LocalBinder extends Binder {
        LocalMediaProjectionService getService() {
            return LocalMediaProjectionService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if (mTestBitmap != null) {
            mTestBitmap.recycle();
            mTestBitmap = null;
        }
        super.onDestroy();
    }
/*
    private Icon createNotificationIcon() {
        mTestBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mTestBitmap);
        canvas.drawColor(Color.BLUE);
        return Icon.createWithBitmap(mTestBitmap);
    }*/

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            final NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            final Notification.Builder notificationBuilder =
                    new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);

            final Notification notification = notificationBuilder.setOngoing(true)
                    .setContentTitle("App is running")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentText("Context")
                    .build();

            startForeground(2, notification);
        }
    }

}
