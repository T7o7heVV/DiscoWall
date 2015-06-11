package de.uni_kl.informatik.disco.discowall.netfilter.bridge;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.uni_kl.informatik.disco.discowall.AppManagement;
import de.uni_kl.informatik.disco.discowall.netfilter.NetfilterExceptions;
import de.uni_kl.informatik.disco.discowall.utils.FileUtils;
import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallAssets;
import de.uni_kl.informatik.disco.discowall.utils.ressources.DroidWallFiles;

class NetfilterBridgeBinaryHandler {
    private static final String LOG_TAG = "NfBinaryHandler";
    private final AppManagement appManagement;
    private ShellExecute.ShellExecuteResult bridgeBinaryExecuteResult;

    public NetfilterBridgeBinaryHandler(AppManagement appManagement) {
        this.appManagement = appManagement;
    }

    public boolean isDeployed() { return getFile().exists(); }

    public File getFile() { return DroidWallFiles.NETFILTER_BRIDGE_BINARY__FILE.getFile(appManagement.getContext()); }

    public boolean isProcessRunning() { return (bridgeBinaryExecuteResult==null) ? false : bridgeBinaryExecuteResult.isRunning(); }

    public void deploy() throws NetfilterExceptions.NetfilterBridgeDeploymentException {
        Log.d(LOG_TAG, "netfilter bridge: deploying...");

        File netfilterBridgeBinary = DroidWallFiles.NETFILTER_BRIDGE_BINARY__FILE.getFile(appManagement.getContext());

        try {
            InputStream netfilterInputStream = DroidWallAssets.NETFILTER_BRIDGE_BINARY.getInputStream(appManagement.getContext());
            FileOutputStream netfilterOutputStream = new FileOutputStream(netfilterBridgeBinary);

            FileUtils.fileStreamCopy(netfilterInputStream, netfilterOutputStream);
            Log.v(LOG_TAG, "netfilter bridge: binary file created");
        } catch (IOException e) {
            throw new NetfilterExceptions.NetfilterBridgeDeploymentException("Could not copy netfilter-bridge binary from assets to file: " + netfilterBridgeBinary.getAbsolutePath(), e);
        }

        Log.v(LOG_TAG, "netfilter bridge: changing permissions: chmod 777");
        try {
            FileUtils.chmod(netfilterBridgeBinary, "777");
        } catch (ShellExecuteExceptions.CallException | ShellExecuteExceptions.NonZeroReturnValueException e) {
            throw new NetfilterExceptions.NetfilterBridgeDeploymentException("Error changing file permissions to '777'.", e);
        }

        Log.d(LOG_TAG, "netfilter bridge: deployed and ready to use.");
    }

    /**
     * Kills all running instances (if any) and then starts a new instance.
     * @param communicationPort
     * @throws ShellExecuteExceptions.CallException
     */
    public void restart(int communicationPort) throws ShellExecuteExceptions.CallException {
        killAllInstances();
        start(communicationPort);
    }

    public void start(int communicationPort) throws ShellExecuteExceptions.CallException {
        // It will NOT be waited until this method returns!
        // The bridge binary runs as background process continuously.
        bridgeBinaryExecuteResult = RootShellExecute.build()
                .doNotReadResult()
                .doNotWaitForTermination()
                .doRedirectStderrToStdout() // so that the stdout only contains error-data
                .appendCommand(getFile().getAbsolutePath() + " localhost " + communicationPort + " > /dev/null")
//                .appendCommand("/data/data/nfqnltest/netfilter_bridge localhost 1337")
                .execute(); // non-blocking call, as ShellExecute.doWaitForTermination==false

        // note that any output is being redirected to /dev/null in order to not create buffer-problems
    }

    public void killAllInstances() throws ShellExecuteExceptions.CallException {
        Log.v(LOG_TAG, "killing all instances of running netfilter-bridge (if any)");

        ShellExecute.ShellExecuteResult pkillResult = RootShellExecute.build()
                .doNotReadResult()
                .doWaitForTermination()
                .appendCommand("pkill '" + getFile().getAbsolutePath() + "'")
                .execute();

        if (pkillResult.returnValue == 0)
            Log.v(LOG_TAG, "pkill returned with 0. All instances killed.");
        else if (pkillResult.returnValue == 1)
            Log.v(LOG_TAG, "pkill returned with 1. No instances running.");
        else
            Log.w(LOG_TAG, "pkill returned with unexpected value of " + pkillResult.returnValue + "."
                    + " Maybe a running instance has a dead-lock?"
                    + " Will continue in the hope that everything works fine."
                    + " Call was: " + pkillResult.commandsAsString);
    }
}
