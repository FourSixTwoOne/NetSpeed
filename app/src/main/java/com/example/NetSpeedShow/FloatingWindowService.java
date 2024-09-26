package com.example.NetSpeedShow;

import static com.example.NetSpeedShow.MainActivity.PREFS_NAME;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Locale;

public class FloatingWindowService extends Service
{
    String T = "↕";
    String D = "↓";
    String U = "↑";

    private WindowManager.LayoutParams params;

    int n = 0;
    private WindowManager windowManager;
    private View floatingView;
    private boolean isMovable = true; // 允许悬浮窗移动
    private float initialX;
    private float initialY;
    private float initialTouchX;
    private float initialTouchY;
    //更新时间间隔
    private static final double UPDATE_INTERVAL = 1.0;
    //上行速度
    TextView upSpeed;
    //下行速度
    TextView downSpeed;
    //总网速
    TextView totalSpeed;
    //memory占用率
    TextView memoryUsage;
    //父布局
    View parentView;
    private Handler handler;
    private Runnable updateRunnable;
    private long startTxBit = 0;
    private long startRxBit = 0;

    //显示隐藏悬浮窗子视图
    private boolean isTotalNetSpeedVisible = true;
    private boolean isDoubleNetSpeedVisible = true;
    private boolean isMemoryUsageVisible = true;
    private boolean isFloatingViewVisible = false;

    private SharedPreferences sharedPreferences;

