package de.uni_kl.informatik.disco.discowall.firewallService.rules;

import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public interface FirewallIptableRulesHandler {
    enum PackageHandlingMode { ACCEPT_PACKAGE, REJECT_PACKAGE, INTERACTIVE }

    void setDefaultPackageHandlingMode(PackageHandlingMode mode) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;
    PackageHandlingMode getDefaultPackageHandlingMode() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException;

    void setUserPackagesForwardToFirewall(int uid, boolean forward) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;
    boolean isUserPackagesForwardedToFirewall(int uid) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;

    boolean isMainChainJumpsEnabled() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException;
    void setMainChainJumpsEnabled(boolean enableJumpsToMainChain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException;
}
