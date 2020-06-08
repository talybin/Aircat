package com.talybin.aircat;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Scanner;

public class HashCat extends Thread {

    private static final String LOG_TAG = "HashCat";
    private static String exePath = null;

    public static void setExePath(String path) {
        exePath = path;
    }

    public static String getExePath() {
        return exePath;
    }

    private Job job;
    private Handler handler;
    private Process process = null;
    private File hashFile = null;

    // Handler messages
    public static final int MSG_ERROR = 1;
    public static final int MSG_SET_STATE = 2;

    public HashCat(Job job, Handler handler) {
        this.job = job;
        this.handler = handler;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (process != null && process.isAlive())
            process.destroy();

        if (hashFile != null)
            hashFile.delete();
    }

    @Override
    public void run() {
        abort();
        process = null;

        createHashFile();
        if (hashFile == null)
            return;

        updateState(Job.State.STARTING);

        InputStreamReader iss = null;
        Scanner scanner = null;

        try {
            process = Runtime.getRuntime().exec(
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

            iss = new InputStreamReader(process.getInputStream());
            scanner = new Scanner(iss);

            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                Log.d(LOG_TAG, "---> line [" + line + "]");
            }

            updateState(Job.State.NOT_RUNNING);
        }
        catch (IOException e) {
            e.printStackTrace();
            abort();
            //listener.onError(e.getMessage());
            reportError(e);
        }
        finally {
            if (iss != null)
                try { iss.close(); } catch (IOException ignored) {}
            if (scanner != null)
                scanner.close();
        }
    }

    // Create hash file containing the hash to be cracked
    private void createHashFile() {
        // Hash in hashcat format: <pmkid>*<ap mac>*<client mac>*<ssid as hex>
        String hash = String.format("%s*%s*%s*%s",
                job.pmkId,
                job.apMac.replace(":", ""),
                job.clientMac.replace(":", ""),
                Utils.toHexSequence(job.ssid));

        // Create hash file
        BufferedWriter bufferedWriter = null;
        try {
            hashFile = File.createTempFile("hashcat-", ".hash");
            // Store hash
            bufferedWriter = new BufferedWriter(new FileWriter(hashFile));
            bufferedWriter.write(hash);
            bufferedWriter.close();
        }
        catch (IOException e) {
            e.printStackTrace();

            if (bufferedWriter != null)
                try { bufferedWriter.close();  } catch (IOException ignored) {}

            if (hashFile != null) {
                hashFile.delete();
                hashFile = null;
            }

            //listener.onError(e.getMessage());
            reportError(e);
        }
    }

    public void abort() {
        if (process != null) {
            if (process.isAlive()) {
                updateState(Job.State.STOPPING);
                process.destroy();
                try { join(); } catch (InterruptedException ignored) {}
            }
            process = null;
            updateState(Job.State.NOT_RUNNING);
        }
    }

    private void reportError(Exception ex) {
        Message msg = handler.obtainMessage(MSG_ERROR, ex);
        handler.sendMessage(msg);
    }

    // TODO jobManger.setState(job, Job.State.RUNNING)? (own handler)
    private void updateState(Job.State newState) {
        handler.sendMessage(
                handler.obtainMessage(MSG_SET_STATE, newState));
    }



    /*
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
            Job job2 = jobs[0];

            // Hash in hashcat format: <pmkid>*<ap mac>*<client mac>*<ssid as hex>
            String hash = String.format("%s*%s*%s*%s",
                    job2.pmkId,
                    job2.apMac.replace(":", ""),
                    job2.clientMac.replace(":", ""),
                    Utils.toHexSequence(job2.ssid));

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
                                job2.getWordListPath(),
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
            if (hashCatProcess != null && hashCatProcess.isAlive()) {
                hashCatProcess.destroy();
                job.setState(Job.State.STOPPING);
            }
        }
    }

    public HashCat(Job job) {
        this.job = job;
//        job.setState(Job.State.STARTING);

//        new Session(null).execute(job);
    }
     */
}
