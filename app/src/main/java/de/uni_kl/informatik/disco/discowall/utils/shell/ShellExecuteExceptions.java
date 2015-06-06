package de.uni_kl.informatik.disco.discowall.utils.shell;

import java.io.IOException;

public class ShellExecuteExceptions {
    public static abstract class ShellExecuteException extends Exception {
        private final ShellExecute.ShellExecuteResult shellExecuteResult;

        public ShellExecute.ShellExecuteResult getShellExecuteResult() {
            return shellExecuteResult;
        }

        public ShellExecuteException(String message, ShellExecute.ShellExecuteResult shellExecuteResult) {
            super(message);
            this.shellExecuteResult = shellExecuteResult;
        }

        public ShellExecuteException(String message, ShellExecute.ShellExecuteResult shellExecuteResult, Exception cause) {
            super(message, cause);
            this.shellExecuteResult = shellExecuteResult;
        }
    }

    public static class CallException extends ShellExecuteException {
        public CallException(String message, ShellExecute.ShellExecuteResult shellExecuteResult, Exception cause) {
            super(message, shellExecuteResult, cause);
        }
    }

    public static class CallInterruptedException extends CallException {
        public CallInterruptedException(ShellExecute.ShellExecuteResult shellExecuteResult, InterruptedException cause) {
            super("Received interrupted exception while waiting for termination of process."
                            + "\n"  + "Command: " + shellExecuteResult.commandsAsString
                            + "\n" + "Call-Exception: " + cause.getMessage(),
                    shellExecuteResult, cause);
        }
    }

    public static class ShellExecuteCommandNotFoundException extends CallException {
        public ShellExecuteCommandNotFoundException(ShellExecute.ShellExecuteResult shellExecuteResult, IOException cause) {
            super("Could not start command because executable was not found. "
                    + "\n"  + "Command: " + shellExecuteResult.commandsAsString
                    + "\n" + "Call-Exception: " + cause.getMessage(),
                    shellExecuteResult, cause);
        }
    }

    public static class ShellExecuteProcessCommunicationException extends CallException {
        public ShellExecuteProcessCommunicationException(ShellExecute.ShellExecuteResult shellExecuteResult, IOException cause) {
            super("Error while communicating with nested process. "
                            + "\n"  + "Command: " + shellExecuteResult.commandsAsString
                            + "\n" + "Call-Exception: " + cause.getMessage(),
                    shellExecuteResult, cause);
        }
    }

    public static class ReturnValueException extends ShellExecuteException {
        private final int[] allowedReturnValues;

        public int[] getAllowedReturnValues() {
            int[] copy = new int[allowedReturnValues.length];
            System.arraycopy(allowedReturnValues, 0, copy, 0, allowedReturnValues.length);
            return copy;
        }

        public ReturnValueException(String message, ShellExecute.ShellExecuteResult shellExecuteResult) {
            this(message, shellExecuteResult, null);
        }

        public ReturnValueException(String message, ShellExecute.ShellExecuteResult shellExecuteResult, int... allowedReturnValues) {
            super(message, shellExecuteResult);

            this.allowedReturnValues = allowedReturnValues;
        }
    }

    public static class NonZeroReturnValueException extends ReturnValueException {
        public NonZeroReturnValueException(ShellExecute.ShellExecuteResult shellExecuteResult) {
            this("Expected return value of 0, but got "+shellExecuteResult.returnValue+"."
                    + "\n" + "ShellExecuteResult was:" + shellExecuteResult.toString()
                    , shellExecuteResult);
        }

        public NonZeroReturnValueException(String message, ShellExecute.ShellExecuteResult shellExecuteResult) {
            super(message, shellExecuteResult, 0);
        }

        public static void assertZero(ShellExecute.ShellExecuteResult result) throws NonZeroReturnValueException {
            if (result.returnValue != 0)
                throw new NonZeroReturnValueException(result);
        }
    }
}
