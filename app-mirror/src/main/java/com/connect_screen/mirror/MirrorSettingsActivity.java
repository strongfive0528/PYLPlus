package com.connect_screen.mirror;

import android.content.Context;
import android.Manifest;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.connect_screen.mirror.job.AcquireShizuku;
import com.connect_screen.mirror.job.ConnectToClient;
import com.connect_screen.mirror.job.ExitAll;
import com.connect_screen.mirror.job.SunshineServer;
import com.connect_screen.mirror.shizuku.PermissionManager;
import com.connect_screen.mirror.shizuku.ShizukuUtils;

import java.util.ArrayList;
import java.util.List;

public class MirrorSettingsActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    public static final String PREF_NAME = "mirror_settings";
    private static final int REQUEST_MICROPHONE_PERMISSION = 2001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiTheme.apply(this);
        setContentView(R.layout.activity_mirror_settings);
        preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        setupAppearanceControls();

        // 两个入口共用同一套确认与重启逻辑：顶部的提示卡片按钮，以及常驻右下角的圆形按钮。
        findViewById(R.id.applySettingsButton).setOnClickListener(v -> confirmApplySettingsAndRestart());
        findViewById(R.id.applySettingsFab).setOnClickListener(v -> confirmApplySettingsAndRestart());
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("屏易连设置");
        }
        
        // 初始化视图和设置监听器
        SwitchCompat singleAppModeCheckbox = findViewById(R.id.singleAppModeCheckbox);
        Button selectAppButton = findViewById(R.id.selectAppButton);
        View singleAppContainer = findViewById(R.id.singleAppContainer);
        SwitchCompat autoRotateCheckbox = findViewById(R.id.autoRotateCheckbox);
        Spinner autoScaleModeSpinner = findViewById(R.id.autoScaleModeSpinner);
        ArrayAdapter<String> autoScaleModeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"强制不缩放", "自动缩放", "强制缩放"});
        autoScaleModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoScaleModeSpinner.setAdapter(autoScaleModeAdapter);
        autoScaleModeSpinner.setSelection(Pref.getAutoScaleMode());
        autoScaleModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                preferences.edit().putInt(Pref.KEY_AUTO_SCALE_MODE, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        EditText dpiEditText = findViewById(R.id.dpiEditText);
        LinearLayout dpiLayout = findViewById(R.id.dpiLayout);
        Spinner floatingButtonModeSpinner = findViewById(R.id.floatingButtonModeSpinner);
        ArrayAdapter<String> floatingButtonModeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"单应用投屏下始终显示悬浮按钮", "单应用投屏下自动隐藏悬浮按钮", "单应用投屏下始终隐藏悬浮按钮"});
        floatingButtonModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        floatingButtonModeSpinner.setAdapter(floatingButtonModeAdapter);
        floatingButtonModeSpinner.setSelection(Pref.getFloatingButtonMode());
        floatingButtonModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                preferences.edit().putInt(Pref.KEY_FLOATING_BUTTON_MODE, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        SwitchCompat autoScreenOffCheckbox = findViewById(R.id.autoScreenOffCheckbox);
        Spinner gamepadTriggerModeSpinner = findViewById(R.id.gamepadTriggerModeSpinner);
        ArrayAdapter<String> triggerModeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"开关触发（兼容优先）", "线性扳机（保留力度）", "同时输出（按键 + 力度）"});
        triggerModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gamepadTriggerModeSpinner.setAdapter(triggerModeAdapter);
        gamepadTriggerModeSpinner.setSelection(Pref.getGamepadTriggerMode());
        gamepadTriggerModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                preferences.edit().putInt(Pref.KEY_GAMEPAD_TRIGGER_MODE, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        SwitchCompat audioOverrideCheckbox = findViewById(R.id.audioOverrideCheckbox);
        findViewById(R.id.gamepadRemapButton).setOnClickListener(v ->
                startActivity(new Intent(this, GamepadRemapActivity.class)));
        Spinner audioDestinationSpinner = findViewById(R.id.audioDestinationSpinner);
        ArrayAdapter<String> audioDestinationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"始终在 Moonlight 客户端播放", "始终在手机上播放"});
        audioDestinationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        audioDestinationSpinner.setAdapter(audioDestinationAdapter);
        int audioPlaybackMode = Pref.getAudioPlaybackMode();
        audioDestinationSpinner.setSelection(audioPlaybackMode == Pref.AUDIO_PLAYBACK_PHONE ? 1 : 0);
        audioOverrideCheckbox.setChecked(audioPlaybackMode != Pref.AUDIO_PLAYBACK_CLIENT_REQUEST);
        audioDestinationSpinner.setVisibility(audioOverrideCheckbox.isChecked() ? View.VISIBLE : View.GONE);
        audioOverrideCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked
                    ? (audioDestinationSpinner.getSelectedItemPosition() == 1
                        ? Pref.AUDIO_PLAYBACK_PHONE : Pref.AUDIO_PLAYBACK_CLIENT)
                    : Pref.AUDIO_PLAYBACK_CLIENT_REQUEST;
            preferences.edit().putInt(Pref.KEY_AUDIO_PLAYBACK_MODE, mode).apply();
            audioDestinationSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        audioDestinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (audioOverrideCheckbox.isChecked()) {
                    preferences.edit().putInt(Pref.KEY_AUDIO_PLAYBACK_MODE,
                            position == 1 ? Pref.AUDIO_PLAYBACK_PHONE : Pref.AUDIO_PLAYBACK_CLIENT).apply();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        SwitchCompat autoBindInputCheckbox = findViewById(R.id.autoBindInputCheckbox);
        SwitchCompat autoMoveImeCheckbox = findViewById(R.id.autoMoveImeCheckbox);
        SwitchCompat disableUsbAudioCheckbox = findViewById(R.id.disableUsbAudioCheckbox);
        SwitchCompat useTouchscreenCheckbox = findViewById(R.id.useTouchscreenCheckbox);
        SwitchCompat autoMatchAspectRatioCheckbox = findViewById(R.id.autoMatchAspectRatioCheckbox);
        SwitchCompat showFloatingInMirrorModeCheckbox = findViewById(R.id.showFloatingInMirrorModeCheckbox);
        SwitchCompat autoConnectClientCheckbox = findViewById(R.id.autoConnectClientCheckbox);
        LinearLayout clientConnectionContainer = findViewById(R.id.clientConnectionContainer);
        Spinner clientSpinner = findViewById(R.id.clientSpinner);
        Button connectClientButton = findViewById(R.id.connectClientButton);
        SwitchCompat useBlackImageCheckbox = findViewById(R.id.useBlackImageCheckbox);
        SwitchCompat preventAutoLockCheckbox = findViewById(R.id.preventAutoLockCheckbox);
        Spinner audioCaptureMethodSpinner = findViewById(R.id.audioCaptureMethodSpinner);
        ArrayAdapter<String> audioCaptureMethodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"自动", "强制使用 REMOTE_SUBMIX（需 Shizuku）", "强制使用系统内录（需麦克风权限）"});
        audioCaptureMethodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        audioCaptureMethodSpinner.setAdapter(audioCaptureMethodAdapter);
        audioCaptureMethodSpinner.setSelection(Pref.getAudioCaptureMethod());
        audioCaptureMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                preferences.edit().putInt(Pref.KEY_AUDIO_CAPTURE_METHOD, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        // 加载保存的设置
        boolean singleAppMode = Pref.getSingleAppMode();
        boolean autoRotate = Pref.getAutoRotate();
        int singleAppDpi = Pref.getSingleAppDpi();
        boolean autoScreenOff = Pref.getAutoScreenOff();
        boolean autoBindInput = Pref.getAutoBindInput();
        boolean autoMoveIme = Pref.getAutoMoveIme();
        boolean disableUsbAudio = Pref.getDisableUsbAudio();
        boolean useTouchscreen = Pref.getUseTouchscreen();
        boolean autoMatchAspectRatio = Pref.getAutoMatchAspectRatio();
        boolean showFloatingInMirrorMode = Pref.getShowFloatingInMirrorMode();
        boolean autoConnectClient = Pref.getAutoConnectClient();
        boolean useBlackImage = Pref.getUseBlackImage();
        boolean preventAutoLock = Pref.getPreventAutoLock();
        
        singleAppModeCheckbox.setChecked(singleAppMode);
        autoRotateCheckbox.setChecked(autoRotate);
        dpiEditText.setText(String.valueOf(singleAppDpi));
        autoScreenOffCheckbox.setChecked(autoScreenOff);
        autoBindInputCheckbox.setChecked(autoBindInput);
        autoMoveImeCheckbox.setChecked(autoMoveIme);
        disableUsbAudioCheckbox.setChecked(disableUsbAudio);
        useTouchscreenCheckbox.setChecked(useTouchscreen);
        autoMatchAspectRatioCheckbox.setChecked(autoMatchAspectRatio);
        showFloatingInMirrorModeCheckbox.setChecked(showFloatingInMirrorMode);
        autoConnectClientCheckbox.setChecked(autoConnectClient);
        useBlackImageCheckbox.setChecked(useBlackImage);
        preventAutoLockCheckbox.setChecked(preventAutoLock);
        if (ShizukuUtils.hasPermission()) {
            autoScreenOffCheckbox.setText("自动熄屏（用音量键唤醒，如果无法唤醒长按电源键强制关机）");
        }

        // 显示已选择的应用名称（如果有）
        String selectedAppName = preferences.getString(Pref.KEY_SELECTED_APP_NAME, "");
        if (!selectedAppName.isEmpty() && singleAppMode) {
            singleAppModeCheckbox.setText("单应用投屏: " + selectedAppName);
        } else {
            singleAppModeCheckbox.setText("单应用投屏（可以投微软桌面这类启动器类型的应用）");
        }
        
        // 监听复选框变化
        singleAppModeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_SINGLE_APP_MODE, isChecked).apply();
            autoScaleModeSpinner.setEnabled(!isChecked);
            autoMatchAspectRatioCheckbox.setEnabled(!isChecked);
            showFloatingInMirrorModeCheckbox.setEnabled(!isChecked);
            singleAppContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            
            if (!isChecked) {
                singleAppModeCheckbox.setText("单应用投屏（可以投微软桌面这类启动器类型的应用）");
            } else if (!selectedAppName.isEmpty()) {
                singleAppModeCheckbox.setText("单应用投屏: " + selectedAppName);
            }
        });
        autoMatchAspectRatioCheckbox.setEnabled(!singleAppMode);
        showFloatingInMirrorModeCheckbox.setEnabled(!singleAppMode);
        
        autoRotateCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_AUTO_ROTATE, isChecked).apply();
        });

        autoScaleModeSpinner.setEnabled(!singleAppMode);

        // 将分辨率相关控件替换为单个按钮和显示当前分辨率的文本视图
        Button resolutionButton = findViewById(R.id.resolutionButton);
        TextView currentResolutionText = findViewById(R.id.currentResolutionText);
        
        // 显示当前分辨率
        updateResolutionText(currentResolutionText);
        
        resolutionButton.setOnClickListener(v -> {
            showResolutionDialog(currentResolutionText);
        });

        // 添加关于按钮点击事件
        Button aboutButton = findViewById(R.id.aboutButton);
        aboutButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        });

        
        // 添加授权按钮
        Button shizukuPermissionBtn = findViewById(R.id.shizukuPermissionBtn);
        shizukuPermissionBtn.setOnClickListener(v -> {
            State.startNewJob(new AcquireShizuku());
        });

        // 更新Shizuku状态
        TextView shizukuStatus = findViewById(R.id.shizukuStatus);
        TextView accessibilityStatus = findViewById(R.id.accessibilityStatus);
        TextView overlayStatus = findViewById(R.id.overlayStatus);
        updateShizukuStatus(shizukuStatus, shizukuPermissionBtn);
        updateAccessibilityStatus(accessibilityStatus);
        updateOverlayStatus(overlayStatus);

        // The entire status row is an entry point; users should not have to wait
        // for a small conditional "grant" button to become visible.
        shizukuStatus.setOnClickListener(v -> State.startNewJob(new AcquireShizuku()));
        accessibilityStatus.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        overlayStatus.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        TextView microphoneStatus = findViewById(R.id.microphoneStatus);
        microphoneStatus.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()))));

        // 添加选择应用按钮点击事件
        selectAppButton.setOnClickListener(v -> {
            showAppSelectionDialog();
        });

        // 监听DPI输入变化
        dpiEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveDpiSetting(dpiEditText);
            }
        });

        // 添加DPI确认按钮
        Button dpiConfirmButton = findViewById(R.id.dpiConfirmButton);
        dpiConfirmButton.setOnClickListener(v -> {
            saveDpiSetting(dpiEditText);
        });

        // 设置单应用模式容器的可见性
        singleAppContainer.setVisibility(singleAppMode ? View.VISIBLE : View.GONE);

        // 添加自动熄屏复选框监听器
        autoScreenOffCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_AUTO_SCREEN_OFF, isChecked).apply();
        });

        // 添加自动绑定输入复选框监听器
        autoBindInputCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_AUTO_BIND_INPUT, isChecked).apply();
        });
        
        // 如果没有 Shizuku 权限
        if (!ShizukuUtils.hasPermission()) {
            autoBindInputCheckbox.setEnabled(false);
        }

        // 添加自动移动输入法复选框监听器
        autoMoveImeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_AUTO_MOVE_IME, isChecked).apply();
        });
        
        // 如果没有 Shizuku 权限，禁用该选项
        if (!ShizukuUtils.hasPermission()) {
            autoMoveImeCheckbox.setEnabled(false);
        }

        // 添加停用USB音频复选框监听器
        disableUsbAudioCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_DISABLE_USB_AUDIO, isChecked).apply();
            
            // 如果有Shizuku权限，则更新系统设置
            if (ShizukuUtils.hasPermission()) {
                if (PermissionManager.grant("android.permission.WRITE_SECURE_SETTINGS")) {
                    try {
                        Settings.Secure.putInt(getContentResolver(),
                                "usb_audio_automatic_routing_disabled", isChecked ? 1 : 0);
                    } catch (SecurityException e) {
                        State.log("failed to set usb_audio_automatic_routing_disabled: " + e);
                    }
                }
            }
        });
        
        // 如果没有Shizuku权限，禁用该选项
        if (!ShizukuUtils.hasPermission()) {
            disableUsbAudioCheckbox.setEnabled(false);
        }

        // 添加触摸屏控制复选框监听器
        useTouchscreenCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_USE_TOUCHSCREEN, isChecked).apply();
        });
        
        // 如果没有 Shizuku 权限，禁用该选项
        if (!ShizukuUtils.hasPermission()) {
            useTouchscreenCheckbox.setEnabled(false);
        }

        // 添加自动匹配宽高比复选框监听器
        autoMatchAspectRatioCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_AUTO_MATCH_ASPECT_RATIO, isChecked).apply();
        });
        
        // 如果没有 Shizuku 权限，禁用该选项
        if (!ShizukuUtils.hasPermission()) {
            autoMatchAspectRatioCheckbox.setEnabled(false);
        }

        // 添加镜像模式下显示悬浮返回键复选框监听器
        showFloatingInMirrorModeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_SHOW_FLOATING_IN_MIRROR_MODE, isChecked).apply();
        });

        // 显示或隐藏客户端连接容器
        clientConnectionContainer.setVisibility(autoConnectClient ? View.VISIBLE : View.GONE);
        
        // 设置自动连接客户端复选框监听器
        autoConnectClientCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_AUTO_CONNECT_CLIENT, isChecked).apply();
            clientConnectionContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            
            // 如果选中，加载客户端列表
            if (isChecked) {
                loadClientList(clientSpinner);
            }
        });
        
        // 如果自动连接客户端已启用，加载客户端列表
        if (autoConnectClient) {
            loadClientList(clientSpinner);
        }
        
        // 设置连接按钮点击事件
        connectClientButton.setOnClickListener(v -> {
            String selectedClient = (String) clientSpinner.getSelectedItem();
            if (selectedClient != null && !selectedClient.isEmpty()) {
                if (selectedClient.equals("手工输入")) {
                    // 显示手工输入对话框
                    showManualInputDialog();
                } else {
                    // 保存选中的客户端
                    preferences.edit().putString(Pref.KEY_SELECTED_CLIENT, selectedClient).apply();
                    int pin = (int)(Math.random() * 9000) + 1000;
                    SunshineServer.suppressPin = String.valueOf(pin);
                    ConnectToClient.connect(pin);
                }
            }
        });

        // 初始化无障碍禁用复选框
        SwitchCompat disableAccessibilityCheckbox = findViewById(R.id.disableAccessibilityCheckbox);
        boolean disableAccessibility = preferences.getBoolean(Pref.KEY_DISABLE_ACCESSIBILITY, false);
        disableAccessibilityCheckbox.setChecked(disableAccessibility);
        
        // 添加无障碍禁用复选框监听器
        disableAccessibilityCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_DISABLE_ACCESSIBILITY, isChecked).apply();
            updateAccessibilityStatus(accessibilityStatus);
            if (isChecked) {
                if (ShizukuUtils.hasPermission()) {
                    TouchpadAccessibilityService.disableAll(MirrorSettingsActivity.this);
                }
            }
        });

        // 添加使用黑色图片复选框监听器
        useBlackImageCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_USE_BLACK_IMAGE, isChecked).apply();
        });

        // 添加阻止自动锁屏复选框监听器
        preventAutoLockCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(Pref.KEY_PREVENT_AUTO_LOCK, isChecked).apply();
        });
        if (!ShizukuUtils.hasPermission()) {
            preventAutoLockCheckbox.setEnabled(false);
        }

    }

    private void setupAppearanceControls() {
        Spinner modeSpinner = findViewById(R.id.appearanceModeSpinner);
        String[] modeValues = {UiTheme.MODE_SYSTEM, UiTheme.MODE_LIGHT, UiTheme.MODE_DARK, UiTheme.MODE_OLED};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Follow system", "Light", "Dark", "OLED black"});
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(modeAdapter);

        SharedPreferences appearance = UiTheme.prefs(this);
        modeSpinner.setSelection(indexOf(modeValues, appearance.getString(UiTheme.MODE, UiTheme.MODE_LIGHT)));
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String mode = modeValues[modeSpinner.getSelectedItemPosition()];
                if (!mode.equals(appearance.getString(UiTheme.MODE, UiTheme.MODE_LIGHT))) {
                    UiTheme.saveMode(MirrorSettingsActivity.this, mode);
                    recreate();
                }
            }
        });
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) return i;
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SunshineServer.suppressPin = null;
    }

    private void confirmApplySettingsAndRestart() {
        AlertDialog restartDialog = new AlertDialog.Builder(this)
                .setTitle("应用设置并重启？")
                .setMessage("当前串流会断开，服务和应用将重新启动；随后可能需要重新授权录屏。")
                .setNegativeButton("取消", null)
                .setPositiveButton("重启", (dialog, which) -> {
                    // Most setting listeners use apply(); force the current snapshot to disk
                    // before ExitAll terminates the process.
                    if (!preferences.edit().commit()) {
                        Toast.makeText(this, "设置保存失败，请重试", Toast.LENGTH_LONG).show();
                        return;
                    }
                    ExitAll.execute(this, true);
                })
                .create();
        DialogStyle.apply(restartDialog);
        restartDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // 更新权限状态
        TextView shizukuStatus = findViewById(R.id.shizukuStatus);
        Button shizukuPermissionBtn = findViewById(R.id.shizukuPermissionBtn);
        TextView accessibilityStatus = findViewById(R.id.accessibilityStatus);
        TextView overlayStatus = findViewById(R.id.overlayStatus);
        TextView microphoneStatus = findViewById(R.id.microphoneStatus);
        
        updateShizukuStatus(shizukuStatus, shizukuPermissionBtn);
        updateAccessibilityStatus(accessibilityStatus);
        updateOverlayStatus(overlayStatus);
        updateMicrophoneStatus(microphoneStatus);

        // 更新无障碍状态显示
        SwitchCompat disableAccessibilityCheckbox = findViewById(R.id.disableAccessibilityCheckbox);
        boolean disableAccessibility = preferences.getBoolean(Pref.KEY_DISABLE_ACCESSIBILITY, false);
        disableAccessibilityCheckbox.setChecked(disableAccessibility);
    }

    private void updateShizukuStatus(TextView statusView, Button permissionBtn) {
        boolean started = ShizukuUtils.hasShizukuStarted();
        boolean hasPermission = ShizukuUtils.hasPermission();
        
        String status;
        if (!started) {
            status = "未启动";
            permissionBtn.setVisibility(View.GONE);
        } else if (!hasPermission) {
            status = "已启动未授权";
            permissionBtn.setVisibility(View.VISIBLE);
        } else {
            status = "已授权";
            permissionBtn.setVisibility(View.GONE);
        }
        
        statusView.setText(status);
    }

    private void updateAccessibilityStatus(TextView statusView) {
        boolean isEnabled = TouchpadAccessibilityService.isAccessibilityServiceEnabled(this);
        boolean isDisabled = preferences.getBoolean(Pref.KEY_DISABLE_ACCESSIBILITY, false);
        
        if (isDisabled) {
            statusView.setText("已禁用");
        } else {
            statusView.setText(isEnabled ? "已授权" : "未授权");
        }
        
        // 获取或创建授权按钮
        View parent = (View) statusView.getParent();
        Button accessibilityPermissionBtn = parent.findViewById(R.id.accessibilityPermissionBtn);
        
        // 根据授权状态显示或隐藏按钮
        if (accessibilityPermissionBtn != null) {
            accessibilityPermissionBtn.setVisibility((isEnabled || isDisabled) ? View.GONE : View.VISIBLE);
            accessibilityPermissionBtn.setOnClickListener(v -> {
                // 跳转到系统无障碍设置页面
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            });
        }
    }

    private void updateOverlayStatus(TextView statusView) {
        boolean hasPermission = Settings.canDrawOverlays(this);
        statusView.setText(hasPermission ? "已授权" : "未授权");
        
        // 获取或创建授权按钮
        View parent = (View) statusView.getParent();
        Button overlayPermissionBtn = parent.findViewById(R.id.overlayPermissionBtn);
        
        // 根据授权状态显示或隐藏按钮
        if (overlayPermissionBtn != null) {
            overlayPermissionBtn.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
            overlayPermissionBtn.setOnClickListener(v -> {
                // 跳转到悬浮窗权限设置页面
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        }
    }

    private void updateMicrophoneStatus(TextView statusView) {
        if (statusView == null) {
            return;
        }
        boolean hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        statusView.setText(hasPermission ? "已授权" : "未授权");

        // 获取或创建授权按钮
        View parent = (View) statusView.getParent();
        Button microphonePermissionBtn = parent.findViewById(R.id.microphonePermissionBtn);

        // 根据授权状态显示或隐藏按钮
        if (microphonePermissionBtn != null) {
            microphonePermissionBtn.setVisibility(hasPermission ? View.GONE : View.VISIBLE);
            microphonePermissionBtn.setOnClickListener(v -> ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE_PERMISSION));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE_PERMISSION) {
            updateMicrophoneStatus(findViewById(R.id.microphoneStatus));
        }
    }

    private void showAppSelectionDialog() {
        // 获取所有桌面应用
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launcherApps = pm.queryIntentActivities(intent, 0);
        
        // 按应用名称排序
        launcherApps.sort((a, b) -> {
            String labelA = a.loadLabel(pm).toString();
            String labelB = b.loadLabel(pm).toString();
            return labelA.compareToIgnoreCase(labelB);
        });
        
        // 创建自定义适配器来显示应用图标和名称
        AppListAdapter adapter = new AppListAdapter(this, launcherApps, pm);
        
        // 创建并显示选择对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请固定设置为微软桌面，ATV Launcher这类应用，一劳永逸");
        builder.setAdapter(adapter, (dialog, which) -> {
            ResolveInfo selectedApp = launcherApps.get(which);
            String selectedPackage = selectedApp.activityInfo.packageName;
            String selectedName = selectedApp.loadLabel(pm).toString();
            
            // 保存选择的应用
            preferences.edit()
                    .putString(Pref.KEY_SELECTED_APP_PACKAGE, selectedPackage)
                    .putString(Pref.KEY_SELECTED_APP_NAME, selectedName)
                    .apply();
            
            // 更新UI显示
            SwitchCompat singleAppModeCheckbox = findViewById(R.id.singleAppModeCheckbox);
            singleAppModeCheckbox.setText("单应用投屏: " + selectedName);
        });
        
        AlertDialog appPickerDialog = builder.create();
        DialogStyle.apply(appPickerDialog);
        appPickerDialog.show();
    }

    // 自定义适配器用于显示应用图标和名称
    private static class AppListAdapter extends ArrayAdapter<ResolveInfo> {
        private final PackageManager pm;
        private final int ICON_SIZE_DP = 36; // 统一图标大小为36dp
        
        public AppListAdapter(Context context, List<ResolveInfo> apps, PackageManager pm) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, apps);
            this.pm = pm;
        }
        
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        android.R.layout.simple_list_item_2, parent, false);
            }
            
            ResolveInfo app = getItem(position);
            if (app != null) {
                TextView text1 = convertView.findViewById(android.R.id.text1);
                TextView text2 = convertView.findViewById(android.R.id.text2);
                
                text1.setText(app.loadLabel(pm));
                text2.setText(app.activityInfo.packageName);
                
                // 设置应用图标并统一大小
                try {
                    // 获取图标
                    android.graphics.drawable.Drawable icon = app.loadIcon(pm);
                    
                    // 计算图标大小（dp转px）
                    float density = getContext().getResources().getDisplayMetrics().density;
                    int iconSizePx = Math.round(ICON_SIZE_DP * density);
                    
                    // 设置图标大小
                    icon.setBounds(0, 0, iconSizePx, iconSizePx);
                    
                    // 设置图标到TextView
                    text1.setCompoundDrawables(icon, null, null, null);
                    text1.setCompoundDrawablePadding(10);
                } catch (Exception e) {
                    // 如果加载图标失败，忽略错误
                }
            }
            
            return convertView;
        }
    }

    private void saveDpiSetting(EditText dpiEditText) {
        try {
            int dpi = Integer.parseInt(dpiEditText.getText().toString());
            // 限制DPI的合理范围，例如60-600
            if (dpi < 60) dpi = 60;
            if (dpi > 600) dpi = 600;
            dpiEditText.setText(String.valueOf(dpi)); // 更新显示值
            preferences.edit().putInt(Pref.KEY_SINGLE_APP_DPI, dpi).apply(); // 保存DPI设置
        } catch (NumberFormatException e) {
            // 如果输入无效，恢复为默认值或上次保存的值
            dpiEditText.setText(Pref.getSingleAppDpi());
        }
    }

    private void updateResolutionText(TextView textView) {
        String resolutionText = String.format("Displaylink 输出: %dx%d@%dHz", 
                Pref.getDisplaylinkWidth(), 
                Pref.getDisplaylinkHeight(),
                Pref.getDisplaylinkRefreshRate());
        textView.setText(resolutionText);
    }

    private void showResolutionDialog(TextView currentResolutionText) {
        // 创建对话框布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_resolution_settings, null);
        
        // 获取对话框中的控件
        EditText widthEditText = dialogView.findViewById(R.id.widthEditText);
        EditText heightEditText = dialogView.findViewById(R.id.heightEditText);
        EditText refreshRateEditText = dialogView.findViewById(R.id.refreshRateEditText);
        Spinner resolutionPresetSpinner = dialogView.findViewById(R.id.resolutionPresetSpinner);
        
        // 设置当前值
        widthEditText.setText(String.valueOf(Pref.getDisplaylinkWidth()));
        heightEditText.setText(String.valueOf(Pref.getDisplaylinkHeight()));
        refreshRateEditText.setText(String.valueOf(Pref.getDisplaylinkRefreshRate()));
        
        // 添加分辨率预设选项
        String[] resolutionPresets = new String[]{"快捷设置", "1080p", "1440p", "2160p", "ipad4"};
        ArrayAdapter<String> resolutionAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            resolutionPresets
        );
        resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionPresetSpinner.setAdapter(resolutionAdapter);
        
        // 设置预设选项的监听器
        resolutionPresetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    switch (position) {
                        case 1: // 1080p
                            widthEditText.setText("1920");
                            heightEditText.setText("1080");
                            refreshRateEditText.setText("60");
                            break;
                        case 2: // 1440p
                            widthEditText.setText("2560");
                            heightEditText.setText("1440");
                            refreshRateEditText.setText("60");
                            break;
                        case 3: // 2160p
                            widthEditText.setText("3840");
                            heightEditText.setText("2160");
                            refreshRateEditText.setText("60");
                            break;
                        case 4: // ipad4
                            widthEditText.setText("2048");
                            heightEditText.setText("1536");
                            refreshRateEditText.setText("60");
                            break;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做任何操作
            }
        });
        
        // 创建并显示对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Displaylink 输出分辨率");
        builder.setView(dialogView);
        builder.setPositiveButton("确定", (dialog, which) -> {
            try {
                int width = Integer.parseInt(widthEditText.getText().toString());
                int height = Integer.parseInt(heightEditText.getText().toString());
                int refreshRate = Integer.parseInt(refreshRateEditText.getText().toString());
                
                // 限制刷新率范围在24-240之间
                refreshRate = Math.max(24, Math.min(240, refreshRate));
                
                preferences.edit()
                        .putInt(Pref.KEY_DISPLAYLINK_WIDTH, width)
                        .putInt(Pref.KEY_DISPLAYLINK_HEIGHT, height)
                        .putInt(Pref.KEY_DISPLAYLINK_REFRESH_RATE, refreshRate)
                        .apply();
                
                // 更新显示的分辨率文本
                updateResolutionText(currentResolutionText);
            } catch (NumberFormatException e) {
                // ignore
            }
        });
        builder.setNegativeButton("取消", null);
        AlertDialog resolutionDialog = builder.create();
        DialogStyle.apply(resolutionDialog);
        resolutionDialog.show();
    }

    // 加载客户端列表
    private void loadClientList(Spinner spinner) {
        // 设置默认选中项
        String selectedClient = Pref.getSelectedClient();
        // 示例数据
        List<String> clients = new ArrayList<>();
        clients.add("手工输入");
        if (!selectedClient.isEmpty()) {
            clients.add(selectedClient);
        }
        clients.addAll(State.discoveredConnectScreenClients);
        
        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            clients
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (!selectedClient.isEmpty()) {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).equals(selectedClient)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    // 显示手工输入对话框
    private void showManualInputDialog() {
        // 创建对话框布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_client_input, null);
        
        // 获取对话框中的控件
        EditText ipEditText = dialogView.findViewById(R.id.ipEditText);
        EditText portEditText = dialogView.findViewById(R.id.portEditText);
        
        // 设置默认端口
        portEditText.setText("42515");
        
        // 创建并显示对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("手工输入客户端");
        builder.setView(dialogView);
        builder.setPositiveButton("确定", (dialog, which) -> {
            try {
                String ip = ipEditText.getText().toString().trim();
                String port = portEditText.getText().toString().trim();
                
                if (!ip.isEmpty()) {
                    // 保存手工输入的客户端信息
                    String clientAddress = ip;
                    if (!port.isEmpty()) {
                        clientAddress += ":" + port;
                    }
                    preferences.edit().putString(Pref.KEY_SELECTED_CLIENT, clientAddress).apply();
                    
                    // 刷新客户端列表
                    Spinner clientSpinner = findViewById(R.id.clientSpinner);
                    loadClientList(clientSpinner);
                    int pin = (int) (Math.random() * 9000) + 1000;
                    SunshineServer.suppressPin = String.valueOf(pin);
                    ConnectToClient.connect(pin);
                }
            } catch (Exception e) {
                // 处理异常
            }
        });
        builder.setNegativeButton("取消", null);
        AlertDialog manualClientDialog = builder.create();
        DialogStyle.apply(manualClientDialog);
        manualClientDialog.show();
    }
} 
