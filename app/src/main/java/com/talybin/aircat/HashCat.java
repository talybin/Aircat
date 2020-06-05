package com.talybin.aircat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Scanner;

public class HashCat {

    private static final String LOG_TAG = "HashCat";
    private static String exePath = null;

    public static void setExePath(String path) {
        exePath = path;
    }

    public static String getExePath() {
        return exePath;
    }

    public interface Listener {
        void onError(String err);
    }

    private static class Session extends AsyncTask<Job, String, String> {

        private Listener listener;
        private Process hashCatProcess = null;

        Session(Listener listener) {
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            hashCatProcess = null;
        }

        @Override
        protected String doInBackground(Job... jobs) {
            // Support for only one job right now
            Job job = jobs[0];

            // Hash in hashcat format: <pmkid>*<ap mac>*<client mac>*<ssid as hex>
            String hash = String.format("%s*%s*%s*%s",
                    job.pmkId,
                    job.apMac.replace(":", ""),
                    job.clientMac.replace(":", ""),
                    Utils.toHexSequence(job.ssid));

            BufferedWriter bufferedWriter = null;
            InputStreamReader iss = null;
            Scanner scanner = null;

            try {
                // Create hash file
                File hashFile = File.createTempFile("hashcat-", ".hash");
                hashFile.deleteOnExit();

                // Store hash
                bufferedWriter = new BufferedWriter(new FileWriter(hashFile));
                bufferedWriter.write(hash);
                bufferedWriter.close();
                bufferedWriter = null;

                //publishProgress(hashFile.getAbsolutePath(), hashFile.getPath());

                hashCatProcess = Runtime.getRuntime().exec(
                        new String[] {
                                "./hashcat",
                                "-m", "16800",
                                "-a", "0",
                                "--status", "--status-timer=10",
                                "--machine-readable",
                                hashFile.getPath(),
                                job.getWordListPath(),
                        },
                        // No environment variables
                        null,
                        // Working directory
                        Paths.get(exePath).getParent().toFile()
                );

                iss = new InputStreamReader(hashCatProcess.getInputStream());
                scanner = new Scanner(iss);

                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    Log.d(LOG_TAG, "---> line [" + line + "]");
                }

            } catch (IOException e) {
                e.printStackTrace();

                if (bufferedWriter != null)
                    try { bufferedWriter.close();  } catch (IOException ignored) {}

                abort();
            }
            finally {
                if (iss != null)
                    try { iss.close(); } catch (IOException ignored) {}
                if (scanner != null)
                    scanner.close();
            }

            Log.d(LOG_TAG, "---> Done");
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String s : values)
                Log.d(LOG_TAG, "---> " + s);
        }

        @Override
        protected void onPostExecute(String result) {
        }

        public void abort() {
            if (hashCatProcess != null && hashCatProcess.isAlive())
                hashCatProcess.destroy();
        }
    }

    public HashCat(Job job) {
        new Session(null).execute(job);
    }
}
