package de.uni_kl.informatik.disco.discowall.netfilter;

import android.util.Log;

import java.io.IOException;

import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class IptablesControl {
    private static final String LOG_TAG = "IptablesControl";

    public static String ruleAdd(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-A " + chain + " " + rule);
    }

    public static String ruleInsert(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-I " + chain + " " + rule);
    }

    public static String ruleInsert(String chain, String rule, int index) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-I " + index + " " + chain + " " + rule);
    }

    public static String ruleDelete(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-D " + chain + " " + rule);
    }

    /**
     * Removes the rule specified by the @ruleNumber within the given @chain.
     * @param chain rule-chain. Typical chains are INPUT, OUTPUT, FORWARD.
     * @param ruleNumber 1-based index of rule to delete.
     * @throws ShellExecuteExceptions.NonZeroReturnValueException thrown if the iptables return-value is non-zero.
     */
    public static String ruleDelete(String chain, int ruleNumber) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-D " + chain + " " + ruleNumber);
    }

    public static boolean ruleExists(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        ShellExecute.ShellExecuteResult result = executeEx("-C " + chain + " " + rule);

        if ((result.returnValue != 0) && (result.returnValue != 1))
            throw new ShellExecuteExceptions.NonZeroReturnValueException(result);
        else
            return (result.returnValue == 0); // chain exists, if the return-value is 0.
    }

    public static String rulesListAll() throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-L -n -v");
    }

    public static String execute(String command) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        ShellExecute.ShellExecuteResult result = executeEx(command);

        if (result.returnValue != 0)
            throw new ShellExecuteExceptions.NonZeroReturnValueException(result);

        return result.processOutput;
    }

    private static ShellExecute.ShellExecuteResult executeEx(String command) throws ShellExecuteExceptions.CallException {
        Log.i(LOG_TAG, "executing iptables command: " + "iptables " + command);
        ShellExecute.ShellExecuteResult result = RootShellExecute.execute(true, true, "iptables " + command);
        Log.i(LOG_TAG, "return/error code: " + result.returnValue);

        return result;
    }
}
