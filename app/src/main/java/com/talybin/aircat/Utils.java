package com.talybin.aircat;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

    private static final String LOG_TAG = "Utils";

    // Cached wireless interface name
    private static String wirelessInterface = null;

    // Extract files from zipped raw resource file to specified directory
    public static boolean unpackRawZip(Context context, int resourceId, String destDir) {
        ZipEntry zipEntry;
        byte[] buffer = new byte[8 * 1024];

        InputStream iss = null;
        ZipInputStream zis = null;

        try {
            iss = context.getResources().openRawResource(resourceId);
            zis = new ZipInputStream(new BufferedInputStream(iss));

            while ((zipEntry = zis.getNextEntry()) != null) {
                String filename = destDir + File.separator + zipEntry.getName();

                if (zipEntry.isDirectory())
                    new File(filename).mkdirs();
                else {
                    FileOutputStream fout = new FileOutputStream(filename);
                    int bytesRead;

                    while ((bytesRead = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, bytesRead);
                    }

                    fout.close();
                    zis.closeEntry();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        finally {
            if (iss != null)
                try { iss.close(); } catch (IOException ignored) {}
            if (zis != null)
                try { zis.close(); } catch (IOException ignored) {}
        }

        return true;
    }

    // Extract files from zipped raw resource file to files directory
    public static boolean unpackRawZip(Context context, int resourceId) {
        return unpackRawZip(context, resourceId, context.getFilesDir().toString());
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
