package com.talybin.aircat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class HashCatService extends Service {

    class LocalBinder extends Binder {
        HashCatService getService() {
            return HashCatService.this;
        }
    }

    static class Progress {
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

    public interface ErrorListener {
        void onError(Exception e);
    }

    // Service related
    static final String ACTION_CANCEL = "cancel";

    private LocalBinder localBinder = new LocalBinder();

    // Notification related
    private static final int PROGRESS_NOTIFICATION_ID = 1;
    private static int dynamicId = PROGRESS_NOTIFICATION_ID + 1;
    private NotificationCompat.Builder progressBuilder = null;

    private ExecutorService poolExecutor = null;

    // Hashcat working directory
    private File workingDir = null;

    // Handler for executing events on main ui thread
    private Handler uiHandler =  new Handler();

    // Queued jobs
    private List<Job> jobQueue = new ArrayList<>();

    // Process is not null if currently running
    private Process hashCatProcess = null;

    // Used in UI thread only
    private boolean isRunning = false;
    private boolean isCancelling = false;

    private ErrorListener errorListener = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        setErrorListener(null);
        jobQueue.forEach(job -> {
            job.setProgressListener(null);
            job.setStateListener(null);
        });
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        poolExecutor = App.getThreadPool();
        workingDir = Paths.get(getFilesDir().toString(), "hashcat").toFile();
        Log.d("HashCatService", "---> onCreate: " + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d("HashCatService", "---> onStartCommand: " + action + ", this: " + this);
        if (action != null) {
            if (action.equals(ACTION_CANCEL)) {
                //stopForeground(false);
                //isCancelling = true;
                // It may take time to shutdown so hide the notification now
                //NotificationManagerCompat.from(this).cancel(PROGRESS_NOTIFICATION_ID);
                jobQueue.clear();
                stopProcess();
            }
        }
        // Do not restart service if it gets killed
        return Service.START_NOT_STICKY;
    }

    List<Job> getJobQueue() {
        return jobQueue;
    }

    void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    // Start or queue processing with specified jobs
    void start(List<Job> jobs) {
        for (Job job : jobs) {
            jobQueue.add(job);
            // Update the job state
            job.setState(Job.State.QUEUED);
        }
        runNext();
    }

    void stop(List<Job> jobs) {
        for (Job job : jobs) {
            job.setState(
                    job.isRunning() ? Job.State.STOPPING : Job.State.NOT_RUNNING);
            // Remove from the queue
            jobQueue.remove(job);
        }

        // If no running jobs left, stop hashcat too
        if (jobQueue.stream().noneMatch(Job::isRunning))
            stopProcess();
    }

    private void runNext() {
        if (isRunning)
            return;

        // Is there any job?
        if (jobQueue.isEmpty()) {
            finalReport();
            // Stop service
            stopSelf();
        }
        else {
            createProgressBuilder();

            Uri uri = jobQueue.get(0).getUri();
            // Getting word list should be on UI thread
            WordList wordList = getWordList(uri);

            // Get all jobs with same uri
            List<Job> sameUriList = jobQueue.stream()
                    .filter(job -> job.getUri().equals(uri))
                    .collect(Collectors.toList());

            sameUriList.forEach(job -> job.setState(Job.State.STARTING));

            if (App.settings().getBoolean("clear_password", false))
                sameUriList.forEach(job -> job.setPassword(null));

            isRunning = true;
            poolExecutor.execute(() -> {
                processJobs(wordList, sameUriList);

                uiHandler.post(() -> {
                    // Remove finished jobs
                    jobQueue.removeAll(sameUriList);
                    sameUriList.forEach(job -> job.setState(Job.State.NOT_RUNNING));
                    // Process next chunk
                    isRunning = false;
                    stopForeground(true);
                    runNext();
                });
            });
        }
    }

    private void stopProcess() {
        synchronized (HashCatService.class) {
            if (hashCatProcess != null) {
                hashCatProcess.destroy();
                hashCatProcess = null;
            }
        }
    }

    // Process a job group
    private void processJobs(WordList wordList, List<Job> jobs) {
        File hashFile = null;
        try {
            hashFile = createHashFile(jobs);
            hashCatProcess = Runtime.getRuntime().exec(
                    new String[] {
                            "./hashcat",

                            // Hash-type: WPA-PMKID-PBKDF2
                            "-m", "16800",

                            // Attack Mode: Straight
                            "-a", "0",

                            // Suppress output
                            "--quiet",

                            // Enable a specific workload profile from settings.
                            // Default is 2 (Economic).
                            "-w", App.settings().getString("hashcat_power_usage", "2"),

                            // Enable automatic update of the status screen.
                            // Sets seconds between status screen updates to X.
                            "--status", "--status-timer=" +
                            App.settings().getString("hashcat_refresh_interval", "3"),

                            // Display the status view in a machine-readable format
                            "--machine-readable",

                            // Enable removal of hashes once they are cracked.
                            // Remained jobs will run faster.
                            "--remove",

                            // Disable the logfile. Not reading it here.
                            "--logfile-disable",

                            // Do not write potfile. Using app database instead.
                            "--potfile-disable",

                            hashFile.getPath()
                    },
                    // No environment variables
                    null,
                    // Working directory
                    workingDir
            );

            InputStream src = Streams.openInputStream(wordList.getUri());
            OutputStream sink = hashCatProcess.getOutputStream();

            pipeStream(src, sink);
            parseOutput(hashCatProcess, wordList, jobs);

            InputStream errStream = hashCatProcess.getErrorStream();
            if (errStream.available() > 0)
                throw new Exception(Utils.toString(errStream));

            // Update on success only
            wordList.setLastUsed();
        }
        catch (Exception e) {
            setError(e);
        }
        finally {
            stopProcess();
            if (hashFile != null)
                hashFile.delete();
        }
    }

    private void setState(List<Job> jobs, Job.State state) {
        uiHandler.post(() -> jobs.forEach(job -> job.setState(state)));
    }

    private void setProgress(List<Job> jobs, Progress progress) {
        uiHandler.post(() -> {
            setProgress(jobs.size(), progress.nr_complete, progress.total);
            jobs.forEach(job -> job.setProgress(progress));
        });
    }

    private void setProgress(int nr_jobs, long complete, long total) {
        float percentComplete = 0;
        if (total > 0)
            percentComplete = complete * 100.f / total;

        progressBuilder
                .setContentTitle(getString(R.string.running_jobs, nr_jobs))
                .setProgress(100, Math.round(percentComplete), false)
                .setContentText(getString(R.string.complete_percent, percentComplete));

        NotificationManagerCompat.from(this)
                .notify(PROGRESS_NOTIFICATION_ID, progressBuilder.build());
    }

    private void setError(Exception err) {
        if (errorListener != null)
            uiHandler.post(() -> errorListener.onError(err));
        else
            err.printStackTrace();
    }

    // Generate job hashes to be read by hashcat process
    private static File createHashFile(List<Job> jobs) throws IOException {
        File hashFile = File.createTempFile("hashcat-", ".16800");

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(hashFile))) {
            for (Job job : jobs) {
                bufferedWriter.write(job.getHash());
                bufferedWriter.newLine();
            }
        }
        return hashFile;
    }

    private WordList getWordList(Uri uri) {
        WordList wordList = WordListManager.getInstance().getOrCreate(uri);
        // Count words if not done already
        if (wordList.getNrWords() == null) {
            poolExecutor.execute(() -> {
                try (InputStream is = Streams.openInputStream(wordList.getUri())) {
                    byte[] buffer = new byte[4096];
                    long rows = 0;

                    for (int cnt; (cnt = is.read(buffer)) > 0; ) {
                        for (int i = 0; i < cnt; ++i)
                            if (buffer[i] == '\n') ++rows;
                    }

                    // Update word list
                    wordList.setNrWords(rows);
                }
                catch (Exception e) {
                    setError(e);
                }
            });
        }
        return wordList;
    }

    // Async copy content of one stream to another.
    // Closing streams on complete.
    private void pipeStream(InputStream src, OutputStream sink) {
        poolExecutor.execute(() -> {
            byte[] buffer = new byte[4096];
            try {
                for (int cnt; (cnt = src.read(buffer)) > 0;)
                    sink.write(buffer, 0, cnt);
            }
            catch (IOException e) {
                // Broken pipe is ok, just means that hashcat has ended (probably
                // found passwords for all jobs) and sink has closed while writing.
                // All other errors should be reported.
                String err = e.getMessage();
                if (err == null || !err.contains("EPIPE"))
                    setError(e);
            }
            finally {
                Utils.silentClose(sink, src);
            }
        });
    }

    private void parseOutput(Process process, WordList wordList, List<Job> jobs) {

        HashCatService.Progress progress = new HashCatService.Progress();
        InputStreamReader outStream = new InputStreamReader(process.getInputStream());
        Scanner scanner = new Scanner(outStream);

        // Update status on first progress
        boolean firstProgress = true;

        // Number of jobs holds synchronization to hashcat output
        int nrJobs = jobs.size();

        while (scanner.hasNext()) {
            //String line = scanner.nextLine();
            //Log.d("HashCat", "---> line [" + line + "]");

            String token = scanner.next();

            // Check for password line as "<ap mac>:<client mac>:<ssid>:<password>"
            // Mac addresses encoded without colons
            int pos = token.indexOf(':');
            if (pos > 0) {
                Log.d("HashCat", token);

                for (Job job : jobs) {
                    // Encode job to output format
                    String encJob = String.format("%s:%s:%s:",
                            job.getApMac().replace(":", ""),
                            job.getClientMac().replace(":", ""),
                            job.getSsid() != null ? job.getSsid() : "");

                    if (token.startsWith(encJob)) {
                        String password = token.substring(token.lastIndexOf(':') + 1);
                        uiHandler.post(() -> setPassword(job, password));

                        // Manually stop the job only if there other jobs pending
                        if (jobs.size() > 1) {
                            jobs.remove(job);
                            uiHandler.post(() -> {
                                // By removing from job queue this job will not be
                                // scheduled again after this processing is done.
                                jobQueue.remove(job);
                                job.setState(Job.State.NOT_RUNNING);
                            });
                        }
                        break;
                    }
                }
            }

            // Progress token
            switch (token) {
                case "STATUS":
                    progress.state = scanner.nextInt();
                    break;

                case "SPEED":
                    progress.speed = scanner.nextInt();
                    break;

                case "PROGRESS":
                    progress.nr_complete = scanner.nextLong();

                    // Update state on first progress line
                    if (firstProgress) {
                        setState(jobs, Job.State.RUNNING);
                        firstProgress = false;
                    }

                    // Update total number if possible
                    Long words = wordList.getNrWords();
                    if (words != null) {
                        // Adjust to number of jobs
                        words *= nrJobs;

                        // Update progress total
                        progress.total = words;

                        // On exhausted state (gone thru word list but didn't find anything)
                        // update number of words to number completed. It will be used
                        // next time, as total number, for better precision.
                        if (progress.state == 5 && words != progress.nr_complete)
                            wordList.setNrWords(progress.nr_complete / nrJobs);
                    }

                    //Log.d("HashCat", "---> " + progress);

                    // This is the last interesting token in progress line,
                    // notify user
                    setProgress(jobs, progress);
                    break;
            }
        }
    }

    private void createProgressBuilder() {
        Intent cancelIntent = new Intent(this, HashCatService.class);
        cancelIntent.setAction(ACTION_CANCEL);

        PendingIntent cancelPendingIntent =
                PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                //PendingIntent.getForegroundService(this, 0, cancelIntent, 0);

        progressBuilder = getNotificationBuilder(getCategory(R.string.category_progress))
                .setContentTitle(getString(R.string.starting))
                .setContentText(getString(R.string.complete_percent, 0.f))
                // Add a progress bar
                .setProgress(100, 0, true)
                // Disable notification sound
                .setNotificationSilent()
                // Add actions
                .addAction(R.drawable.ic_stop, getString(android.R.string.cancel), cancelPendingIntent)
                // Ongoing notifications do not have an 'X' close button,
                // and are not affected by the "Clear all" button
                .setOngoing(true);

        startForeground(PROGRESS_NOTIFICATION_ID, progressBuilder.build());
    }

    private void setPassword(Job job, String password) {
        job.setPassword(password);

        Notification notification = getNotificationBuilder(getCategory(R.string.category_password))
                .setContentTitle(job.getSafeSSID())
                .setContentText(password)
                .setOngoing(false)
                .build();

        NotificationManagerCompat.from(this)
                .notify(dynamicId++, notification);
    }

    private void finalReport() {
        NotificationCompat.Builder builder = getNotificationBuilder(getCategory(R.string.category_final_report))
                .setContentTitle(getString(R.string.finished_jobs))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setOngoing(false);

        NotificationManagerCompat.from(this)
                .notify(dynamicId++, builder.build());
    }

    private NotificationCompat.Builder getNotificationBuilder(String channelId) {
        NotificationCompat.Builder builder;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            builder = new NotificationCompat.Builder(this, channelId);
        else
            builder = new NotificationCompat.Builder(this);

        return builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);
    }

    private String getCategory(int resourceName) {
        return getCategory(getString(resourceName), null, NotificationManager.IMPORTANCE_DEFAULT);
    }

    private String getCategory(String name, String description, int importance) {

        final NotificationManager nm = (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
        final String channelId = HashCatService.class.getName() + "." + name;

        if (nm != null) {
            NotificationChannel channel = nm.getNotificationChannel(channelId);

            if (channel == null) {
                channel = new NotificationChannel(channelId, name, importance);
                if (description != null)
                    channel.setDescription(description);
                nm.createNotificationChannel(channel);
            }
        }

        return channelId;
    }
}
