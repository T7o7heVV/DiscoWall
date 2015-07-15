package de.uni_kl.informatik.disco.discowall.utils;

import android.app.Activity;

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
}
