package com.example.NetSpeedShow;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private SharedPreferences sharedPreferences;
    public static final String PREFS_NAME = "AppSettings";
    int n = 0;//计数器

    ToggleButton networkSpeedButton;
    ToggleButton button2;
    ToggleButton netStandButton;
    View viewBg;

    int color1;
    int color2;
    boolean isShowedFloatingWindow = false;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchTotalNetSpeed;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchDoubleNetSpeed;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchMemoryUsage;
    private SeekBar seekBarSize;
    private SeekBar seekBarColor;
    private SeekBar seekBarOpacity;
    private SeekBar seekBarBackgroundColor;
    private FloatingWindowService myService;
    private boolean isBound = false;
    private final ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Log.d("服务绑定", "开始");
            FloatingWindowService.LocalBinder binder = (FloatingWindowService.LocalBinder) service;
            myService = binder.getService();
            // 标记为已绑定
            isBound = true;
            InitSeekBar();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            isBound = false; // 标记为未绑定
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("启动", "生命周期1");
        color1 = ContextCompat.getColor(this, R.color.button_on);
        color2 = ContextCompat.getColor(this, R.color.button_off);
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 获取视图和设置监听器
        initViews();
        showFloatingWindowSettings();
    }

    @Override
    protected void onResume()
    {
        Log.d("启动", "生命周期2");
        super.onResume();
        restoreSettings();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initViews()
    {
        Log.d("初始化", "视图及设置点击事件");
        //初始化视图及设置点击事件
        networkSpeedButton = findViewById(R.id.toggle_networkSpeed);
        button2 = findViewById(R.id.toggle_reset);
        netStandButton = findViewById(R.id.toggle_netStand);
        viewBg = findViewById(R.id.view_bg);

        networkSpeedButton.setOnClickListener(this);
        button2.setOnClickListener(this);
        netStandButton.setOnClickListener(this);

        //为view添加触摸事件
        viewBg.setOnTouchListener((v, event) -> true); // 消费所有触摸事件

        // 定义成员变量
        seekBarSize = findViewById(R.id.seekBar_size);
        seekBarColor = findViewById(R.id.seekBar_color);
        seekBarOpacity = findViewById(R.id.seekBar_opacity);
        seekBarBackgroundColor = findViewById(R.id.seekBar_backgroundColor);

        // 获取开关按钮
        switchTotalNetSpeed = findViewById(R.id.switch_totalNetSpeed);
        switchDoubleNetSpeed = findViewById(R.id.switch_doubleNetSpeed);
        switchMemoryUsage = findViewById(R.id.switch_memoryUsage);
    }

    //初始化seekBar
    private void InitSeekBar()
    {
        Log.d("初始化", "监听器");
        // 设置共用的 SeekBar 监听器
        seekBarSize.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarColor.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarOpacity.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarBackgroundColor.setOnSeekBarChangeListener(seekBarChangeListener);
        // 设置开关监听器
        switchTotalNetSpeed.setOnCheckedChangeListener((buttonView, isChecked) -> updateTotalNetSpeed());

        switchDoubleNetSpeed.setOnCheckedChangeListener((buttonView, isChecked) -> updateDoubleNetSpeed());

        switchMemoryUsage.setOnCheckedChangeListener((buttonView, isChecked) -> updateMemoryUsage());
        n = 1;
    }

    //发送Switch改变信息
    private void updateTotalNetSpeed()
    {

        if (n == 1)
        {
            myService.changeTotalNetSpeedVisible(switchTotalNetSpeed.isChecked());
        }
    }
    private void updateDoubleNetSpeed()
    {
        if (n == 1)
        {
            myService.changeDoubleNetSpeedVisible(switchDoubleNetSpeed.isChecked());
        }
    }
    private void updateMemoryUsage()
    {
        if (n == 1)
        {
            myService.changeMemoryUsageVisible(switchMemoryUsage.isChecked());
        }
    }

    //控制显示文本属性
    private final SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener()
    {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
            Log.d("滑动条", "改变");
            if (seekBar.getId() == R.id.seekBar_size && n == 1)
            {
                // 改变 TextView 字体大小
                myService.changeTextSize(progress);
            } else if (seekBar.getId() == R.id.seekBar_color)
            {
                myService.changeTextColor(progress);
            } else if (seekBar.getId() == R.id.seekBar_opacity)
            {
                // 改变 TextView 透明度
                myService.changeTextAlpha(progress);
            } else if (seekBar.getId() == R.id.seekBar_backgroundColor)
            {
                // 改变背景颜色
                myService.changeParentColor(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar)
        {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar)
        {
        }
    };


    //点击事件
    @Override
    public void onClick(View v)
    {
        ToggleButton tb = (ToggleButton) v;
        if (tb.isChecked())
        {
            offToOn(tb);
        } else
        {
            onToOff(tb);
        }
        if (tb.getId() == R.id.toggle_networkSpeed)
        {
            saveSettings();
            if (tb.isChecked())
            {
                // 检查悬浮窗权限
                if (isDrawOverlaysAllowed())
                {
                    // 权限已被授予，直接启用悬浮窗
                    isShowedFloatingWindow = true;
                    startFloatingWindowService();
                } else
                {
                    // 权限未被授予，引导用户到设置界面
                    showPermissionSettingsDialog(tb);
                }
            } else
            {
                Log.d("isShowFloatingWindow", String.valueOf(isShowedFloatingWindow));
                //如果悬浮窗已开启，关闭悬浮窗
                if (isShowedFloatingWindow)
                {
                    isShowedFloatingWindow = false;
                    stopService(new Intent(this, FloatingWindowService.class));
                }
                //断开服务绑定
                if (isBound)
                {
                    unbindService(connection);
                    isBound = false;
                }
            }
            showFloatingWindowSettings();
        } else if (tb.getId() == R.id.toggle_netStand)
        {
            toggleFloatingWindowMovement(tb);
        } else if (tb.getId() == R.id.toggle_reset)
        {
            resetSettings();
        }
    }

    //显示悬浮窗设置的方法
    private void showFloatingWindowSettings()
    {
        if (isShowedFloatingWindow)
        {
            viewBg.setVisibility(View.GONE);
        } else
        {
            viewBg.setVisibility(View.VISIBLE);
        }
    }

    //启动悬浮窗服务
    private void startFloatingWindowService()
    {
        Log.d("启动", "服务绑定");
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    //切换悬浮窗移动状态
    private void toggleFloatingWindowMovement(ToggleButton tb)
    {
        if (n == 1)
        {
            myService.changeMovable(!tb.isChecked());
        }
            sharedPreferences.edit().putBoolean("isMovable", !tb.isChecked()).apply();

    }

    //检查悬浮窗权限
    private boolean isDrawOverlaysAllowed()
    {
        return Settings.canDrawOverlays(this);
    }

    //显示权限设置对话框
    private void showPermissionSettingsDialog(ToggleButton tb)
    {
        new AlertDialog.Builder(this)
                .setTitle("权限请求")
                .setMessage("为了启用悬浮窗功能，请授予应用悬浮窗权限。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    // 设置恢复按钮到未选中状态
                    tb.setChecked(false);
                    onToOff(tb);
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    // 单击取消时，恢复按钮状态
                    tb.setChecked(false);
                    onToOff(tb);
                })
                .show();
    }

    //设置按钮状态
    private void offToOn(ToggleButton tb)
    {
        tb.setTextColor(color1);
        tb.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    }

    //设置按钮状态
    private void onToOff(ToggleButton tb)
    {
        tb.setTextColor(color2);
        tb.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d("暂停", "Activity");
        saveSettings();
    }

    //保存用户设置
    private void saveSettings()
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("networkSpeedButton", networkSpeedButton.isChecked());
        editor.putBoolean("netStandButton", netStandButton.isChecked());
        editor.putBoolean("totalNetSpeed", switchTotalNetSpeed.isChecked());
        editor.putBoolean("doubleNetSpeed", switchDoubleNetSpeed.isChecked());
        editor.putBoolean("memoryUsage", switchMemoryUsage.isChecked());
        editor.putInt("size", seekBarSize.getProgress());
        editor.putInt("color", seekBarColor.getProgress());
        editor.putInt("opacity", seekBarOpacity.getProgress());
        Log.d("大小", String.valueOf(seekBarSize.getProgress()));
        Log.d("颜色", String.valueOf(seekBarColor.getProgress()));
        Log.d("透明度", String.valueOf(seekBarOpacity.getProgress()));
        editor.putInt("backgroundColor", seekBarBackgroundColor.getProgress());
        Log.d("背景颜色", String.valueOf(seekBarBackgroundColor.getProgress()));
        editor.putBoolean("isShowWin", networkSpeedButton.isChecked());
        editor.putBoolean("isMoveable", !netStandButton.isChecked());
        editor.putBoolean("isBind", isBound);
        editor.apply(); // 提交变化

    }

    //恢复设置的方法
    private void restoreSettings()
    {
        Log.d("恢复", "设置");
        // 恢复状态
        boolean totalNetSpeed = sharedPreferences.getBoolean("totalNetSpeed", true);
        boolean doubleNetSpeed = sharedPreferences.getBoolean("doubleNetSpeed", true);
        boolean memoryUsage = sharedPreferences.getBoolean("memoryUsage", true);
        switchTotalNetSpeed.setChecked(totalNetSpeed);
        switchDoubleNetSpeed.setChecked(doubleNetSpeed);
        switchMemoryUsage.setChecked(memoryUsage);
        seekBarSize.setProgress(sharedPreferences.getInt("size", 30));
        seekBarColor.setProgress(sharedPreferences.getInt("color", 0));
        seekBarOpacity.setProgress(sharedPreferences.getInt("opacity", 100));
        seekBarBackgroundColor.setProgress(sharedPreferences.getInt("backgroundColor", 0));
        restoreButtonStatus();

    }

    //恢复Button的状态
    private void restoreButtonStatus()
    {
        Log.d("恢复", "Button的状态");
        // 恢复按钮状态
        boolean networkSpeed = sharedPreferences.getBoolean("networkSpeedButton", false);
        boolean netStand = sharedPreferences.getBoolean("netStandButton", false);
        isShowedFloatingWindow = sharedPreferences.getBoolean("isShowWin", false);
        isBound = sharedPreferences.getBoolean("isBind", false);
        networkSpeedButton.setChecked(networkSpeed);
        netStandButton.setChecked(netStand);
        if (networkSpeed)
        {
            offToOn(networkSpeedButton);
            viewBg.setVisibility(View.GONE);
            //判断是否建立链接
            if (isBound)
            {
                startFloatingWindowService();
            }
            toggleFloatingWindowMovement(netStandButton);
        } else
        {
            onToOff(networkSpeedButton);
            viewBg.setVisibility(View.VISIBLE);

            if (isShowedFloatingWindow)
            {
                stopService(new Intent(this, FloatingWindowService.class));
                isShowedFloatingWindow = false;
                sharedPreferences.edit().putBoolean("isShowWin", false).apply();
            }
            if (isBound)
            {
                unbindService(connection);
                isBound = false;
                sharedPreferences.edit().putBoolean("isBind", false).apply();
            }
        }
        if (netStand)
        {
            offToOn(netStandButton);
        } else
        {
            onToOff(netStandButton);
        }

    }

    //重置设置的方法
    private void resetSettings() {
        Log.d("重置", "设置");

        // 创建一个AlertDialog来询问用户是否重置设置
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("重置设置")
                .setMessage("确定要重置所有设置吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 用户点击了“确定”，执行重置操作
                    // 断开服务绑定
                    if (isBound) {
                        unbindService(connection);
                        isBound = false;
                    }
                    if (isShowedFloatingWindow) {
                        stopService(new Intent(this, FloatingWindowService.class));
                    }
                    Log.d("resetSettings", "重置设置");
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();
                    button2.setChecked(false);
                    onToOff(button2);
                    n = 0;
                    restoreSettings();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    // 用户点击了“取消”，不执行任何操作
                    button2.setChecked(false);
                    onToOff(button2);
                })
                .create();

        // 设置点击对话框外部时的行为
        alertDialog.setOnCancelListener(dialog -> {
            // 恢复按钮状态
            button2.setChecked(false);
            onToOff(button2);
        });

        // 显示对话框
        alertDialog.show();
    }

    @Override
    protected void onStop()
    {
        Log.d("停止", "Activity");
        super.onStop();
        saveSettings();
    }

    //销毁
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d("销毁", "Activity");
        //断开服务绑定
        if (isBound)
        {
            unbindService(connection);
            isBound = false;
        }
    }
}