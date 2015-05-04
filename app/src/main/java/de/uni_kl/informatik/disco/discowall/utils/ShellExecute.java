package de.uni_kl.informatik.disco.discowall.utils;

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
    }

    public static class Builder {
        public final ShellExecute shellExecute = new ShellExecute();
        public final LinkedList<String> commands = new LinkedList<>();

        public Builder waitForTermination() {
            shellExecute.waitForTermination = true;
            return this;
        }

        public Builder readResult() {
            shellExecute.readResult = true;
            return this;
        }

        public Builder appendCommand(String command) {
            commands.add(command);
            return this;
        }

        public ShellExecuteResult execute() throws IOException, InterruptedException {
            return ShellExecute.execute(shellExecute.readResult, shellExecute.waitForTermination, shellExecute.shell, this.commands.toArray(new String[this.commands.size()]));
        }
    }

    public String shell;
    public boolean readResult = false;
    public boolean waitForTermination = false;

    public static Builder build() {
        return new Builder();
    }

    public ShellExecute(String shell) {
        this.shell = shell;
    }

    public ShellExecute() {
        this.shell = "sh";
    }

    public ShellExecuteResult execute(String[] commands) throws IOException, InterruptedException {
        return execute(readResult, waitForTermination, shell, commands);
    }

    public static ShellExecuteResult execute(boolean readResult, boolean waitForTermination, String shell, String... cmds) throws IOException, InterruptedException {
        ShellExecuteResult shellExecuteResult = new ShellExecuteResult(shell, cmds);
        Log.v(LOG_TAG, "executing command [shell="+shellExecuteResult.shell+"]: " + shellExecuteResult.commandsAsString);

        try {
            Process p = Runtime.getRuntime().exec(shell);
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd+"\n");
            }

            os.writeBytes("exit\n");
            os.flush();

            // Wait until the su process terminates (after the "exit").
            // Then the entire output of the process is available.
            if (waitForTermination || readResult) {
                Log.v(LOG_TAG, "waiting for termination of command: " + shellExecuteResult.commandsAsString);
                shellExecuteResult.returnValue = p.waitFor();
                Log.v(LOG_TAG, "command '" + shellExecuteResult.commandsAsString + "' terminated with return code: " + shellExecuteResult.returnValue);
            }

            if (readResult) {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String result = "";

                Log.v(LOG_TAG, "reading string output of command: '"+shellExecuteResult.commandsAsString);
                while(inputReader.ready()) {
                    result = result + inputReader.readLine() + "\n";
                }
                Log.v(LOG_TAG, "string output of command '"+shellExecuteResult.commandsAsString+"': " + result);

                shellExecuteResult.processOutput = result;
            }

            return shellExecuteResult;
        } catch (IOException | InterruptedException e) {
            Log.e(LOG_TAG, "exception when trying to execute command: " + shellExecuteResult.commandsAsString);

            throw e;
        }
    }
}
