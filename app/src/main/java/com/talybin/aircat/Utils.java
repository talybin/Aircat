package com.talybin.aircat;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
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

public class Utils {

    private static final String LOG_TAG = "Utils";

    // Cached wireless interface name
    private static String wirelessInterface = null;

    // A value holder
    public static class Holder<T> {
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
    public static boolean unpackRawZip(int resourceId, String destDir, String[] executables)
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
            if (iss != null)
                try { iss.close(); } catch (IOException ignored) {}
            if (zis != null)
                try { zis.close(); } catch (IOException ignored) {}
        }

        return true;
    }

    // Extract files from zipped raw resource file to files directory
    public static boolean unpackRawZip(int resourceId, String[] executables) {
        return unpackRawZip(resourceId, App.getContext().getFilesDir().toString(), executables);
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


    static InputStream openInputStream(Uri uri) throws FileNotFoundException {
        return App.getInstance().getContentResolver().openInputStream(uri);
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

    /*
    static void silentClose(Closeable... objs) {
        for (Closeable obj : objs) {
            if (obj != null)
                try { obj.close(); } catch (IOException ignored) {}
        }
    }*/

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

    public static void retrieveNrRows(Context context, Uri uri) {
        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... iss) {
                InputStream is = null;
                byte[] buffer = new byte[4096];
                long rows = 0;

                try {
                    is = context.getContentResolver().openInputStream(uri);
                    if (is != null) {
                        for (int cnt; (cnt = is.read(buffer)) > 0; ) {
                            for (int i = 0; i < cnt; ++i)
                                if (buffer[i] == '\n') ++rows;
                        }
                        return rows;
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    if (is != null)
                        try { is.close(); } catch (IOException ignored) {}
                }
                return null;
            }

            @Override
            protected void onPostExecute(Long nrRows) {
                // TODO save to file
                if (nrRows != null)
                    Log.d("retrieveNrRows", uri.getLastPathSegment() + ": " + nrRows);
            }
        }.execute();
/*
        new Thread() {
            @Override
            public void run() {

                InputStream is = context.getContentResolver().openInputStream(uri);
                ContentResolver contentResolver = context.getContentResolver();
                byte[] buffer = new byte[4096];
                long rows = 0;

                for (
                        int cnt; (cnt = is.read(buffer)) > 0; ) {
                    for (int i = 0; i < cnt; ++i)
                        if (buffer[i] == '\n') ++rows;
                }
            }
        }.start();
 */
    }

    /*
    public static long getUriSize(ContentResolver contentResolver, Uri uri) {
        AssetFileDescriptor afd = null;
        long size = 0;
        try {
            afd = contentResolver.openAssetFileDescriptor(uri, "r");
        }
        catch (FileNotFoundException ignored) {}
        if (afd != null) {
            size = afd.getLength();
            try { afd.close(); } catch (IOException ignored) {}
        }
        return size;
    }

    public static long getUriSize(Context context, Uri uri) {
        return getUriSize(context.getContentResolver(), uri);
    }
     */
}
