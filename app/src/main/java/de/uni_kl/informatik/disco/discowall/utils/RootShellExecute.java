package de.uni_kl.informatik.disco.discowall.utils;

import java.io.IOException;

public class RootShellExecute extends ShellExecute {
    public RootShellExecute() {
        super("su");
    }

    public static Builder build() {
        Builder builder = ShellExecute.build();
        builder.shellExecute.shell = "su";
        return builder;
    }

    public static ShellExecute.ShellExecuteResult execute(boolean readResult, boolean waitForTermination, String command) throws IOException, InterruptedException {
        return ShellExecute.execute(readResult, waitForTermination, "su", command);
    }

    public static ShellExecute.ShellExecuteResult execute(boolean readResult, boolean waitForTermination, String... cmds) throws IOException, InterruptedException {
        return ShellExecute.execute(readResult, waitForTermination, "su", cmds);
    }
}
