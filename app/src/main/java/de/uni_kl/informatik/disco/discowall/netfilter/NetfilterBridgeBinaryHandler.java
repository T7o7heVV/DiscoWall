package de.uni_kl.informatik.disco.discowall.netfilter;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.uni_kl.informatik.disco.discowall.AppManagement;
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

    public boolean isProcessRunning() { return bridgeBinaryExecuteResult.isRunning(); }

    public void deploy() throws NetfilterExceptions.NetfilterBridgeDeploymentException {
        Log.v(LOG_TAG, "netfilter bridge: deploying...");

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

        Log.v(LOG_TAG, "netfilter bridge: deployed and ready to use.");
    }

    public void execute(int communicationPort) throws ShellExecuteExceptions.CallException {
        // It will NOT be waited until this method returns!
        // The bridge binary runs as background process continuously.
        bridgeBinaryExecuteResult = RootShellExecute.build()
                .doNotReadResult()
                .doNotWaitForTermination()
                .appendCommand(getFile().getAbsolutePath() + " localhost " + communicationPort)
                .execute(); // non-blocking call, as ShellExecute.doWaitForTermination==false

        Thread readerThread = new Thread() {
            @Override
            public void run() {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bridgeBinaryExecuteResult.process.getInputStream()));
                Log.d(LOG_TAG, "Beginning to stream NetfilterBridge output...");

                while(bridgeBinaryExecuteResult.isRunning()) {
                    try {
                        Log.d(LOG_TAG, "NetfilterBridge:: " + bufferedReader.readLine());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        readerThread.setDaemon(true);
        readerThread.start();
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
