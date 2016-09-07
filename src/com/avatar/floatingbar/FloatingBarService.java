
package com.avatar.floatingbar;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FloatingBarService extends Service {
    private final static String SHOWING_FLOATINGBAR = "show_floatingbar";
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private FloatingNavigationBar mBar;
    private boolean mIsShowing = false;
    private SettingsDBObserver mObserver;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (getShowingFlagFromDB()) {
                showFloatingView();
            } else {
                hideFloatingView();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FloatingBarService", "onCreate");
        initFloatingView();
        mObserver = new SettingsDBObserver(this, mHandler);
        mObserver.register();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.d("FloatingBarService", "onStartCommand");
        Message.obtain(mHandler, 0).sendToTarget();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("FloatingBarService", "onDestroy");
        hideFloatingView();
        mObserver.unregister();
        mObserver = null;
    }

    private void initFloatingView() {
        Log.d("FloatingBarService", "initFloatingView");
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.type = LayoutParams.TYPE_SYSTEM_ALERT;
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.x = 0;
        mLayoutParams.y = 0;

        mBar = new FloatingNavigationBar(getApplication());
        mBar.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        if (!getShowingFlagFromDB()) {
            return;
        }

        showFloatingView();
    }

    private boolean getShowingFlagFromDB() {
        return Settings.System.getInt(getContentResolver(), SHOWING_FLOATINGBAR, 1) == 1;
    }

    private void showFloatingView() {
        if (mIsShowing) {
            Log.d("FloatingBarService", "showFloatingView: return");
            return;
        }

        Log.d("FloatingBarService", "showFloatingView");
        if (mBar != null) {
            mWindowManager.addView(mBar, mLayoutParams);
            mIsShowing = true;
        }
    }

    private void hideFloatingView() {
        if (!mIsShowing) {
            Log.d("FloatingBarService", "hideFloatingView: return");
            return;
        }

        Log.d("FloatingBarService", "hideFloatingView");
        if (mBar != null) {
            mWindowManager.removeView(mBar);
            mIsShowing = false;
        }
    }

    private void updateFloatingBar(float x, float y) {
        mLayoutParams.x = (int) x - mBar.getMeasuredWidth() / 2;
        mLayoutParams.y = (int) y - mBar.getMeasuredHeight() / 2;

        mWindowManager.updateViewLayout(mBar, mLayoutParams);
    }

    private class FloatingNavigationBar extends LinearLayout implements OnClickListener,
            OnTouchListener {
        private LinearLayout mBarView;
        private ImageView mBtn;
        private BatteryDialog mBatteryDialog;

        public FloatingNavigationBar(Context context) {
            super(context);
            bindView(context);
        }

        private void bindView(Context context) {
            setOrientation(LinearLayout.HORIZONTAL);
            View view = View.inflate(context,
                    R.layout.floating_navigation_bar, null);
            addView(view, new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            mBtn = (ImageView) view.findViewById(R.id.floatBtn);
            mBarView = (LinearLayout) view.findViewById(R.id.floatView);
            KeyButtonView back = (KeyButtonView) view.findViewById(R.id.back);
            KeyButtonView settings = (KeyButtonView) view.findViewById(R.id.settings);
            KeyButtonView brightness = (KeyButtonView) view.findViewById(R.id.brightness);
            KeyButtonView recent = (KeyButtonView) view.findViewById(R.id.recent);
            KeyButtonView home = (KeyButtonView) view.findViewById(R.id.home);
            KeyButtonView battery = (KeyButtonView) view.findViewById(R.id.battery);

            mBtn.setOnTouchListener(this);
            mBarView.setOnTouchListener(this);

            mBtn.setOnClickListener(this);
            mBarView.setOnClickListener(this);
            back.setOnClickListener(this);
            settings.setOnClickListener(this);
            brightness.setOnClickListener(this);
            recent.setOnClickListener(this);
            home.setOnClickListener(this);
            battery.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.d("FloatingBarService", "onClick");
            switch (v.getId()) {
                case R.id.recent:
                    toggleRecents();
                    hideControlView();
                    break;

                case R.id.back:
                case R.id.home:
                    hideControlView();
                    break;

                case R.id.settings:
                    startSettingsActivity();
                    hideControlView();
                    break;

                case R.id.brightness:
                    hideControlView();
                    showBrightnessDialog();
                    break;

                case R.id.battery:
                    showBatteryDialog();
                    hideControlView();
                    break;

                case R.id.floatView:
                case R.id.floatBtn:
                    showControlView();
                    break;
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.d("FloatingBarService", "onTouch");
            float x = event.getRawX();
            float y = event.getRawY();
            Log.d("FloatingBarService", "onTouch: x = " + x + "; y = " + y);
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    Log.d("FloatingBarService", "onTouch: MOVE");
                    updateFloatingBar(x, y);
                    break;

                case MotionEvent.ACTION_UP:
                    Log.d("FloatingBarService", "onTouch: UP");
                    break;
            }
            return false;
        }

        private void toggleRecents() {
            Intent intent = new Intent("com.android.systemui.recent.action.TOGGLE_RECENTS");
            intent.setClassName("com.android.systemui",
                    "com.android.systemui.recent.RecentsActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            getContext().startActivity(intent);
        }

        private void showControlView() {
            if (mBarView.getVisibility() == View.GONE) {
                mBarView.setVisibility(View.VISIBLE);
                mBtn.setVisibility(View.INVISIBLE);
                return;
            }
        }

        private void hideControlView() {
            if (mBarView.getVisibility() == View.VISIBLE) {
                mBarView.setVisibility(View.GONE);
                mBtn.setVisibility(View.VISIBLE);
                return;
            }
        }

        private void startSettingsActivity() {
            Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getContext().startActivity(intent);
        }

        private void showBrightnessDialog() {
            Intent intent = new Intent(Intent.ACTION_SHOW_BRIGHTNESS_DIALOG);
            getContext().sendBroadcast(intent);
        }

        private void showBatteryDialog() {
            Log.d("FloatingBarService", "showBatteryDialog");
            if (mBatteryDialog == null) {
                mBatteryDialog = new BatteryDialog(getContext());
                mBatteryDialog.setOnDismissListener(new OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mBatteryDialog = null;
                    }

                });
            }

            if (!mBatteryDialog.isShowing()) {
                mBatteryDialog.show();
            }
        }
    }

    private class SettingsDBObserver extends ContentObserver {
        private Context mContext;
        private Handler mObserverHandler;

        public SettingsDBObserver(Context context, Handler handler) {
            super(handler);
            mContext = context;
            mObserverHandler = handler;
        }

        public void register() {
            Uri uri = Uri.withAppendedPath(Settings.System.CONTENT_URI, SHOWING_FLOATINGBAR);
            Log.d("FloatingBarService", "register: uri = " + uri);
            mContext.getContentResolver().registerContentObserver(uri , true, this);
        }

        public void unregister() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("FloatingBarService", "onChange");
            Message.obtain(mObserverHandler, 0).sendToTarget();
        }
    }
}
