package de.uni_kl.informatik.disco.discowall.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.uni_kl.informatik.disco.discowall.utils.shell.RootShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecute;
import de.uni_kl.informatik.disco.discowall.utils.shell.ShellExecuteExceptions;

public class FileUtils {
    public static File createTempFile(String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile("__tmp" + prefix, suffix);
        tempFile.deleteOnExit();
        return tempFile;
    }

    public static void fileStreamCopy(File source, File dest) throws IOException {
        fileStreamCopy(new FileInputStream(source), new FileOutputStream(dest));
    }

    public static void fileStreamCopy(InputStream is, OutputStream os) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public static void chmod(File file, String args) throws ShellExecuteExceptions.CallException, ShellExecuteExceptions.NonZeroReturnValueException {
        ShellExecute.ShellExecuteResult result = RootShellExecute.build()
                .appendCommand("chmod " + args + " " + file.getAbsolutePath())
                .execute();

        if (result.returnValue != 0)
            throw new ShellExecuteExceptions.NonZeroReturnValueException(result);
    }


    public static File createUniqueFilename(File parent, String filenamePrefix, String filenameSuffix) {
        File unchangedFile = new File(parent, filenamePrefix + filenameSuffix);
        if (!unchangedFile.exists())
            return unchangedFile;

        // Create file with name of form: prefix_n_suffix - where "n" is the first number where this filename is unique.
        int fileNumber = 0;
        while(new File(parent, filenamePrefix +"_" + fileNumber + filenameSuffix).exists())
            fileNumber++;

        return new File(parent, filenamePrefix +"_" + fileNumber + filenameSuffix);
    }

}
