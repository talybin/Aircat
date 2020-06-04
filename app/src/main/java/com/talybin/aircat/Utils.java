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

    // Pathes to raw files
    public static String tcpDumpPath = null;
    public static String hashCatPath = null;
    public static String builtinPasswordsPath = null;

    // Cached wireless interface name
    private static String wirelessInterface = null;

    // Install file from raw resource typically under files/ directory.
    // Return path to destination file or null on error.
    public static String installRaw(Context context, int sourceId, String dest, boolean executable) {
        FileOutputStream oss = null;
        InputStream iss = null;
        String destPath = null;

        try {
            destPath = context.getFilesDir().getCanonicalPath() + File.separator + dest;
            File out = new File(destPath);

            // Check if already installed
            if (!out.exists()) {
                // Make sure dirs exists
                new File(Objects.requireNonNull(out.getParent())).mkdirs();
                // Do install
                oss = new FileOutputStream(out);
                iss = context.getResources().openRawResource(sourceId);

                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = iss.read(buffer)) != -1) {
                    oss.write(buffer, 0, bytesRead);
                }
            }

            // Set executable if needed
            if (executable && !out.canExecute()) {
                if (!out.setExecutable(executable)) {
                    Log.e(LOG_TAG, "Failed to set executable");
                    destPath = null;
                }
            }
        }
        catch (Exception ex) {
            Log.e(LOG_TAG, "Failed to install: " + ex.toString());
            destPath = null;
        }
        finally {
            if (iss != null)
                try { iss.close(); } catch (IOException ignored) {}
            if (oss != null)
                try { oss.close(); } catch (IOException ignored) {}
        }
        return destPath;
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

    // Convert string to series of ascii bytes in hex format
    public static String toHexSequence(String src) {
        StringBuilder buffer = new StringBuilder();
        char[] digits = new char[2];

        for (byte value : src.getBytes()) {
            digits[0] = Character.forDigit((value >> 4) & 0xf, 16);
            digits[1] = Character.forDigit(value & 0xf, 16);
            buffer.append(digits);
        }
        return buffer.toString();
    }
}