    @SuppressLint("InflateParams")
    @Override
    public void onCreate()
    {
        Log.d("onCreate", "启动");
        super.onCreate();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 获取 LayoutInflater
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.floating_window_layout, null);
        // 处理触摸事件
        floatingView.setOnTouchListener(new View.OnTouchListener()
        {
            private long downTime;
            private static final int CLICK_DISTANCE = 10; // 定义一个小的移动距离来判断点击
            private boolean isDragging = false;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (isMovable)
                {
                    Log.d("状态", "触摸悬浮框");
                    switch (event.getAction())
                    {
                        case MotionEvent.ACTION_DOWN:
                            handleActionDown(event);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            handleActionMove(event, params);
                            return true;

                        case MotionEvent.ACTION_UP:
                            handleActionUp();
                            return true;
                    }
                }
                return false;
            }

            private void handleActionDown(MotionEvent event)
            {
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                downTime = System.currentTimeMillis(); // 记录按下时间
                isDragging = false; // 重置拖动状态
            }

            private void handleActionMove(MotionEvent event, WindowManager.LayoutParams params)
            {
                // 判断是否拖动
                if (Math.abs(event.getRawX() - initialTouchX) > CLICK_DISTANCE ||
                        Math.abs(event.getRawY() - initialTouchY) > CLICK_DISTANCE)
                {
                    isDragging = true; // 标记为拖动状态
                }
                params.x = (int) (initialX + (int) (event.getRawX() - initialTouchX));
                params.y = (int) (initialY + (int) (event.getRawY() - initialTouchY));
                windowManager.updateViewLayout(floatingView, params);
            }

            private void handleActionUp()
            {
                long upTime = System.currentTimeMillis(); // 获取抬起时间
                if (!isDragging && (upTime - downTime < 300))
                {
                    // 认为是点击事件，不消耗点击事件
                    Log.d("状态", "点击悬浮框");
                } else if (isDragging)
                {
                    // 当拖动结束时，保存位置到 SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("posX", params.x);
                    editor.putInt("posY", params.y);
                    editor.apply();// 应用更改
                }
            }
        });
        InitView();
        upDateWindow(); // 更新悬浮窗子视图属性

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("onStartCommand", "启动");
        update();
        // 检查浮动窗口是否已存在
        if (!isFloatingViewVisible)
        {
            Log.d("启动", "悬浮窗");
            showFloatingWindow();
            updateVisibility();
            startUpdatingMetrics();

        }

        return START_STICKY;
    }

    // 更新悬浮窗内容
    private void update()
    {
        Log.d("更新", "悬浮窗");
        isMovable = sharedPreferences.getBoolean("moveable", isMovable);
        isTotalNetSpeedVisible = sharedPreferences.getBoolean("totalNetSpeed", isTotalNetSpeedVisible);
        isDoubleNetSpeedVisible = sharedPreferences.getBoolean("doubleNetSpeed", isDoubleNetSpeedVisible);
        isMemoryUsageVisible = sharedPreferences.getBoolean("memoryUsage", isMemoryUsageVisible);
    }

    private void upDateWindow()
    {
        Log.d("更新", "悬浮窗设置");

        changeTextSize(sharedPreferences.getInt("size", 30));
        changeTextColor(sharedPreferences.getInt("color", 0));
        changeTextAlpha(sharedPreferences.getInt("opacity", 100));
        changeParentColor(sharedPreferences.getInt("backgroundColor", 0));
    }

    private void updateVisibility()
    {
        totalSpeed.setVisibility(isTotalNetSpeedVisible ? View.VISIBLE : View.GONE);
        upSpeed.setVisibility(isDoubleNetSpeedVisible ? View.VISIBLE : View.GONE);
        downSpeed.setVisibility(isDoubleNetSpeedVisible ? View.VISIBLE : View.GONE);
        memoryUsage.setVisibility(isMemoryUsageVisible ? View.VISIBLE : View.GONE);
    }

    private void showFloatingWindow()
    {
        isFloatingViewVisible = true;
        Log.d("显示", "悬浮窗");
        // 计算初始位置，使其在屏幕下方中间
        params = getLayoutParams();
        params.gravity = Gravity.TOP | Gravity.START; // 设置重力为顶部对齐
        params.x = sharedPreferences.getInt("posX", 200);
        params.y = sharedPreferences.getInt("posY", 1600);
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        windowManager.addView(floatingView, params);
    }

    private void startUpdatingMetrics()
    {
        if (floatingView == null) return;
        handler = new Handler();
        updateRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                updateMetrics();
                n = 1;
                startTxBit = TrafficStats.getTotalTxBytes();
                startRxBit = TrafficStats.getTotalRxBytes();
                handler.postDelayed(this, (long) UPDATE_INTERVAL * 1000); // 每秒更新2次
            }
        };
        handler.post(updateRunnable);
    }

    private void updateMetrics()
    {
        double uploadSpeed = getUploadSpeed();
        double downloadSpeed = getDownloadSpeed();
        String totalSpeedValue = getTotalSpeed(uploadSpeed, downloadSpeed);
        String memoryUsageValue = getMemoryUsage(this);

        // 根据状态决定是否更新文本
        if (isTotalNetSpeedVisible)
        {
            totalSpeed.setText(totalSpeedValue);
        }
        if (isDoubleNetSpeedVisible)
        {
            upSpeed.setText(formatUp(formatSpeed(uploadSpeed)));
            downSpeed.setText(formatDown(formatSpeed(downloadSpeed)));
        }
        if (isMemoryUsageVisible)
        {
            memoryUsage.setText(memoryUsageValue);
        }
    }

    private double getUploadSpeed()
    {
        if (n == 1)
        {
            return (double) (TrafficStats.getTotalTxBytes() - startTxBit) / UPDATE_INTERVAL;
        } else return 0;
    }

    private double getDownloadSpeed()
    {
        if (n == 1)
        {
            return (double) (TrafficStats.getTotalRxBytes() - startRxBit) / UPDATE_INTERVAL;
        } else return 0;
    }

    private String getTotalSpeed(double upload, double download)
    {
        String total = formatSpeed(upload + download);
        return String.format("%s:%2s", total, T);
    }

    //计算内存使用率
    public String getMemoryUsage(Context context)
    {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null)
        {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            long totalMemory = memoryInfo.totalMem; // 总内存  
            long availableMemory = memoryInfo.availMem; // 可用内存  
            long usedMemory = totalMemory - availableMemory; // 已用内存  

            return String.format(Locale.US, "%.1f%%", (float) usedMemory / totalMemory * 100);
        } else
        {
            Log.e("MemoryUsage", "Failed to get ActivityManager");
            return "N/A"; // 表示无法获取内存使用率
        }
    }

    private String formatUp(String bytes)
    {
        return String.format("%s:%2s", bytes, U);
    }

    private String formatDown(String bytes)
    {
        return String.format("%s:%2s", bytes, D);
    }

    private String formatSpeed(double bytes)
    {
        if (bytes < 102)
        {
            return String.format(Locale.US, "%.1fB/s", bytes);
        } else if (bytes < 1024 * 1024)
        {
            return String.format(Locale.US, "%.1fKB/s", bytes / 1024);
        } else
        {
            return String.format(Locale.US, "%.1fMB/s", bytes / (1024 * 1024));
        }
    }

    //***用于改变悬浮窗属性的方法***//
    //初始化悬浮窗的视图
    private void InitView()
    {
        parentView = floatingView.findViewById(R.id.layout_netShowView);
        totalSpeed = floatingView.findViewById(R.id.tv_totalSpeed);
        upSpeed = floatingView.findViewById(R.id.tv_upSpeed);
        downSpeed = floatingView.findViewById(R.id.tv_downSpeed);
        memoryUsage = floatingView.findViewById(R.id.tv_memoryUsage);
        isMovable = sharedPreferences.getBoolean("isMovable", true);
    }

    //文字大小
    public void changeTextSize(int size)
    {
        float textSize = ((float) size * 2 / 10);

        totalSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        upSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        downSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        memoryUsage.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

    }

    //切换总网速显示
    public void changeTotalNetSpeedVisible(boolean visible)
    {
        isTotalNetSpeedVisible = visible;
        totalSpeed.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    //切换双网速显示
    public void changeDoubleNetSpeedVisible(boolean visible)
    {
        isDoubleNetSpeedVisible = visible;
        upSpeed.setVisibility(visible ? View.VISIBLE : View.GONE);
        downSpeed.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    //切换内存使用率显示
    public void changeMemoryUsageVisible(boolean visible)
    {
        isMemoryUsageVisible = visible;
        memoryUsage.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    //切换悬浮窗移动状态
    public void changeMovable(boolean movable)
    {
        if (movable != isMovable)
        {
            isMovable = movable;
            if (isMovable)
            {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
            } else
            {
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
            }
            windowManager.updateViewLayout(floatingView, params);
        } else
        {
            Log.d("changeMovable", "当前状态" + isMovable);
        }
    }

    //字体颜色
    private void setTextColor(TextView textView, int progress)
    {
        if (progress < 100)
        {
            textView.setTextColor(Color.argb(255, progress * 255 / 100, 0, 0));
        } else if (progress < 200)
        {
            int adjusted = progress - 100;
            textView.setTextColor(Color.argb(255, 255 - (adjusted * 255 / 100), adjusted * 255 / 100, 0));
        } else if (progress < 300)
        {
            int adjusted = progress - 200;
            textView.setTextColor(Color.argb(255, 0, 255 - (adjusted * 255 / 100), adjusted * 255 / 100));
        } else
        {
            int adjusted = progress - 300;
            textView.setTextColor(Color.argb(255, adjusted * 255 / 100, adjusted * 255 / 100, 255));
        }
    }

    private void setBgColor(TextView textView, int progress)
    {
        int alpha = (int) (255 * 0.2);
        // 设置背景透明度
        if (progress < 10)
        {
            // 完全透明
            alpha = 0;
            textView.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
        }
        // 根据 progress 设置背景颜色
        else if (progress < 110)
        {
            int adjusted = progress - 10;
            textView.setBackgroundColor(Color.argb(alpha, adjusted * 255 / 100, 0, 0)); // 红色渐变
        } else if (progress < 210)
        {
            int adjusted = progress - 110;
            textView.setBackgroundColor(Color.argb(alpha, 255 - (adjusted * 255 / 100), adjusted * 255 / 100, 0)); // 黄色渐变
        } else if (progress < 310)
        {
            int adjusted = progress - 210;
            textView.setBackgroundColor(Color.argb(alpha, 0, 255 - (adjusted * 255 / 100), adjusted * 255 / 100)); // 绿色渐变
        } else
        {
            int adjusted = progress - 310;
            textView.setBackgroundColor(Color.argb(alpha, adjusted * 255 / 100, adjusted * 255 / 100, 255)); // 蓝色渐变
        }
    }

    public void changeTextColor(int progress)
    {
        progress = progress * 4;
        setTextColor(totalSpeed, progress);
        setTextColor(upSpeed, progress);
        setTextColor(downSpeed, progress);
        setTextColor(memoryUsage, progress);
    }

    //背景透明度
    public void changeTextAlpha(int alpha)
    {
        float opacityAlpha = (float) alpha / 100;
        totalSpeed.setAlpha(opacityAlpha);
        upSpeed.setAlpha(opacityAlpha);
        downSpeed.setAlpha(opacityAlpha);
        memoryUsage.setAlpha(opacityAlpha);
    }

    //父视图容量
    public void changeParentColor(int progress)
    {
        progress = (int) (progress * 4.1);
        setBgColor(totalSpeed, progress);
        setBgColor(upSpeed, progress);
        setBgColor(downSpeed, progress);
        setBgColor(memoryUsage, progress);
    }

    //获取布局参数
    private static WindowManager.LayoutParams getLayoutParams()
    {
        Log.d("获取", "布局参数");
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else
        {
            // 对于低于 API 26 的版本，使用替代方案
            layoutType = WindowManager.LayoutParams.TYPE_PHONE; // 或者其他合适的类型
        }

        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d("关闭", "FloatingWindowService");
        if (floatingView != null)
        {
            isFloatingViewVisible = false;
            windowManager.removeView(floatingView);

        }

        if (handler != null)
        {
            handler.removeCallbacks(updateRunnable); // 停止更新
        }
    }

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder
    {
        FloatingWindowService getService()
        {
            return FloatingWindowService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }
}