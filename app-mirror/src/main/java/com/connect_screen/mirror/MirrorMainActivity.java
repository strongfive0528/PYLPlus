package com.connect_screen.mirror;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionConfig;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.connect_screen.mirror.job.AcquireShizuku;
import com.connect_screen.mirror.job.AutoRotateAndScaleForDisplaylink;
import com.connect_screen.mirror.job.CreateVirtualDisplay;
import com.connect_screen.mirror.job.ExitAll;
import com.connect_screen.mirror.shizuku.ShizukuUtils;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import rikka.shizuku.Shizuku;
import com.topjohnwu.superuser.Shell;

public class MirrorMainActivity extends AppCompatActivity implements IMainActivity {

    static {
        // Set settings before the main shell can be created
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10));
    }

    public static final int REQUEST_CODE_MEDIA_PROJECTION = 1001; // 定义一个请求码
    public static final int REQUEST_RECORD_AUDIO_PERMISSION = 1002;
    public static final String TAG = "MirrorMainActivity";

    private static final String FIRST_RUN_PREFS = "first_run";
    private static final String KEY_PERMISSION_TIP_VERSION = "permission_tip_version";

    private static final int PERMISSION_STEP_MICROPHONE = 0;
    private static final int PERMISSION_STEP_ACCESSIBILITY = 1;
    private static final int PERMISSION_STEP_OVERLAY = 2;
    private static final int PERMISSION_STEP_COUNT = 3;

    private boolean permissionWalkActive;
    private final Set<Integer> permissionWalkAttempted = new HashSet<>();

    private RecyclerView logRecyclerView;
    private LogAdapter logAdapter;

    // 添加时间戳和延迟常量
    private static final long MONITOR_INIT_DELAY = 3000; // 5秒延迟
    private long lastCheckTime = 0;

    Button settingsBtn;
    Button screenOffBtn;
    Button touchScreenBtn;
    Button exitBtn;
    TextView mirrorStatus;
    TextView mirrorStatusDetail;
    View actionSection;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            State.resumeJob();
        } else {
            State.log("未知权限请求代码: " + requestCode);
        }
    }

    private void onRequestShizukuPermissionsResult(int requestCode, int grantResult) {
        if (requestCode == AcquireShizuku.SHIZUKU_PERMISSION_REQUEST_CODE) {
            State.log("Shizuku 权限请求结果: " + (grantResult == PackageManager.PERMISSION_GRANTED ? "已授权" : "被拒绝"));
            State.resumeJob();
        } else {
            State.log("未知 Shizuku 请求代码: " + requestCode);
        }
    }

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER =
        this::onRequestShizukuPermissionsResult;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("");
                android.util.Log.i("MainActivity", "成功添加隐藏API豁免");
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "添加隐藏API豁免失败: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiTheme.apply(this);
        // 设置 State.currentActivity 为当前的 MainActivity 实例
        State.setCurrentActivity(this);

        // 添加保持屏幕常亮的标志
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 获取 DoNotAutoStartMoonlight 参数
        boolean doNotAutoStartMoonlight = getIntent().getBooleanExtra("DoNotAutoStartMoonlight", false);
        if (doNotAutoStartMoonlight) {
            Pref.doNotAutoStartMoonlight = doNotAutoStartMoonlight;
        }

        AcquireShizuku.fixRootShizuku();
        if (!Pref.getDisableAccessibility()) {
            ensureAccessiblityServiceStarted();
        }

        // 检查 SunshineService 是否已经在运行，如果没有运行才启动
        if (SunshineService.instance == null) {
            startMediaProjectionService();
        } else {
            State.log("SunshineService 服务已在运行");
        }
        
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);

        // 移除默认的 ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        // 初始化日志列表
        logRecyclerView = findViewById(R.id.logRecyclerView);
        State.initLogPersistence(this);
        logAdapter = new LogAdapter(State.logs);
        logRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        logRecyclerView.setAdapter(logAdapter);


        // 观察 UI 状态变化
        State.uiState.observe(this, this::updateUI);

        // 初始化主界面控件
        initHomeControls();

        showPermissionTipIfNeeded();
    }

    /**
     * Lists the permissions streaming depends on and hands the user straight to the settings page,
     * so an unnoticed missing grant (for example RECORD_AUDIO) is caught up front. Shown on the
     * first launch of a build, and on every later launch while one of the required grants is still
     * missing. Shizuku is deliberately excluded: it is optional and has no system settings page.
     */
    private void showPermissionTipIfNeeded() {
        SharedPreferences firstRun = getSharedPreferences(FIRST_RUN_PREFS, MODE_PRIVATE);
        boolean firstLaunchOfThisBuild =
                firstRun.getInt(KEY_PERMISSION_TIP_VERSION, -1) != BuildConfig.VERSION_CODE;
        boolean requiredPermissionMissing = !isPermissionStepSatisfied(PERMISSION_STEP_MICROPHONE)
                || !isPermissionStepSatisfied(PERMISSION_STEP_ACCESSIBILITY)
                || !isPermissionStepSatisfied(PERMISSION_STEP_OVERLAY);
        if (!firstLaunchOfThisBuild && !requiredPermissionMissing) {
            return;
        }
        firstRun.edit().putInt(KEY_PERMISSION_TIP_VERSION, BuildConfig.VERSION_CODE).apply();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("开始前请先完成授权")
                .setMessage("串流依赖以下权限：\n\n"
                        + "· 麦克风：采集系统声音；未授权时串流会中断\n"
                        + "· 无障碍服务：触摸与按键控制\n"
                        + "· 悬浮窗：显示悬浮返回按钮\n"
                        + "· Shizuku（可选）：单应用投屏与熄屏控制\n\n"
                        + "关闭本提示后将打开设置页，请在“连接准备”中逐项确认授权状态。")
                .setPositiveButton("去设置", null)
                .create();
        DialogStyle.apply(dialog);
        dialog.setOnDismissListener(ignored -> {
            permissionWalkActive = true;
            continuePermissionWalk();
        });
        dialog.show();
    }

    private boolean isPermissionStepSatisfied(int step) {
        switch (step) {
            case PERMISSION_STEP_MICROPHONE:
                return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED;
            case PERMISSION_STEP_ACCESSIBILITY:
                return TouchpadAccessibilityService.isAccessibilityServiceEnabled(this);
            case PERMISSION_STEP_OVERLAY:
                return Settings.canDrawOverlays(this);
            default:
                return true;
        }
    }

    private void launchPermissionStep(int step) {
        switch (step) {
            case PERMISSION_STEP_MICROPHONE:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                break;
            case PERMISSION_STEP_ACCESSIBILITY:
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                break;
            case PERMISSION_STEP_OVERLAY:
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
                break;
            default:
                break;
        }
    }

    /**
     * Walks the missing permissions one at a time, sending the user to the same destination the
     * matching settings row uses. A step is only opened once, then the settings page follows.
     */
    private void continuePermissionWalk() {
        if (!permissionWalkActive) {
            return;
        }
        for (int step = 0; step < PERMISSION_STEP_COUNT; step++) {
            if (permissionWalkAttempted.contains(step) || isPermissionStepSatisfied(step)) {
                continue;
            }
            permissionWalkAttempted.add(step);
            State.log("权限引导：跳转未授权项 " + (step + 1));
            launchPermissionStep(step);
            return;
        }
        permissionWalkActive = false;
        startActivity(new Intent(this, MirrorSettingsActivity.class));
    }

    private void ensureAccessiblityServiceStarted() {
        if (TouchpadAccessibilityService.isAccessibilityServiceEnabled(this)) {
            Intent serviceIntent = new Intent(this, TouchpadAccessibilityService.class);
            this.startService(serviceIntent);
        }
    }

    // 添加初始化主界面控件的方法
    private void initHomeControls() {
        settingsBtn = findViewById(R.id.settingsBtn);
        screenOffBtn = findViewById(R.id.screenOffBtn);
        touchScreenBtn = findViewById(R.id.touchScreenBtn);
        exitBtn = findViewById(R.id.exitBtn);
        mirrorStatus = findViewById(R.id.mirrorStatus);
        mirrorStatusDetail = findViewById(R.id.mirrorStatusDetail);
        actionSection = findViewById(R.id.actionSection);
        
        // 获取设置
        
        refresh();

        // 设置按钮点击事件
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MirrorSettingsActivity.class);
            startActivity(intent);
        });

        screenOffBtn.setOnClickListener(v -> {
            CreateVirtualDisplay.doPowerOffScreen(this);
        });

        touchScreenBtn.setOnClickListener(v -> {
            boolean useTouchscreen = Pref.getUseTouchscreen();
            if (ShizukuUtils.hasPermission() && useTouchscreen) {
                VirtualDisplay virtualDisplay = State.displaylinkState.getVirtualDisplay();
                if (virtualDisplay == null) {
                    virtualDisplay = State.mirrorVirtualDisplay;
                }
                if (virtualDisplay == null) {
                    return;
                }
                int displayId = virtualDisplay.getDisplay().getDisplayId();
                Intent intent = new Intent(this, TouchscreenActivity.class);
                intent.putExtra("surface", virtualDisplay.getSurface());
                intent.putExtra("display", displayId);
                startActivity(intent);
            } else {
                TouchpadActivity.startTouchpad(this, State.lastSingleAppDisplay, false);
            }
        });

        exitBtn.setOnClickListener(v -> {
            if (AutoRotateAndScaleForDisplaylink.instance != null) {
                AutoRotateAndScaleForDisplaylink.instance.release();
            }
            ExitAll.execute(this, false);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        State.setCurrentActivity(this);
        refresh();
        continuePermissionWalk();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        State.setCurrentActivity(null);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
       
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                State.log("用户授予了投屏权限");
                lastCheckTime = System.currentTimeMillis(); // 记录时间戳
                if (SunshineService.instance == null) {
                    Intent sunshineServiceIntent = new Intent(this, SunshineService.class);
                    sunshineServiceIntent.putExtra("data", data);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(sunshineServiceIntent);
                    } else {
                        startService(sunshineServiceIntent);
                    }
                    State.log("启动 SunshineService 服务");
                } else {
                    MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    State.setMediaProjection(mediaProjectionManager.getMediaProjection(RESULT_OK, data));
                    State.getMediaProjection().registerCallback(new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
                            super.onStop();
                            State.log("MediaProjection onStop 回调");
                        }
                    }, null);
                    State.resumeJob();
                }
            } else {
                State.log("用户拒绝了投屏权限");
                refresh();
                State.resumeJob();
            }
        }
    }

    // 添加一个方法来检查服务是否在运行
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    // 更新日志列表的方法
    public void updateLogs() {
        try {
            if (logAdapter != null) {
                logAdapter.notifyDataSetChanged();
                logRecyclerView.scrollToPosition(logAdapter.getItemCount() - 1);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public void startMediaProjectionService() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null) {
            Intent captureIntent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                captureIntent = mediaProjectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay());
            } else {
                captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            }
            MirrorMainActivity mirrorMainActivity = State.getCurrentActivity();
            if (mirrorMainActivity != null) {
                mirrorMainActivity.startActivityForResult(captureIntent, MirrorMainActivity.REQUEST_CODE_MEDIA_PROJECTION);
                TouchpadAccessibilityService.grantPermissionByClick(mirrorMainActivity);
            }
        } else {
            throw new RuntimeException("无法获取 MediaProjectionManager 服务");
        }
    }

    // 修改 updateUI 方法
    private void updateUI(MirrorUiState state) {
        if (state.errorStatusText != null) {
            mirrorStatus.setText("需要处理");
            mirrorStatusDetail.setText(state.errorStatusText);
            settingsBtn.setVisibility(View.VISIBLE);
            screenOffBtn.setVisibility(View.GONE);
            touchScreenBtn.setVisibility(View.GONE);
            actionSection.setVisibility(View.GONE);
            return;
        }
        // 根据状态设置 UI
        mirrorStatus.setText(state.mirrorStatusText);
        mirrorStatusDetail.setText(state.mirrorStatusDetailText);
        mirrorStatus.setVisibility(View.VISIBLE);
        
        settingsBtn.setVisibility(state.settingsBtnVisibility ? View.VISIBLE : View.GONE);
        screenOffBtn.setText("熄屏");
        screenOffBtn.setVisibility(state.screenOffBtnVisibility ? View.VISIBLE : View.GONE);
        touchScreenBtn.setVisibility(state.touchScreenBtnVisibility ? View.VISIBLE : View.GONE);
        actionSection.setVisibility(state.screenOffBtnVisibility || state.touchScreenBtnVisibility
                ? View.VISIBLE : View.GONE);
        
        if (state.touchScreenBtnVisibility) {
            touchScreenBtn.setText(state.touchScreenBtnText);
        }
    }

    // 修改 refresh 方法
    public void refresh() {
        if (State.uiState.getValue().errorStatusText != null) {
            return;
        }
        boolean singleAppMode = Pref.getSingleAppMode();
        boolean useTouchscreen = Pref.getUseTouchscreen();
        
        // 更新 ViewModel 中的状态
        boolean isScreenMirroring = State.mirrorVirtualDisplay != null || 
                                   State.displaylinkState.getVirtualDisplay() != null || 
                                   State.lastSingleAppDisplay != 0;

        MirrorUiState newUiState = new MirrorUiState();

        if (SunshineService.instance == null) {
            newUiState.mirrorStatusText = "等待投屏授权";
            newUiState.mirrorStatusDetailText = "尚未获得投屏权限。请授予权限；如无法继续，可使用下方退出按钮重试。";
            newUiState.settingsBtnVisibility = true;
            newUiState.screenOffBtnVisibility = false;
            newUiState.touchScreenBtnVisibility = false;
        } else if (isScreenMirroring) {
            newUiState.mirrorStatusText = "正在投屏";
            newUiState.mirrorStatusDetailText = "建议在系统设置中关闭屏易连省电限制，并锁定任务。触摸控制可能需要 Shizuku 权限。";
            newUiState.settingsBtnVisibility = false;
            newUiState.screenOffBtnVisibility = ShizukuUtils.hasPermission();
            newUiState.touchScreenBtnVisibility = singleAppMode;

            if (singleAppMode) {
                newUiState.touchScreenBtnText = useTouchscreen ? "触摸屏" : "触控板";
            }
        } else {
            newUiState.mirrorStatusText = "等待连接";
            newUiState.mirrorStatusDetailText = "连接显示器，或使用 Moonlight 无线投屏。USB 2.0 手机需要 DisplayLink 扩展坞。"
                    + "如遇串流连接后崩溃，请对照设置页对应内容检查权限授权状态。";
            try {
                for (String ip : SunshineService.getAllWifiIpAddresses(this)) {
                    newUiState.mirrorStatusDetailText += "\n" + ip;
                }
            } catch(Throwable e) {
                // ignore
            }
            newUiState.settingsBtnVisibility = true;
            newUiState.screenOffBtnVisibility = false;
            newUiState.touchScreenBtnVisibility = false;
        }

        State.uiState.setValue(newUiState);
    }
}
