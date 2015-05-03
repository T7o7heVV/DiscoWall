package de.uni_kl.informatik.disco.discowall.utils;

import android.util.Log;

import java.io.IOException;

public class RootUtils {
    private static final String LOG_TAG = RootUtils.class.getCanonicalName();

    public static ShellExecute.ShellExecuteResult shellExecuteAsRoot(String... cmds) throws IOException, InterruptedException {
        return ShellExecute.shellExecute(false, "su", cmds);
    }

    public static ShellExecute.ShellExecuteResult shellExecuteAsRoot(boolean waitForTermination, String... cmds) throws IOException, InterruptedException {
        return ShellExecute.shellExecute(waitForTermination, "su", cmds);
    }

    public static ShellExecute.ShellExecuteResult shellExecuteAsRootWithResult(String... cmds) throws IOException, InterruptedException {
        return ShellExecute.shellExecuteWithResult("su", cmds);
    }

    public static boolean isDeviceRooted() {
        try {
            ShellExecute.ShellExecuteResult executeResult = shellExecuteAsRootWithResult("id");
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
