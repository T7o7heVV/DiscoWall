package de.uni_kl.informatik.disco.discowall.netfilter;

import java.io.IOException;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.utils.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.SystemUtils;

public class NetfilterUtils {
    public static boolean isIptablesModuleInstalled() throws IOException, InterruptedException, ShellExecute.NonZeroReturnValueException {
//        ShellExecute.ShellExecuteResult result = new RootShellExecute().execute("gunzip -c /proc/config.gz | grep -i netfilter config | grep -i =y");

        LinkedList<String> configContent = SystemUtils.getKernelConfig(); // will throw FileNotFoundException if /proc/config.gz is missing for this kernel

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
