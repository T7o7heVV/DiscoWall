package de.uni_kl.informatik.disco.discowall.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallFiles;

public class SystemInfo {
    private static final String LOG_TAG = SystemInfo.class.getCanonicalName();

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
         * @throws IOException
         * @throws InterruptedException
         * @throws ShellExecute.NonZeroReturnValueException
         */
    public static LinkedList<String> getKernelConfig() throws IOException, InterruptedException, ShellExecute.NonZeroReturnValueException {
        final String configGzPath = "/proc/config.gz";
        if (!new File(configGzPath).exists())
            throw new FileNotFoundException("Config-file " + configGzPath + " does not exist.");

        File configFile = DroidWallFiles.createTempFile("config", ".txt");

        ShellExecute.ShellExecuteResult result = new RootShellExecute().build()
                .appendCommand("gunzip -c /proc/config.gz > '" + configFile.getAbsolutePath() + "'")
                .appendCommand("echo >> '" + configFile.getAbsolutePath() + "'")
                .execute();

        if (result.returnValue != 0) {
            throw new ShellExecute.NonZeroReturnValueException(result);
        }

        BufferedReader br = new BufferedReader(new FileReader(configFile));
        LinkedList<String> configFileContent = new LinkedList<>();

        while (br.ready())
            configFileContent.add(br.readLine());

        br.close();
        configFile.delete();

        return configFileContent;
    }
}
