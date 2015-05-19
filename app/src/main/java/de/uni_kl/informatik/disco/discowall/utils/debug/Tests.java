package de.uni_kl.informatik.disco.discowall.utils.debug;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.netfilter.NfqueueControl;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallAssets;

public class Tests {
//    public static void assetsTest(Context context) {
//        BufferedReader reader = null;
//        try {
//            reader = DroidWallAssets.DEBUG_TESTFILE.getBufferedReader(context);
//        } catch (IOException e) {
//            Log.e("tag", e.getMessage());
//        }
//
//        try {
//            Log.i("Tests", "debut testfile content: " + reader.readLine());
//            Thread.sleep(1000);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    public static void engineTest(NfqueueControl nqc) {
        Log.i("Engine Test", "Beginning nfqueue engine tests...");

        try {
            Log.i("Engine Test", "Rules enabled: " + nqc.rulesAreEnabled());
            Log.i("Engine Test", "Enable rules...");
            nqc.rulesEnableAll();
            Log.i("Engine Test", "Rules enabled: " + nqc.rulesAreEnabled());

            Log.i("Engine Test", "Disable rules...");
            nqc.rulesDisableAll();
            Log.i("Engine Test", "Rules enabled: " + nqc.rulesAreEnabled());

            Log.i("Engine Test", "Tests completed without errors.");
        } catch (Exception e) {
            Log.e("Engine Test", "Tests halted with exception: " + e.getMessage());

            e.printStackTrace();
        }
    }
}
