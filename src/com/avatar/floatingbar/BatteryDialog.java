
package com.avatar.floatingbar;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class BatteryDialog extends Dialog {
    private final int mBatteryDialogLongTimeout;
    private final int mBatteryDialogShortTimeout;

    private TextView mBatteryStatus;

    private BatteryReceiver mReceiver;

    private Handler mHandler = new Handler();

    private final Runnable mDismissDialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (BatteryDialog.this.isShowing()) {
                BatteryDialog.this.dismiss();
            }
        }
    };

    public BatteryDialog(Context context) {
        super(context);
        Resources r = context.getResources();
        mBatteryDialogLongTimeout = r.getInteger(R.integer.battery_dialog_long_timeout);
        mBatteryDialogShortTimeout = r.getInteger(R.integer.battery_dialog_short_timeout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY);
        window.getAttributes().privateFlags |=
                WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.requestFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.battery_dialog);
        setCanceledOnTouchOutside(true);

        mBatteryStatus = (TextView) findViewById(R.id.batteryDialogText);

        mReceiver = new BatteryReceiver();
        mReceiver.registerReceiver(getContext());
    }

    @Override
    protected void onStart() {
        super.onStart();
        dismissBatteryDialog(mBatteryDialogLongTimeout);
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeBatteryDialogCallbacks();
        mReceiver.unregisterReceiver(getContext());
        mReceiver = null;
    }

    private void removeBatteryDialogCallbacks() {
        mHandler.removeCallbacks(mDismissDialogRunnable);
    }

    private void dismissBatteryDialog(int timeout) {
        removeBatteryDialogCallbacks();
        mHandler.postDelayed(mDismissDialogRunnable, timeout);
    }

    private class BatteryReceiver extends BroadcastReceiver {
        public void registerReceiver(Context context) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            context.registerReceiver(this, filter);
        }

        public void unregisterReceiver(Context context) {
            context.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1);

            String text;
            if (level == 100) {
                text = context.getString(R.string.battery_charged_label);
            } else {
                text = (plugType == BatteryManager.BATTERY_PLUGGED_AC) ? context.getString(
                        R.string.battery_charging_label, level) : context.getString(
                        R.string.battery_meter_format, level);
            }
            Log.d("FloatingBarService", "level = " + level + " ; scale = " + scale
                    + " ; plugType = " + plugType + " ; text = " + text);
            mBatteryStatus.setText(text);
        }
    }
}
