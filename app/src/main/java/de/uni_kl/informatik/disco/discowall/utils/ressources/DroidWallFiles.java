package de.uni_kl.informatik.disco.discowall.utils.ressources;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public enum DroidWallFiles {
    DEPLOYED_BINARIES__DIR,
    FIREWALL_RULES__DIR,
    NETFILTER_BRIDGE_BINARY__FILE,
    ;

    public File getFile(Context context) throws RuntimeException {
        switch(this) {
            case DEPLOYED_BINARIES__DIR: return context.getDir("bin", Context.MODE_PRIVATE);
            case FIREWALL_RULES__DIR: return context.getDir("rules", Context.MODE_PRIVATE);

            case NETFILTER_BRIDGE_BINARY__FILE: return new File(DEPLOYED_BINARIES__DIR.getFile(context), "netfilter_bridge");

            default: throw new RuntimeException("Method not implemented for enum value: " + this);
        }
    }

}
