package de.uni_kl.informatik.disco.discowall.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GuiUtils {
    /**
     * refreshing the Activity be closing and reopening the activity
     * @param activity
     */
    public static void restartActivity(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(0, 0); // disabling slide-animation on Activity-finish

        activity.startActivity(activity.getIntent());
    }

    public static void expandStatusbar(Context context) throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        Object sbservice = context.getSystemService("statusbar");
        Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
        Method showsb;
        if (Build.VERSION.SDK_INT >= 17) {
            showsb = statusbarManager.getMethod("expandNotificationsPanel");
        } else {
            showsb = statusbarManager.getMethod("expand");
        }

        showsb.invoke(sbservice);
    }
}
