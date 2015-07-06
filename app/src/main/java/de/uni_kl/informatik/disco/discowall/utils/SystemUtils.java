package de.uni_kl.informatik.disco.discowall.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions.*;

public class SystemUtils {
    private static final String LOG_TAG = SystemUtils.class.getCanonicalName();

    public static final int getUserID(Context context) {
        PackageManager packageManager = context.getPackageManager();

        try {
            Log.v(LOG_TAG, "fetching ApplicationInfo...");
            ApplicationInfo appInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Log.v(LOG_TAG, "appInfo: " + appInfo);
            Log.v(LOG_TAG, "uid: " + appInfo.uid);

            return appInfo.uid;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Unable to fetch ApplicationInfo.", e);
            return -1;
        }
    }


    public static final ApplicationInfo getApplicationInfo(Context context) {
        PackageManager packageManager = context.getPackageManager();

        try {
            return packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Unable to fetch ApplicationInfo.", e);
            return null;
        }
    }

    public static boolean isDeviceRooted() {
        try {
            ShellExecute.ShellExecuteResult executeResult = RootShellExecute.execute("id");
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

    public static boolean hasKernelConfig() {
        return new File("/proc/config.gz").exists();
    }
        /**
         * Returns the extracted string-content of the file /proc/config.gz
         * @return
         */
    public static LinkedList<String> getKernelConfig() throws ShellExecuteExceptions.NonZeroReturnValueException, IOException, CallException {
        final String configGzPath = "/proc/config.gz";
        if (!new File(configGzPath).exists())
            throw new FileNotFoundException("Config-file " + configGzPath + " does not exist.");

        File configFile = FileUtils.createTempFile("config", ".txt");

        ShellExecute.ShellExecuteResult result = new RootShellExecute().build()
                .appendCommand("gunzip -c /proc/config.gz > '" + configFile.getAbsolutePath() + "'")
                .appendCommand("echo >> '" + configFile.getAbsolutePath() + "'")
                .execute();

        if (result.returnValue != 0) {
            throw new ShellExecuteExceptions.NonZeroReturnValueException(result);
        }

        BufferedReader br = new BufferedReader(new FileReader(configFile));
        LinkedList<String> configFileContent = new LinkedList<>();

        while (br.ready())
            configFileContent.add(br.readLine());

        br.close();
        configFile.delete();

        return configFileContent;
    }

//    public static LinkedList<String> getNetworkDevices() throws IOException {
//        LinkedList<String> devices = new LinkedList<>();
//        BufferedReader reader = new BufferedReader(new FileReader(DiscoWallConstants.Files.networkDevicesListFile));
//
//        String line;
//        while((line = reader.readLine()) != null)
//            devices.add(line.trim());
//
//        return devices;
//    }

}
