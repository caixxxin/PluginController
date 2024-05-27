package com.caixxxin.miplugincontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.caixxxin.miplugincontroller.MiPluginService;

public class AutoStartReceiver extends BroadcastReceiver {
    String TAG = "MiPluginAutoStartReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        //开机启动
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i(TAG, "boot");
            // Intent thisIntent = new Intent(context, MainActivity.class);//设置要启动的app
            // thisIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // context.startActivity(thisIntent);

            Intent serviceIntent = new Intent(context, MiPluginService.class);
            context.startService(serviceIntent);
        }
    }
}