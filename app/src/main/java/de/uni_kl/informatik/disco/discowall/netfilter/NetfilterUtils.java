package de.uni_kl.informatik.disco.discowall.netfilter;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import de.uni_kl.informatik.disco.discowall.utils.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.SystemInfo;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallFiles;

public class NetfilterUtils {
    public static boolean isIptablesModuleInstalled() throws IOException, InterruptedException, ShellExecute.NonZeroReturnValueException {
//        ShellExecute.ShellExecuteResult result = new RootShellExecute().execute("gunzip -c /proc/config.gz | grep -i netfilter config | grep -i =y");

        LinkedList<String> configContent = SystemInfo.getKernelConfig(); // will throw FileNotFoundException if /proc/config.gz is missing for this kernel

        boolean nfAdvanced = false;
        boolean nfNetlink = false;
        boolean nfNetlinkQueue = false;

        for(String line : configContent) {
            line = line.trim();

            // Ignoring out-commented lines
            if (line.startsWith("#"))
                continue;

            if (line.toUpperCase().contains("CONFIG_NETFILTER_ADVANCED=Y"))
                nfAdvanced = true;
            else if (line.toUpperCase().contains("CONFIG_NETFILTER_NETLINK=Y"))
                nfNetlink = true;
            else if (line.toUpperCase().contains("CONFIG_NETFILTER_NETLINK_QUEUE=Y"))
                nfNetlinkQueue = true;
        }

        return nfAdvanced && nfNetlink && nfNetlinkQueue;
    }
}
