package com.talybin.aircat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class HashCat extends Thread implements Handler.Callback {

    private Handler handler = null;
    private Listener listener = null;
    private Job job = null;
    private Process process = null;

    private File hashFile = null;
    private File workingDir = null;
    private Progress progress = new Progress();

    // Handler messages
    private static final int MSG_ERROR = 1;
    private static final int MSG_SET_STATE = 2;
    private static final int MSG_SET_PROGRESS = 3;
    private static final int MSG_SET_PASSWORD = 4;

    // Return working directory of hashcat
    public static String getWorkingDir(Context context) {
        return context.getFilesDir().getPath() + "/hashcat";
    }

    // Never null
    Progress getProgress() {
        return progress;
    }

    // HashCat progress
    public static class Progress {
        // State of the job:
        //  3 (running)
        //  5 (exhausted)
        //  6 (cracked)
        //  7 (aborted)
        //  8 (quit)
        int state = 0;

        // Speed in hashes per second
        int speed = 0;

        // Number of hashes completed so far
        long nr_complete = 0;

        // Total number of hashes
        long total = 0;

        // For debug purpose
        @NonNull
        @SuppressLint("DefaultLocale")
        public String toString() {
            return String.format("state: %d, speed: %d H/s, complete: %d/%d",
                    state, speed, nr_complete, total);
        }
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
        this.listener = listener != null ? listener : new DefaultListener(context);
        this.workingDir = Paths.get(getWorkingDir(context)).toFile();
        this.handler = new Handler(this);

        createHashFile();
    }

    // Constructor
    public HashCat(Context context, Job job) {
        this(context, job, null);
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
        Log.d("HashCat", "---> Got message: " + msg.what);
        switch (msg.what) {
            case HashCat.MSG_SET_STATE:
                job.setState((Job.State)msg.obj);
                break;

            case HashCat.MSG_SET_PROGRESS:
                job.setProgress((Progress)msg.obj);
                break;

            case HashCat.MSG_SET_PASSWORD:
                job.setPassword((String)msg.obj);
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

        InputStreamReader outStream = null;
        InputStreamReader errStream = null;
        Scanner scanner = null;

        // Password starts with AP mac address without colons
        final String apMac = job.getApMac().replace(":", "");

        try {
            process = Runtime.getRuntime().exec(
                    new String[] {
                            "./hashcat",
                            "-m", "16800",
                            "-a", "0",
                            "--quiet",
                            "--status", "--statu" +
                            "s-timer=3",
                            "--machine-readable",
                            "--logfile-disable",
                            "--potfile-disable",
                            hashFile.getPath(),
                            job.getWordListPath(),
                    },
                    // No environment variables
                    null,
                    // Working directory
                    workingDir
            );

            outStream = new InputStreamReader(process.getInputStream());
            errStream = new InputStreamReader(process.getErrorStream());
            scanner = new Scanner(outStream);

            while (scanner.hasNext()) {
                //String line = scanner.nextLine();
                //Log.d("HashCat", "---> line [" + line + "]");

                String token = scanner.next();

                if (token.startsWith(apMac)) {
                    // Password line as "<ap mac>:<client mac>:<ssid>:<password>"
                    notifyHandler(MSG_SET_PASSWORD, token.substring(token.lastIndexOf(':') + 1));
                    continue;
                }

                switch (token) {
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

            if (!process.waitFor(1000, TimeUnit.MILLISECONDS))
                throw new Exception("Timeout waiting hashcat to finish");

            if (errStream.ready()) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int length;
                while ((length = errStream.read(buffer)) != -1)
                    sb.append(buffer, 0, length);
                throw new Exception(sb.toString());
            }

            notifyHandler(MSG_SET_STATE, Job.State.NOT_RUNNING);
        }
        catch (Exception e) {
            e.printStackTrace();
            notifyHandler(MSG_ERROR, e);
            abort();
        }
        finally {
            if (outStream != null)
                try { outStream.close(); } catch (IOException ignored) {}
            if (errStream != null)
                try { errStream.close(); } catch (IOException ignored) {}
            if (scanner != null)
                scanner.close();
        }
    }

    // Call this instead of interrupt()
    public void abort() {
        if (process != null) {
            if (process.isAlive()) {
                notifyHandler(MSG_SET_STATE, Job.State.STOPPING);
                // TODO send 'q' instead, join(a couple of secs) and destroy
                process.destroy();
                try { join(); } catch (InterruptedException ignored) {}
            }
            process = null;
            notifyHandler(MSG_SET_STATE, Job.State.NOT_RUNNING);
        }
    }
}
