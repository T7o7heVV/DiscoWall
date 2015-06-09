package de.uni_kl.informatik.disco.discowall.netfilter.iptables;

import java.util.Arrays;

import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class IptablesControl {
    private static final String LOG_TAG = "IptablesControl";

    public static boolean ruleAddIfMissing(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (!IptablesControl.ruleExists(chain, rule)) {
            IptablesControl.ruleAdd(chain, rule);
            return true;
        }

        return false;
    }

    public static boolean ruleInsertIfMissing(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (!IptablesControl.ruleExists(chain, rule)) {
            IptablesControl.ruleInsert(chain, rule);
            return true;
        }

        return false;
    }

    public static boolean ruleInsertIfMissing(String chain, String rule, int index) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (!IptablesControl.ruleExists(chain, rule)) {
            IptablesControl.ruleInsert(chain, rule, index);
            return true;
        }

        return false;
    }

    public static boolean ruleDeleteIfExisting(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        if (IptablesControl.ruleExists(chain, rule)) {
            IptablesControl.ruleDelete(chain, rule);
            return true;
        }

        return false;
    }


    public static String chainAdd(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-N " + chain);
    }

    public static String chainAddIgnoreIfExisting(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return execute("-N " + chain, new int[] {0, 1});
    }

    public static String chainRemove(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-X " + chain);
    }

    public static String chainRemoveIgnoreIfMissing(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return execute("-X " + chain, new int[] {0, 1});
    }

    public static String ruleAdd(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-A " + chain + " " + rule);
    }

    public static String ruleInsert(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-I " + chain + " " + rule);
    }

    public static String ruleInsert(String chain, String rule, int index) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-I " + chain + " " + index + " " + rule);
    }

    public static String ruleDelete(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-D " + chain + " " + rule);
    }

    public static String ruleDeleteIgnoreIfMissing(String chain, String rule) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return execute("-D " + chain + " " + rule, new int[] {0, 1});
    }

    public static String rulesDeleteAll(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-F " + chain);
    }

    public static String rulesDeleteAllIgnoreIfChainMissing(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return execute("-F " + chain, new int[] {0, 1});
    }

    public static boolean chainExists(String chain) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        ShellExecute.ShellExecuteResult executeResult = executeEx("-L " + chain, new int[] {0, 1}); // if 1 is returned, the chain does not exist
        return executeResult.returnValue == 0; // else, value is 1 since all others will raise an exception
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

    public static String getRuleInfoText(boolean numericFormat, boolean verbose) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-L" + (numericFormat?" -n":"") + (verbose?" -v":""));
    }

    public static String getRuleInfoText(String chain, boolean numericFormat, boolean verbose) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        return execute("-L " + chain + (numericFormat?" -n":"") + (verbose?" -v":""));
    }

    public static String execute(String command) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        ShellExecute.ShellExecuteResult result = executeEx(command);

        if (result.returnValue != 0)
            throw new ShellExecuteExceptions.NonZeroReturnValueException(result);

        return result.processOutput;
    }

    public static String execute(String command, int[] allowedReturnValues) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        return executeEx(command, allowedReturnValues).processOutput;
    }

    private static ShellExecute.ShellExecuteResult executeEx(String command, int[] allowedReturnValues) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.ReturnValueException {
        ShellExecute.ShellExecuteResult result = executeEx(command);

        boolean returnValueAllowed = false;
        for(int allowedValue : allowedReturnValues) {
            if (allowedValue == result.returnValue) {
                returnValueAllowed = true;
                break;
            }
        }

        if (!returnValueAllowed)
            throw new ShellExecuteExceptions.ReturnValueException("Return-Value-Exception: Got return value of "+result.returnValue + " but expected return value of: " + Arrays.toString(allowedReturnValues), result, allowedReturnValues);

        return result;
    }

    private static ShellExecute.ShellExecuteResult executeEx(String command) throws ShellExecuteExceptions.CallException {
        return RootShellExecute.execute(true, true, "iptables " + command);
    }
}
