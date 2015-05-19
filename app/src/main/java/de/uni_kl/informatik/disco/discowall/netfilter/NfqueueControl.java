package de.uni_kl.informatik.disco.discowall.netfilter;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.uni_kl.informatik.disco.discowall.AppManagement;
import de.uni_kl.informatik.disco.discowall.utils.FileUtils;
import de.uni_kl.informatik.disco.discowall.utils.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallAssets;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallFiles;

public class NfqueueControl {
    private static final String LOG_TAG = "NfqueueControl";
    private static final String IPTABLES_NFQUEUE_RULE = "-p tcp -j NFQUEUE --queue-num 0";
    private final NetfilterBridgeBinaryHandler bridgeBinaryHandler;
    private final AppManagement appManagement;

    public NfqueueControl(AppManagement appManagement) throws IOException, ShellExecute.NonZeroReturnValueException, InterruptedException {
        this.appManagement = appManagement;
        this.bridgeBinaryHandler = new NetfilterBridgeBinaryHandler();

        Log.v("NFBridge Deploy", "is deployed: " + bridgeBinaryHandler.isBinaryDeployed());

        if (!bridgeBinaryHandler.isBinaryDeployed())
            bridgeBinaryHandler.deployBinary();

        Log.v("NFBridge Deploy", "is deployed: " + bridgeBinaryHandler.isBinaryDeployed());
    }

    private void executeNetfilterBridge() {
        // TODO
    }

    private void terminateNetfilterBridge() {
        // TODO
    }

    private class NetfilterBridgeBinaryHandler {
        public boolean isBinaryDeployed() {
            return getBinaryFile().exists();
        }

        public void deployBinary() throws IOException, ShellExecute.NonZeroReturnValueException, InterruptedException {
            File netfilterBridgeBinary = DroidWallFiles.NETFILTER_BRIDGE_BINARY__FILE.getFile(appManagement.getContext());

            InputStream netfilterInputStream = DroidWallAssets.NETFILTER_BRIDGE_BINARY.getInputStream(appManagement.getContext());
            FileOutputStream netfilterOutputStream = new FileOutputStream(netfilterBridgeBinary);

            FileUtils.fileStreamCopy(netfilterInputStream, netfilterOutputStream);
            FileUtils.chmod(netfilterBridgeBinary, "777");
        }

        public File getBinaryFile() {
            return DroidWallFiles.NETFILTER_BRIDGE_BINARY__FILE.getFile(appManagement.getContext());
        }
    }

    public void rulesEnableAll() throws InterruptedException, IptablesControl.IptablesException, IOException {
        ruleAddIfMissing(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
        ruleAddIfMissing(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

    public void rulesDisableAll() throws InterruptedException, IptablesControl.IptablesException, IOException {
        ruleDeleteIfExisting(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE);
        ruleDeleteIfExisting(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

    private void ruleAddIfMissing(String chain, String rule) throws InterruptedException, IptablesControl.IptablesException, IOException {
        if (!IptablesControl.ruleExists(chain, rule))
            IptablesControl.ruleAdd(chain, rule);
    }

    private void ruleDeleteIfExisting(String chain, String rule) throws InterruptedException, IptablesControl.IptablesException, IOException {
        if (IptablesControl.ruleExists(chain, rule))
            IptablesControl.ruleDelete(chain, rule);
    }

    public boolean rulesAreEnabled() throws InterruptedException, IptablesControl.IptablesException, IOException {
        return IptablesControl.ruleExists(IptableConstants.Chains.INPUT, IPTABLES_NFQUEUE_RULE)
                || IptablesControl.ruleExists(IptableConstants.Chains.OUTPUT, IPTABLES_NFQUEUE_RULE);
    }

}
