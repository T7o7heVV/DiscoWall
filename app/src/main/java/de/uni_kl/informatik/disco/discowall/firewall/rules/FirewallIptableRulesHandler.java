package de.uni_kl.informatik.disco.discowall.firewall.rules;

import java.net.UnknownHostException;

import de.uni_kl.informatik.disco.discowall.packages.Connections;
import de.uni_kl.informatik.disco.discowall.packages.Packages;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public interface FirewallIptableRulesHandler {
    String getFirewallRulesText() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException;

    enum PackageHandlingMode { ACCEPT_PACKAGE, REJECT_PACKAGE, INTERACTIVE }

    void setDefaultPackageHandlingMode(PackageHandlingMode mode) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;
    PackageHandlingMode getDefaultPackageHandlingMode() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException;

    boolean isMainChainJumpsEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException;
    void setMainChainJumpsEnabled(boolean enableJumpsToMainChain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;

    // -----------------------------------
    //  User / App Rules:
    // -----------------------------------

    // Enabling firewall for a certain app:
    void setUserPackagesForwardToFirewall(int uid, boolean forward) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;
    boolean isUserPackagesForwardedToFirewall(int uid) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;

    // PolicyRules - Accept/Block a user-/app-connection
    void addPolicyRule(Packages.TransportLayerProtocol protocol, int userID, Connections.IConnection connection, FirewallRules.RulePolicy policy, FirewallRules.DeviceFilter deviceFilter) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;
    void deletePolicyRule(Packages.TransportLayerProtocol protocol, int userID, Connections.IConnection connection, FirewallRules.RulePolicy policy, FirewallRules.DeviceFilter deviceFilter) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;

    // Redirection rules - Redirect a user-/app-connection:
    void addRedirectionRule(Packages.TransportLayerProtocol protocol, int userID, int localOutgoingPort, Packages.IpPortPair remoteHostToRedirect, Packages.IpPortPair redirectTo, FirewallRules.DeviceFilter deviceFilter) throws ShellExecuteExceptions.ShellExecuteException, UnknownHostException;
    void deleteRedirectionRule(Packages.TransportLayerProtocol protocol, int userID, int localOutgoingPort, Packages.IpPortPair remoteHostToRedirect, Packages.IpPortPair redirectTo, FirewallRules.DeviceFilter deviceFilter) throws ShellExecuteExceptions.ShellExecuteException, UnknownHostException;
    void enableIptablesRedirection() throws ShellExecuteExceptions.ShellExecuteException;
}
