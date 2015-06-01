package de.uni_kl.informatik.disco.discowall;

public final class DiscoWallConstants {
    private DiscoWallConstants() {}

    // IMPORTANT: DO NOT add language-specific strings here! I am using the res.values.strings for that.

    public static final class NotificationIDs {
        public static final int firewallService = 10;
    }

    public static final class Firewall {
        public static final int defaultPort = 1337;
        public static final int notificationID = NotificationIDs.firewallService;
    }
}
