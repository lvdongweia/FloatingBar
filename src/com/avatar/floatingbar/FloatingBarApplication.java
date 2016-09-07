
package com.avatar.floatingbar;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

public class FloatingBarApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FloatingBarService", "FloatingBarApplication: onCreate");

        Intent intent = new Intent("com.avatar.floatingbar.START_FLOATING_BAR");
        intent.setClassName("com.avatar.floatingbar", "com.avatar.floatingbar.FloatingBarService");
        startService(intent);
    }
}
