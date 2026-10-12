package com.connect_screen.mirror.job;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.util.Log;

import com.connect_screen.mirror.BuildConfig;
import com.connect_screen.mirror.PureBlackActivity;
import com.connect_screen.mirror.State;
import com.connect_screen.mirror.SunshineService;
import com.connect_screen.mirror.TouchpadAccessibilityService;
import com.connect_screen.mirror.shizuku.ShizukuUtils;

public class ExitAll {
    public static void execute(Context context, boolean restart) {
        if (SunshineService.instance != null) {
            SunshineService.instance.releaseWakeLock();
        }
        boolean wasSunshineStarted = SunshineServer.exitServer();
        CreateVirtualDisplay.restoreAspectRatio();
        InputRouting.moveImeToDefault();
        SunshineAudio.restoreVolume(context);
        State.unbindUserService();
        if (State.mediaProjectionInUse != null) {
            State.mediaProjectionInUse.stop();
            State.mediaProjectionInUse = null;
        }
        State.setMediaProjection(null);
        // Launch from outside this process so System.exit(0) cannot kill the new Activity.
        if (restart) {
            PackageManager packageManager = context.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (intent != null && intent.getComponent() != null && alarmManager != null) {
                Intent mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
                mainIntent.setPackage(context.getPackageName());
                mainIntent.putExtra("DoNotAutoStartMoonlight", true);
                PendingIntent launch = PendingIntent.getActivity(context, 0, mainIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 1200, launch);
            } else {
                Log.e("ExitAll", "无法安排应用重启：缺少启动入口或 AlarmManager");
            }
        }

        if (State.mirrorVirtualDisplay != null) {
            State.mirrorVirtualDisplay.release();
        }

        State.displaylinkState.destroy();

        if (!restart && !wasSunshineStarted && !ShizukuUtils.hasPermission() && TouchpadAccessibilityService.getInstance() != null) {
            // 对于 typec 直连，但是只用无障碍的用户不退无障碍
            if (State.getCurrentActivity() != null) {
                State.getCurrentActivity().finish();
            }
            return;
        }
        if (TouchpadAccessibilityService.getInstance() != null && ShizukuUtils.hasPermission()) {
            // 下次可自动获取
            TouchpadAccessibilityService.getInstance().disableSelf();
        }
        if (context != null) {
            context.stopService(new Intent(context, SunshineService.class));
        }
        // 退出应用进程
        System.exit(0);
        try {
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch(Throwable e) {
            // ignore
        }
    }
}
