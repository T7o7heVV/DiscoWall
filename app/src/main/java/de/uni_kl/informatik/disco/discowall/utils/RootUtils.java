package de.uni_kl.informatik.disco.discowall.utils;

import android.util.Log;

import java.io.IOException;

public class RootUtils {
    private static final String LOG_TAG = RootUtils.class.getCanonicalName();

    public static boolean isDeviceRooted() {
        try {
            ShellExecute.ShellExecuteResult executeResult = RootShellExecute.build().waitForTermination().readResult().appendCommand("id").execute();
//            ShellExecute.ShellExecuteResult executeResult = shellExecuteAsRootWithResult("id");
            Log.v(LOG_TAG, "root-check shell result: " + executeResult);

            if (executeResult == null)
                return false;
            if (executeResult.processOutput == null)
                return false;

            return executeResult.processOutput.contains("uid=0(root)");
        } catch (Exception e) {
            return false;
        }
    }
}
