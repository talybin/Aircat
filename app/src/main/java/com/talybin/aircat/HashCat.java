package com.talybin.aircat;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class HashCat extends Thread implements Handler.Callback {

    public static String getExePath() {
        return App.getContext().getFilesDir() + "/hashcat/hashcat";
    }

    private Handler handler = null;
    private Listener listener = null;
    private Job job = null;
    private Process process = null;
    private ContentResolver contentResolver = null;

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

        // Number bytes processed so far
        long nr_complete = 0;

        // Total number of bytes in initial input stream
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

        this.contentResolver = context.getContentResolver();

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
    public static String makeHash(Job job) {
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

    private Thread pipeStream(InputStream is, OutputStream os) {
        return new Thread() {
            public void run() {
                byte[] buffer = new byte[4096];
                int cnt;
                try {
                    while ((cnt = is.read(buffer)) > 0) {
                        os.write(buffer, 0, cnt);
                    }
                }
                catch (IOException e) {
                    Log.d("pipeStream", "exception: " + e.getMessage());
                    //notifyHandler(MSG_ERROR, e);
                }
                finally {
                    try { is.close(); } catch (IOException ignored) {}
                    try { os.close(); } catch (IOException ignored) {}
                }
            }
        };
    }

    private Callable<Long> countRows(InputStream is) {
        return () -> {
            byte[] buffer = new byte[4096];
            long rows = 0;

            for (int cnt; (cnt = is.read(buffer)) > 0; ) {
                for (int i = 0; i < cnt; ++i)
                    if (buffer[i] == '\n') ++rows;
            }
            return rows;
        };
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

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            process = Runtime.getRuntime().exec(
                    new String[] {
                            "./hashcat",
                            "-m", "16800",
                            "-a", "0",
                            "--quiet",
                            "--status", "--status-timer=3",
                            "--machine-readable",
                            "--logfile-disable",
                            "--potfile-disable",
                            hashFile.getPath()
                    },
                    // No environment variables
                    null,
                    // Working directory
                    workingDir
            );

            outStream = new InputStreamReader(process.getInputStream());
            errStream = new InputStreamReader(process.getErrorStream());
            scanner = new Scanner(outStream);

            pipeStream(contentResolver.openInputStream(
                    job.getWordList().getUri()), process.getOutputStream()).start();

            // TODO this should be done once (1 & 2). Utils method? running as soon as added if not exist in file
            // 1. Read number of rows from file
            // 2. Calculate if not found
            // 3. Update file here if number of runned rows greater than from file (no executor needed)
            Future<Long> nrRows = executor.submit(
                    countRows(contentResolver.openInputStream(job.getWordList().getUri())));

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
                            //progress.total = scanner.nextLong();
                            notifyHandler(MSG_SET_STATE, Job.State.RUNNING);
                        }
                        if (progress.total == 0 && nrRows.isDone()) {
                            progress.total = nrRows.get();
                            Log.d("hashcat", "---> nr rows: " + progress.total);
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

            executor.shutdown();
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
