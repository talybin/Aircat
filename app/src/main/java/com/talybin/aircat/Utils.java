package com.talybin.aircat;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Utils {

    private static final String LOG_TAG = "Utils";

    // Cached wireless interface name
    private static String wirelessInterface = null;

    // Install binary from raw resource typically under files/ directory
    public static boolean installBinary(Context context, int sourceId, String dest) {
        FileOutputStream oss = null;
        InputStream iss = null;
        boolean ret = false;

        try {
            File out = new File(context.getFilesDir().getCanonicalPath() + File.separator + dest);
            oss = new FileOutputStream(out);
            iss = context.getResources().openRawResource(sourceId);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = iss.read(buffer)) != -1) {
                oss.write(buffer, 0, bytesRead);
            }

            ret = out.setExecutable(true);
            if (!ret)
                Log.e(LOG_TAG, "Failed to set executable");
        }
        catch (Exception ex) {
            Log.e(LOG_TAG, "Failed to install: " + ex.toString());
        }
        finally {
            if (iss != null)
                try { iss.close(); } catch (IOException ignored) {}
            if (oss != null)
                try { oss.close(); } catch (IOException ignored) {}
        }
        return ret;
    }

    // Try to find wireless interface.
    // Return a string with name of wireless interface, otherwise, if
    // not found or found more than one, return "any".
    public static String getWirelessInterface() {
        if (wirelessInterface == null) {
            String devListPath = "/sys/class/net";
            File folder = new File(devListPath);

            // Iterate over all interfaces
            for (File devFile : Objects.requireNonNull(folder.listFiles())) {
                // Check "/sys/class/net/<dev>/wireless"
                boolean isWireless = Files.exists(
                        Paths.get(devListPath, devFile.getName(), "wireless"));
                if (isWireless) {
                    if (wirelessInterface != null) {
                        // Found more than one
                        wirelessInterface = "any";
                        break;
                    }
                    // First match
                    wirelessInterface = devFile.getName();
                }
            }
        }

        return wirelessInterface;
    }
}
