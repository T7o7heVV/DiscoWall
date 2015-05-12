package de.uni_kl.informatik.disco.discowall.netfilter;

import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.utils.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.ShellExecute;

public class IptablesControl {
    public static class IptablesException extends ShellExecute.ShellExecuteException {
        public IptablesException(ShellExecute.ShellExecuteResult shellExecuteResult) {
            this("Execution of iptables-command returned a non-zero value.", shellExecuteResult);
        }

        public IptablesException(String message, ShellExecute.ShellExecuteResult shellExecuteResult) {
            super(message
                        + "\n" + "> Iptables return value: " + shellExecuteResult.returnValue
                        + "\n" + "> Iptables command: " + shellExecuteResult.commandsAsString
                        + "\n" + "> Iptables output: " + shellExecuteResult.processOutput,
                    shellExecuteResult
            );
        }

    }

    private static final String LOG_TAG = "IptablesControl";

    public static String ruleAdd(String chain, String rule) throws IOException, InterruptedException, IptablesException {
        return execute("-A " + chain + " " + rule);
    }

    public static String ruleInsert(String chain, String rule) throws IOException, InterruptedException, IptablesException {
        return execute("-I " + chain + " " + rule);
    }

    public static String ruleInsert(String chain, String rule, int index) throws IOException, InterruptedException, IptablesException {
        return execute("-I " + index + " " + chain + " " + rule);
    }

    public static String ruleDelete(String chain, String rule) throws IOException, InterruptedException, IptablesException {
        return execute("-D " + chain + " " + rule);
    }

    /**
     * Removes the rule specified by the @ruleNumber within the given @chain.
     * @param chain rule-chain. Typical chains are INPUT, OUTPUT, FORWARD.
     * @param ruleNumber 1-based index of rule to delete.
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws IptablesException thrown if the iptables return-value is non-zero.
     */
    public static String ruleDelete(String chain, int ruleNumber) throws IOException, InterruptedException, IptablesException {
        return execute("-D " + chain + " " + ruleNumber);
    }

    public static boolean ruleExists(String chain, String rule) throws IOException, InterruptedException, IptablesException {
        ShellExecute.ShellExecuteResult result = executeEx("-C " + chain + " " + rule);

        if ((result.returnValue != 0) && (result.returnValue != 1))
            throw new IptablesException("Iptables return value of 0 or 1 expected, but got " + result.returnValue, result);
        else
            return (result.returnValue == 0); // chain exists, if the return-value is 0.
    }

    public static String rulesListAll() throws IOException, InterruptedException, IptablesException {
        return execute("-L -n -v");
    }

    public static String execute(String command) throws IOException, InterruptedException, IptablesException {
        ShellExecute.ShellExecuteResult result = executeEx(command);

        if (result.returnValue != 0)
            throw new IptablesException(result);

        return result.processOutput;
    }

    private static ShellExecute.ShellExecuteResult executeEx(String command) throws IOException, InterruptedException {
        Log.i(LOG_TAG, "executing iptables command: " + "iptables " + command);
        ShellExecute.ShellExecuteResult result = RootShellExecute.execute(true, true, "iptables " + command);
        Log.i(LOG_TAG, "return/error code: " + result.returnValue);

        return result;
    }
}
