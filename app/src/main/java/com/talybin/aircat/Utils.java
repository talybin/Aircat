package com.talybin.aircat;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Utils {

    // Cached wireless interface name
    private static String wirelessInterface = null;

    // A value holder
    static class Holder<T> {
        private T value = null;

        Holder() {}
        Holder(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }

        void set(T value) {
            this.value = value;
        }
    }

    // Extract files from zipped raw resource file to specified directory
    static boolean unpackRawZip(int resourceId, String destDir, String[] executables)
    {
        ZipEntry zipEntry;

        InputStream iss = null;
        ZipInputStream zis = null;

        try {
            iss = App.getContext().getResources().openRawResource(resourceId);
            zis = new ZipInputStream(new BufferedInputStream(iss));

            for (; (zipEntry = zis.getNextEntry()) != null; zis.closeEntry()) {
                Path filePath = Paths.get(destDir, zipEntry.getName());
                if (zipEntry.isDirectory())
                    Files.createDirectories(filePath);
                else
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Set executable permission
            for (String exe : executables)
                new File(destDir + File.separator + exe).setExecutable(true);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        finally {
            silentClose(iss, zis);
        }

        return true;
    }

    // Extract files from zipped raw resource file to files directory
    static boolean unpackRawZip(int resourceId, String[] executables) {
        return unpackRawZip(resourceId, App.getContext().getFilesDir().toString(), executables);
    }

    // Try to find wireless interface.
    // Return a string with name of wireless interface, otherwise, if
    // not found or found more than one, return "any".
    static String getWirelessInterface() {
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

    static String toString(InputStream src) throws IOException {
        InputStreamReader isr = new InputStreamReader(src);
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int length;

        while ((length = isr.read(buffer)) != -1)
            sb.append(buffer, 0, length);
        return sb.toString();
    }

    static void silentClose(Closeable... objs) {
        for (Closeable obj : objs) {
            if (obj != null)
                try { obj.close(); } catch (IOException ignored) {}
        }
    }

    // Convert string to series of ascii bytes in hex format
    static String toHexSequence(String src) {
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
