package de.uni_kl.informatik.disco.discowall.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import de.uni_kl.informatik.disco.discowall.utils.ressources.DiscoWallConstants;
import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class NetworkUtils {
    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    public static String[] NetworkInterfaces3g = new String[] { "rmnet", "pdp", "ppp", "uwbr", "wimax", "vsnet", "ccmni", "usb" };
    public static String[] NetworkInterfacesWifi = new String[] { "tiwlan", "wlan", "eth", "ra" };

    public static class NetworkDevice {
        public final int id;
        public final String name;

        public NetworkDevice(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String toString() {
            return name + " [id="+id+"]";
        }
    }

    public static LinkedList<NetworkDevice> getNetworkDevices() throws ShellExecuteExceptions.CallException {
        ShellExecute.ShellExecuteResult executionResult = RootShellExecute.build()
                .doReadResult()
                .doWaitForTermination()
                .appendCommand("ip link show")
                .execute();

        /* Format: <id>: <device-name>: <some-infos...>
         * Example:
         *  1: lo: <LOOPBACK,UP,LOWER_UP> mtu 16436 qdisc noqueue state UNKNOWN
                link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
            2: rmnet0: <POINTOPOINT,MULTICAST,NOARP> mtu 1500 qdisc noop state DOWN qlen 1000
                link/ppp
         *
         * IMPORTANT: The IDs are NOT line-device-counters! In some cases (like on my HTC OneX+) the deviceIDs will jump from 8 to 16.
         */

        LinkedList<NetworkDevice> devices = new LinkedList<>();
        String[] lines = executionResult.getProcessOutputInLines();

        for(int i=0; i<lines.length; i=i+2) { // after each device-line there comes one info-line which is not of interest here
            String line = lines[i].trim();

            final String formatErrorMsg = "Unexpected result format of command 'ip link show'. Expected format '<id>: <deviceName>:<rest>' but got: " + line;

            if (!line.contains(":"))
                continue;

            int colonPos = line.indexOf(":");
            int id;

            try {
                id = Integer.parseInt(line.substring(0, colonPos));
            } catch (Exception e) {
                Log.e(LOG_TAG, formatErrorMsg, e);
                continue;
            }

            String lineWithoutId = line.substring(colonPos+1);
            if (!lineWithoutId.contains(":")) {
                Log.e(LOG_TAG, formatErrorMsg);
                continue;
            }

            String device = lineWithoutId.substring(0, lineWithoutId.indexOf(":")).trim();
            devices.add(new NetworkDevice(device, id));
        }

        return devices;
    }

    public static LinkedList<String> readDnsServerConfigFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(DiscoWallConstants.Files.dnsServerConfigFile));

        /**
         * Example-Content (from Google Nexus) of /etc/resolv.conf file:
         *      > nameserver 8.8.4.4
         *      > nameserver 8.8.8.8
         *
         */

        LinkedList<String> nameserverIPs = new LinkedList<>();

        String line;
        while((line = reader.readLine()) != null) {
            line = line.trim();

            // ignore comments within file
            if (line.startsWith("#"))
                continue;

            // we search for the "nameserver" entries only
            if (!line.startsWith("nameserver"))
                continue;

            String ip = line.substring(line.indexOf("nameserver") + "nameserver".length());
            nameserverIPs.add(ip.trim());
        }

        reader.close();
        return nameserverIPs;
    }

//    public static int getProcessIdByIpAndPort(Packages.IpPortPair ipPortA, Packages.IpPortPair ipPortB) throws ShellExecuteExceptions.CallException {
//        ShellExecute.ShellExecuteResult executionResult = RootShellExecute.build()
//                .doReadResult()
//                .doWaitForTermination()
//                .appendCommand("busybox netstat -tup")
//                .execute();
//
//        for(String line : executionResult.getProcessOutputInLines()) {
//            if (!line.contains(ipPortA.getIp() + ":" + ipPortA.getPort()))
//                continue;
//            if (!line.contains(ipPortB.getIp() + ":" + ipPortB.getPort()))
//                continue;
//
//            Log.i(LOG_TAG, "PID line: " + line);
//        }
//
//        return -1;
//    }
//
//    public static Packages.IpPortPair matchConnectionsToProcesses() throws ShellExecuteExceptions.CallException {
//        // List apps with ports in LISTEN state
//        ShellExecute.ShellExecuteResult executionResult = RootShellExecute.build()
//                .doReadResult()
//                .doWaitForTermination()
//                .appendCommand("busybox netstat -lptu") // "-l" shows listen-mode only, without "-l" the rest only is shown
//                .execute();
//
//        // List apps with ports in CONNECTED/CLOSED_WAIT state
//        executionResult = RootShellExecute.build()
//                .doReadResult()
//                .doWaitForTermination()
//                .appendCommand("busybox netstat -tup")
//                .execute();
//
//        return null;
//    }
}
