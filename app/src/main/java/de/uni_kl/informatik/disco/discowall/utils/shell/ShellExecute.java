package de.uni_kl.informatik.disco.discowall.utils.shell;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class ShellExecute {
    private static final String LOG_TAG = ShellExecute.class.getCanonicalName();

    public static class ShellExecuteResult {
        public final String shell, commandsAsString;
        public final String[] commands;
        public String processOutput;
        public int returnValue;
        public Process process;

        @Override
        public String toString() {
            return "ShellExecuteResult:"
                    + "\n * shell: " + shell
                    + "\n * command: " + commandsAsString
                    + "\n * process output: " + processOutput
                    + "\n * return value: " + returnValue;
        }

        public ShellExecuteResult(String shell, String[] commands) {
            this.commands = commands;
            this.shell = shell;

            String cmdStr = "";
            for(String str : commands) {
                if (!cmdStr.isEmpty())
                    cmdStr = cmdStr + "; ";
                cmdStr = cmdStr + str;
            }

            this.commandsAsString = cmdStr;
        }

        public boolean isRunning() {
            if (process == null)
                throw new RuntimeException("Process handle has not been attached to " + this.getClass().getSimpleName() + " on startup. Requested information not available.");

            // Workaround: If the process is still running, calling Process.exitValue() will throw an exception.
            try {
                process.exitValue();
                return true;
            } catch (IllegalThreadStateException e) {
                return false;
            }
        }
    }

    public static class Builder {
        private final ShellExecute shellExecute = new ShellExecute();
        private final LinkedList<String> commands = new LinkedList<>();

        public Builder setShell(String shell) {
            shellExecute.shell = shell;
            return this;
        }

        public Builder doNotWaitForTermination() {
            shellExecute.waitForTermination = false;
            return this;
        }

        public Builder doNotReadResult() {
            shellExecute.readResult = false;
            return this;
        }

        public Builder doWaitForTermination() {
            shellExecute.waitForTermination = true;
            return this;
        }

        public Builder doReadResult() {
            shellExecute.readResult = true;
            return this;
        }

        public Builder appendCommand(String command) {
            commands.add(command);
            return this;
        }

        public ShellExecuteResult executeAndAssertReturnValueZero() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
            ShellExecuteResult result = execute();

            ShellExecuteExceptions.NonZeroReturnValueException.assertZero(result);
            return result;
        }

        public ShellExecuteResult execute() throws ShellExecuteExceptions.CallException {
            return ShellExecute.execute(shellExecute.readResult, shellExecute.waitForTermination, shellExecute.shell, this.commands.toArray(new String[this.commands.size()]));
        }
    }

    public String shell;
    public boolean readResult = true;
    public boolean waitForTermination = true;

    public static Builder build() {
        return new Builder();
    }

    public ShellExecute(String shell) {
        this.shell = shell;
    }

    public ShellExecute() {
        this.shell = "sh";
    }

    public ShellExecuteResult execute(String... commands) throws ShellExecuteExceptions.CallException {
        return execute(readResult, waitForTermination, shell, commands);
    }

    public static ShellExecuteResult execute(boolean readResult, boolean waitForTermination, String shell, String command) throws ShellExecuteExceptions.CallException {
        return execute(readResult, waitForTermination, shell, new String[] { command });
    }

    public static ShellExecuteResult execute(boolean readResult, boolean waitForTermination, String shell, String... cmds) throws ShellExecuteExceptions.CallException {
        ShellExecuteResult shellExecuteResult = new ShellExecuteResult(shell, cmds);
        Log.v(LOG_TAG, "executing command [shell="+shellExecuteResult.shell+"]: " + shellExecuteResult.commandsAsString);

        Process p = null;
        try {
            p = Runtime.getRuntime().exec(shell);
            shellExecuteResult.process = p;
        } catch (IOException e) {
            throw new ShellExecuteExceptions.ShellExecuteCommandNotFoundException(shellExecuteResult, e);
        }

        DataOutputStream os = new DataOutputStream(p.getOutputStream());

        try {
            for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd+"\n");
            }

            os.writeBytes("exit\n");
            os.flush();

            if (readResult) {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String result = "";

                Log.v(LOG_TAG, "reading string output of command: '"+shellExecuteResult.commandsAsString);
                while(inputReader.ready()) {
                    result = result + inputReader.readLine() + "\n";
                }
                Log.v(LOG_TAG, "string output of command '" + shellExecuteResult.commandsAsString + "': " + result);

                shellExecuteResult.processOutput = result;
            }
        } catch(IOException e) {
            throw new ShellExecuteExceptions.ShellExecuteProcessCommunicationException(shellExecuteResult, e);
        }

        // Wait until the su process terminates (after the "exit").
        // Then the entire output of the process is available.
        if (waitForTermination || readResult) {
            Log.v(LOG_TAG, "waiting for termination of command: " + shellExecuteResult.commandsAsString);

            try {
                shellExecuteResult.returnValue = p.waitFor();
            } catch(InterruptedException e) {
                throw new ShellExecuteExceptions.CallInterruptedException(shellExecuteResult, e);
            }

            Log.v(LOG_TAG, "command '" + shellExecuteResult.commandsAsString + "' terminated with return code: " + shellExecuteResult.returnValue);
        }

        return shellExecuteResult;
    }
}
