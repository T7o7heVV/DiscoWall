package de.uni_kl.informatik.disco.discowall.utils.shell;

public class RootShellExecute extends ShellExecute {
    public RootShellExecute() {
        super("su");
    }

    public static Builder build() {
        return ShellExecute.build().setShell("su");
    }

    public static ShellExecute.ShellExecuteResult execute(String command) throws ShellExecuteExceptions.CallException {
        return ShellExecute.execute(true, true, "su", command);
    }

    public static ShellExecute.ShellExecuteResult execute(boolean readResult, String command) throws ShellExecuteExceptions.CallException {
        return ShellExecute.execute(readResult, true, "su", command);
    }

    public static ShellExecute.ShellExecuteResult execute(boolean readResult, boolean waitForTermination, String command) throws ShellExecuteExceptions.CallException {
        return ShellExecute.execute(readResult, waitForTermination, "su", command);
    }

    public static ShellExecute.ShellExecuteResult execute(boolean readResult, boolean waitForTermination, String... cmds) throws ShellExecuteExceptions.CallException {
        return ShellExecute.execute(readResult, waitForTermination, "su", cmds);
    }
}
