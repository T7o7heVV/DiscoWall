package de.uni_kl.informatik.disco.discowall.utils.ressources;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by tw on 12.05.15.
 */
public enum DroidWallAssets {
    DIR_BINARIES,
    BINARY_NETFILTER_BRIDGE,
    DEBUG_TESTFILE
    ;

    public static class AssetFunctionalityNotImplementedException extends RuntimeException {
        private final DroidWallAssets asset;

        public AssetFunctionalityNotImplementedException(DroidWallAssets asset) {
            super("DroidWall asset " + asset + " does not provide the requested functionality.");
            this.asset = asset;
        }

        public DroidWallAssets getAsset() {
            return asset;
        }
    }

    public InputStream getInputStream(Context context) throws IOException {
        AssetManager assetManager = context.getAssets();

        switch (this) {
            case BINARY_NETFILTER_BRIDGE: return assetManager.open(BINARY_NETFILTER_BRIDGE.getRelativePath());
            case DEBUG_TESTFILE: return assetManager.open(DEBUG_TESTFILE.getRelativePath());

            default: throw new AssetFunctionalityNotImplementedException(this);
        }
    }

    public BufferedReader getBufferedReader(Context context) throws IOException {
        switch (this) {
            case BINARY_NETFILTER_BRIDGE: return new BufferedReader(new InputStreamReader(BINARY_NETFILTER_BRIDGE.getInputStream(context)));
            case DEBUG_TESTFILE: return new BufferedReader(new InputStreamReader(DEBUG_TESTFILE.getInputStream(context)));

            default: throw new AssetFunctionalityNotImplementedException(this);
        }
    }

    public String getRelativePath() {
        switch(this) {
            case DIR_BINARIES: return "bin/";

            case BINARY_NETFILTER_BRIDGE: return DIR_BINARIES + "netfilter_bridge";
            case DEBUG_TESTFILE: return "test.txt";

            default: throw new AssetFunctionalityNotImplementedException(this);
        }
    }
}
