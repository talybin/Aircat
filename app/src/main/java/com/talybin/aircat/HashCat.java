package com.talybin.aircat;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Scanner;

public class HashCat extends Thread implements Handler.Callback {

    private Handler handler = null;
    private Listener listener = null;
    private Job job = null;
    private Process process = null;

    private File hashFile = null;
    private File workingDir = null;

    // Handler messages
    private static final int MSG_ERROR = 1;
    private static final int MSG_SET_STATE = 2;
    private static final int MSG_SET_PROGRESS = 3;

    // Return working directory of hashcat
    public static String getWorkingDir(Context context) {
        return context.getFilesDir().getPath() + "/hashcat";
    }

    public interface Listener {
        void onHashCatError(HashCat instance, Exception ex);
    }

    // Default listener used when no listener given
    private static class DefaultListener implements Listener {

        private Context context;

        DefaultListener(Context context) {
            this.context = context;
        }

        @Override
        public void onHashCatError(HashCat instance, Exception ex) {
            Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Constructor
    public HashCat(Context context, Job job, Listener listener) {
        super();

        this.job = job;
        this.listener = listener;
        this.workingDir = Paths.get(getWorkingDir(context)).toFile();
        this.handler = new Handler(this);

        createHashFile();
    }

    // Constructor
    public HashCat(Context context, Job job) {
        this(context, job, new DefaultListener(context));
    }

    // Destructor
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    // Clean up
    private void close() {
        if (hashFile != null) {
            hashFile.delete();
            hashFile = null;
        }
    }

    // Return hash in hashcat format: <pmkid>*<ap mac>*<client mac>*<ssid as hex>
    private static String makeHash(Job job) {
        return String.format("%s*%s*%s*%s",
                job.getHash(),
                job.getApMac().replace(":", ""),
                job.getClientMac().replace(":", ""),
                Utils.toHexSequence(job.getSSID()));
    }

    // Create hash file containing the hash to be cracked
    private void createHashFile() {
        BufferedWriter bufferedWriter = null;
        try {
            hashFile = File.createTempFile("hashcat-", ".hash");
            // Store hash
            bufferedWriter = new BufferedWriter(new FileWriter(hashFile));
            bufferedWriter.write(makeHash(job));
        }
        catch (IOException e) {
            e.printStackTrace();
            listener.onHashCatError(this, e);
            close();
        }
        finally {
            if (bufferedWriter != null)
                try { bufferedWriter.close();  } catch (IOException ignored) {}
        }
    }

    // Post message to handler
    private void notifyHandler(int what, Object data) {
        handler.sendMessage(handler.obtainMessage(what, data));
    }

    // Message queue handler
    public boolean handleMessage(Message msg) {
        Log.d("HashCatHandler", "---> Got message: " + msg.what);
        switch (msg.what) {
            case HashCat.MSG_SET_STATE:
                job.setState((Job.State)msg.obj);
                break;

            case HashCat.MSG_SET_PROGRESS:
                job.setProgress((Job.Progress)msg.obj);
                break;

            case HashCat.MSG_ERROR:
                listener.onHashCatError(this, (Exception)msg.obj);
                break;
        }
        return true;
    }

    // Execute hashcat session
    @Override
    public void run() {
        if (hashFile == null)
            return;

        notifyHandler(MSG_SET_PROGRESS, null);
        notifyHandler(MSG_SET_STATE, Job.State.STARTING);

        InputStreamReader iss = null;
        Scanner scanner = null;

        try {
            process = Runtime.getRuntime().exec(
                    new String[] {
                            "./hashcat",
                            "-m", "16800",
                            "-a", "0",
                            "--quiet",
                            "--status", "--status-timer=3",
                            "--machine-readable",
                            hashFile.getPath(),
                            job.getWordListPath(),
                    },
                    // No environment variables
                    null,
                    // Working directory
                    workingDir
            );

            iss = new InputStreamReader(process.getInputStream());
            scanner = new Scanner(iss);

            Job.Progress progress = new Job.Progress();

            while (scanner.hasNext()) {
                //String line = scanner.nextLine();
                //Log.d(LOG_TAG, "---> line [" + line + "]");

                switch (scanner.next()) {
                    case "STATUS":
                        progress.state = scanner.nextInt();
                        break;
                    case "SPEED":
                        progress.speed = scanner.nextInt();
                        break;
                    case "PROGRESS":
                        progress.nr_complete = scanner.nextLong();
                        // Send status
                        if (progress.nr_complete == 0) {
                            progress.total = scanner.nextLong();
                            notifyHandler(MSG_SET_STATE, Job.State.RUNNING);
                        }
                        notifyHandler(MSG_SET_PROGRESS, progress);
                        break;
                }
            }

            notifyHandler(MSG_SET_STATE, Job.State.NOT_RUNNING);
        }
        catch (IOException e) {
            e.printStackTrace();
            notifyHandler(MSG_ERROR, e);
            abort();
        }
        finally {
            if (iss != null)
                try { iss.close(); } catch (IOException ignored) {}
            if (scanner != null)
                scanner.close();
        }
    }

    public void abort() {
        if (process != null) {
            if (process.isAlive()) {
                notifyHandler(MSG_SET_STATE, Job.State.STOPPING);
                // TODO send 'q' instead
                process.destroy();
                try { join(); } catch (InterruptedException ignored) {}
            }
            process = null;
            notifyHandler(MSG_SET_STATE, Job.State.NOT_RUNNING);
        }
    }
}
