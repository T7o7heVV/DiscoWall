package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import android.util.Log;

import java.util.HashMap;

import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.AppUtils;
import de.uni_kl.informatik.disco.discowall.utils.NetworkUtils;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class FirewallRulesManager {
    private static final String LOG_TAG = FirewallRulesManager.class.getSimpleName();
    public enum FirewallMode { FILTER_TCP, FILTER_UDP, FILTER_ALL, ALLOW_ALL, BLOCK_ALL }

    private final HashMap<String, Connections.Connection> connectionIdToConnectionMap = new HashMap<>();
    private FirewallMode firewallMode;

    public FirewallRulesManager() {
        firewallMode = FirewallMode.FILTER_TCP;
    }

    public FirewallMode getFirewallMode() {
        return firewallMode;
    }

    public void setFirewallMode(FirewallMode firewallMode) {
        this.firewallMode = firewallMode;
    }

    //    public StaticFirewallRules.StaticTcpRule createRule_Accept(FirewallRules.DeviceFilter device, Packages.TcpPackage tcpPackage) {
//        return new StaticFirewallRules.StaticTcpRule(FirewallRules.FirewallRuleAction.ACCEPT, tcpPackage.getSource(), tcpPackage.getDestination(), device);
//    }
//
//    public StaticFirewallRules.StaticTcpRule createRule_AcceptTCP(FirewallRules.DeviceFilter device, Packages.IpPortPair source, Packages.IpPortPair destination) {
//        return new StaticFirewallRules.StaticTcpRule(FirewallRules.FirewallRuleAction.ACCEPT, source, destination, device);
//    }

    public boolean isPackageAccepted(Packages.TransportLayerPackage tlPackage, Connections.Connection connection) {
        switch(firewallMode) {
            case ALLOW_ALL:     // MODE: accept all packages
                return true;

            case BLOCK_ALL:     // MODE: block all packages
                return false;

            case FILTER_TCP:    // MODE: filter tcp only, accept rest
                if (tlPackage.getProtocol() != Packages.TransportProtocol.TCP)
                    return true; // package is not tcp ==> will not be filtered
                else
                    break;

            case FILTER_UDP:    // MODE: filter udp only, accept rest
                if (tlPackage.getProtocol() != Packages.TransportProtocol.UDP)
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
