package de.uni_kl.informatik.disco.discowall.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

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

    public String shell = "sh";
    public boolean readResult = false;
    public boolean waitForTermination = false;

    public ShellExecute() {
    }

    public ShellExecuteResult execute(String... cmds) throws IOException, InterruptedException {
        return ShellExecute.shellExecuteEx(readResult, waitForTermination, shell, cmds);
    }



    public static ShellExecuteResult shellExecute(String... cmds) throws IOException, InterruptedException {
        return shellExecuteEx(false, false, "sh", cmds);
    }

    public static ShellExecuteResult shellExecute(String shell, String... cmds) throws IOException, InterruptedException {
        return shellExecuteEx(false, false, shell, cmds);
    }

    public static ShellExecuteResult shellExecute(boolean waitForTermination, String... cmds) throws IOException, InterruptedException {
        return shellExecuteEx(false, waitForTermination, "sh", cmds);
    }

    public static ShellExecuteResult shellExecute(boolean waitForTermination, String shell, String... cmds) throws IOException, InterruptedException {
        return shellExecuteEx(false, waitForTermination, shell, cmds);
    }

    public static ShellExecuteResult shellExecuteWithResult(String... cmds) throws IOException, InterruptedException {
        return shellExecuteEx(true, true, "sh", cmds);
    }

    public static ShellExecuteResult shellExecuteWithResult(String shell, String... cmds) throws IOException, InterruptedException {
        return shellExecuteEx(true, true, shell, cmds);
    }

    private static ShellExecuteResult shellExecuteEx(boolean readResult, boolean waitForTermination, String shell, String... cmds) throws IOException, InterruptedException {
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
