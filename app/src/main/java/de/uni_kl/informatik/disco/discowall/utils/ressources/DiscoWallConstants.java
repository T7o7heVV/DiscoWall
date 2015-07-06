package de.uni_kl.informatik.disco.discowall.utils.ressources;

import java.io.File;

public final class DiscoWallConstants {
    private DiscoWallConstants() {}

    // IMPORTANT: DO NOT add language-specific strings here! I am using the res.values.strings for that.

    public static final class NotificationIDs {
        public static final int firewallService = 10;
    }

    public static final class Files {
        public static final File dnsServerConfigFile = new File("/etc/resolv.conf");
//        public static final File networkDevicesListFile = new File("/proc/net/dev");
        public static final File installedPackagesXmlFile = new File("/data/system/packages.xml");
    }

    public static final class Firewall {
        public static final int defaultPort = 1337;
        public static final int notificationID = NotificationIDs.firewallService;
    }

//    public static final class Iptables {
//        public static final int markPackageInterfaceWifi = 1000;
//        public static final int markPackageInterfaceUmts = 2000;
//    }

    public static final class DnsCache {
        public static final File dnsServerConfigFile = Files.dnsServerConfigFile;
        public static final int dnsCachePort = 5353;
    }

}
