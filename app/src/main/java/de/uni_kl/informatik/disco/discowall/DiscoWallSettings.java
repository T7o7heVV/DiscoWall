package de.uni_kl.informatik.disco.discowall;

public class DiscoWallSettings {
    private DiscoWallSettings() {}
    private static DiscoWallSettings INSTANCE;

    public static DiscoWallSettings getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DiscoWallSettings();

        return INSTANCE;
    }

    public int getFirewallPort() {
        return DiscoWallConstants.Firewall.defaultPort;
    }

}
