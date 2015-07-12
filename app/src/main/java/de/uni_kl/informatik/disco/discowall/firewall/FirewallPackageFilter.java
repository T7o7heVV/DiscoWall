package de.uni_kl.informatik.disco.discowall.firewall;

import android.util.Log;

import de.uni_kl.informatik.disco.discowall.firewall.rules.FirewallIptableRulesHandler;
import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;

public class FirewallPackageFilter {
    private static final String LOG_TAG = FirewallPackageFilter.class.getSimpleName();

    public enum FirewallMode { FILTER_TCP, FILTER_UDP, FILTER_ALL, ALLOW_ALL, BLOCK_ALL }

    private FirewallMode firewallMode;

    public FirewallPackageFilter(FirewallIptableRulesHandler firewallIptableRulesHandler) {
        this.firewallMode = FirewallMode.FILTER_TCP;
    }

    public FirewallMode getFirewallMode() {
        return firewallMode;
    }

    public void setFirewallMode(FirewallMode firewallMode) {
        this.firewallMode = firewallMode;
    }

    public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage, Connections.Connection connection) {
        switch(firewallMode) {
            case ALLOW_ALL:     // MODE: accept all packages
                return true;

            case BLOCK_ALL:     // MODE: block all packages
                return false;

            case FILTER_TCP:    // MODE: filter tcp only, accept rest
                if (tlPackage.getProtocol() != Packages.TransportLayerProtocol.TCP)
                    return true; // package is not tcp ==> will not be filtered
                else
                    break;

            case FILTER_UDP:    // MODE: filter udp only, accept rest
                if (tlPackage.getProtocol() != Packages.TransportLayerProtocol.UDP)
                    return true;
                else
                    break;

            case FILTER_ALL:    // MODE: filter any package
                break;
        }

//        switch(tlPackage.getProtocol())
//        {
//            case TCP:
//                return isFilteredTcpPackageAccepted((Packages.TcpPackage) tlPackage, (Connections.TcpConnection) connection);
//            case UDP:
//                return isFilteredUdpPackageAccepted((Packages.UdpPackage) tlPackage, (Connections.UdpConnection) connection);
//            default:
//                Log.e(LOG_TAG, "Unsupported Protocol: " + tlPackage.getProtocol());
//                return true;
//        }

        return isFilteredPackageAccepted(tlPackage, connection);
    }

    private boolean isFilteredPackageAccepted(Packages.TransportLayerPackage tlPackage, Connections.Connection connection) {
        Log.i(LOG_TAG, "filtering package: " + tlPackage);

        return true;
    }

//    private boolean isFilteredUdpPackageAccepted(Packages.UdpPackage udpPackage, Connections.UdpConnection connection) {
//        return true;
//    }
//
//    private boolean isFilteredTcpPackageAccepted(Packages.TcpPackage tcpPackage, Connections.TcpConnection connection) {
//        Log.i(LOG_TAG, "Connection: " + connection);
//
//        return true;
//    }


}
