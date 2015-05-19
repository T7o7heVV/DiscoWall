package de.uni_kl.informatik.disco.discowall.utils.ressources;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public enum DroidWallFiles {
    DEPLOYED_BINARIES__DIR,
    NETFILTER_BRIDGE_BINARY__FILE,
    ;

    public File getFile(Context context) throws RuntimeException {
        switch(this) {
            case DEPLOYED_BINARIES__DIR: return context.getDir("bin", Context.MODE_PRIVATE);

            case NETFILTER_BRIDGE_BINARY__FILE: return new File(DEPLOYED_BINARIES__DIR.getFile(context), "netfilter_bridge");

            default: throw new RuntimeException("Method not implemented for enum value: " + this);
        }
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile("__tmp" + prefix, suffix);
        tempFile.deleteOnExit();
        return tempFile;
    }
}
