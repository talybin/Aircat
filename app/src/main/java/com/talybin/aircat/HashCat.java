package com.talybin.aircat;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class HashCat {

    private static HashCat instance = null;

    // This is a singleton
    static HashCat getInstance() {
        if (instance == null) {
            synchronized (HashCat.class) {
                if (instance == null)
                    instance = new HashCat();
            }
        }
        return instance;
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

    private ExecutorService poolExecutor;

    // Hashcat working directory
    private File workingDir;

    // Handler for executing events on main ui thread
    private Handler handler =  new Handler();

    // Queued jobs
    private List<Job> jobQueue = new ArrayList<>();

    // Process is not null if currently running
    private Process hashCatProcess = null;

    // Used in UI thread only
    private boolean isRunning = false;

    // Listeners
    private ErrorListener errorListener = null;

    private HashCat() {
        poolExecutor = App.getThreadPool();
        workingDir = Paths.get(App.getContext().getFilesDir().toString(), "hashcat").toFile();
    }

    void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    // Run on hashcat handler to avoid concurrent modification.
    // Calls from UI do not have to use this since handler created
    // from App context.
    void post(Runnable r) {
        handler.post(r);
    }

    void start(Job... jobs) {
        start(Arrays.asList(jobs));
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

    void stop(Job... jobs) {
        stop(Arrays.asList(jobs));
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

    void stopAll() {
        stop(new ArrayList<>(jobQueue));
    }

    private void runNext() {
        if (isRunning)
            return;

        // Is there any job?
        if (!jobQueue.isEmpty()) {
            HashCatService.start();

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

                post(() -> {
                    // Remove finished jobs
                    jobQueue.removeAll(sameUriList);
                    sameUriList.forEach(job -> job.setState(Job.State.NOT_RUNNING));

                    HashCatService.stop();

                    // Process next chunk
                    isRunning = false;
                    runNext();
                });
            });
        }
    }

    private void stopProcess() {
        synchronized (HashCat.class) {
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
        post(() -> jobs.forEach(job -> job.setState(state)));
    }

    private void setProgress(List<Job> jobs, Progress progress) {
        post(() -> {
            jobs.forEach(job -> job.setProgress(progress));
            HashCatService.showProgress(jobs, progress);
        });
    }

    private void setPassword(Job job, String password) {
        post(() -> {
            job.setPassword(password);
            HashCatService.showPassword(job, password);
        });
    }

    private void setError(Exception err) {
        if (errorListener != null)
            post(() -> errorListener.onError(err));
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

        Progress progress = new Progress();
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
                        post(() -> setPassword(job, password));

                        // Manually stop the job only if there other jobs pending
                        if (jobs.size() > 1) {
                            jobs.remove(job);
                            post(() -> {
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
}
