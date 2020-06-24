package com.talybin.aircat;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.SimpleCursorTreeAdapter;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class HashCat {

    private static HashCat instance = null;

    // Always same
    private static File workingDir = null;

    static HashCat get() {
        if (instance == null) {
            synchronized (HashCat.class) {
                if (instance == null) {
                    workingDir = Paths.get(App.getContext().getFilesDir().toString(), "hashcat").toFile();
                    instance = new HashCat();
                }
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

    public interface Listener {
        void onProgress(Job job, Progress progress);
    }

    private ExecutorService hashCatExecutor;
    private ExecutorService poolExecutor;

    // Handler for executing events on main ui thread
    private Handler uiHandler;

    // Queued jobs
    private Map<Uri, List<Job>> jobQueue;

    private Listener listener = null;

    private HashCat() {
        // Creates an Executor that uses a single worker thread operating off
        // an unbounded queue
        hashCatExecutor = Executors.newSingleThreadExecutor();

        // Creates a thread pool that creates new threads as needed, but will
        // reuse previously constructed threads when they are available
        poolExecutor = Executors.newCachedThreadPool();

        uiHandler = new Handler();
        jobQueue = new HashMap<>();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    // Add jobs to the queue
    void add(Job... jobs) {
        for (Job job : jobs) {
            List<Job> list = jobQueue.get(job.getWordList());
            if (list == null)
                list = jobQueue.put(job.getWordList(), new ArrayList<>());
            list.add(job);
            // Update the job state
            job.setState(Job.State.QUEUED);
        }
    }

    // Start or queue processing with previously added or/and specified jobs
    void start(Job... jobs) {
        add(jobs);
        jobQueue.forEach((uri, jobList) -> {
            hashCatExecutor.execute(() -> processJobs(uri, jobList));
        });
        jobQueue.clear();
    }

    void stop(Job... jobs) {
        for (Job job : jobs) {
            String jobId = job.getPmkId();
            Uri key = job.getWordList();

            // First check the queue
            List<Job> queued = jobQueue.get(key);
            if (queued != null) {
                List<Job> filtered = queued.stream()
                        .filter(j -> !jobId.equals(j.getPmkId()))
                        .collect(Collectors.toList());
                jobQueue.replace(key, filtered);
            }
            // If job is running not much we can do without interrupting.
            // But if all running jobs should be stopped the hashcat should
            // be stopped as well.
            else {
                // TODO
            }
        }
    }

    // Process a job group
    private void processJobs(Uri uri, List<Job> jobs) {
        if (jobs.isEmpty())
            return;

        setState(jobs, Job.State.STARTING);

        Process process = null;
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
                            createHashFile(jobs).getPath()
                    },
                    // No environment variables
                    null,
                    // Working directory
                    workingDir
            );

            pipeStream(Utils.openInputStream(uri), process.getOutputStream());
            parseOutput(process, uri, jobs);

            InputStream errStream = process.getErrorStream();
            if (errStream.available() > 0)
                throw new Exception(Utils.toString(errStream));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (process != null)
                process.destroy();

            setState(jobs, Job.State.NOT_RUNNING);
        }
    }

    private void setState(List<Job> jobs, Job.State state) {
        uiHandler.post(() -> jobs.forEach(job -> job.setState(state)));
    }

    private void setProgress(List<Job> jobs, Progress progress) {
        if (listener != null)
            uiHandler.post(() -> jobs.forEach(job -> listener.onProgress(job, progress)));
    }

    // Generate job hashes to be read by hashcat process
    private static File createHashFile(List<Job> jobs) throws IOException {
        File hashFile = File.createTempFile("hashcat-", ".16800");
        hashFile.deleteOnExit();

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(hashFile))) {
            for (Job job : jobs) {
                bufferedWriter.write(job.getHash());
                bufferedWriter.newLine();
            }
        }
        return hashFile;
    }

    // Async find word list by uri. Also updates number of words
    // if not specified (needed for progress indication).
    private Future<WordList> getWordList(Uri uri) {
        return poolExecutor.submit(() -> {
            WordList wordList = WordLists.getOrCreate(uri);
            // Count number of words and update word list
            if (wordList.getNrWords() == null) {
                // Open and count number of words
                try (InputStream is = Utils.openInputStream(uri)) {
                    if (is != null) {
                        byte[] buffer = new byte[4096];
                        long rows = 0;

                        for (int cnt; (cnt = is.read(buffer)) > 0; ) {
                            for (int i = 0; i < cnt; ++i)
                                if (buffer[i] == '\n') ++rows;
                        }

                        // Update word list
                        wordList.setNrWords(rows);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return wordList;
        });
    }

    // Async copy content of one stream to another
    private void pipeStream(InputStream src, OutputStream sink) {
        poolExecutor.execute(() -> {
            byte[] buffer = new byte[4096];
            try {
                for (int cnt; (cnt = src.read(buffer)) > 0;)
                    sink.write(buffer, 0, cnt);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void parseOutput(Process process, Uri uri, List<Job> jobs) {

        Progress progress = new Progress();
        InputStreamReader outStream = new InputStreamReader(process.getInputStream());
        Scanner scanner = new Scanner(outStream);

        Future<WordList> wordListFuture = getWordList(uri);

        while (scanner.hasNext()) {
            //String line = scanner.nextLine();
            //Log.d("HashCat", "---> line [" + line + "]");

            String token = scanner.next();

            // Check for password line as "<ap mac>:<client mac>:<ssid>:<password>"
            // Mac addresses encoded without colons
            int pos = token.indexOf(':');
            if (pos > 0) {
                String apMac = token.substring(0, pos - 1);
                String password = token.substring(token.lastIndexOf(':') + 1);
                jobs.stream()
                        .filter(j -> j.getApMac().replace(":", "").equals(apMac))
                        .forEach(j -> uiHandler.post(() -> j.setPassword(password)));
                Log.d("HashCat", token);
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
                    if (progress.nr_complete == 0)
                        setState(jobs, Job.State.RUNNING);

                    // Update total number if possible
                    if (wordListFuture.isDone()) {
                        try {
                            WordList wordList = wordListFuture.get();
                            Long cnt = wordList.getNrWords();

                            // Update progress total
                            if (progress.total == 0 && cnt != null)
                                progress.total = cnt;

                            // On exhausted state (gone thru word list but didn't find anything)
                            // update number of words to number completed. It will be used
                            // next time, as total number, for better precision.
                            if (progress.state == 5 && (cnt == null || cnt != progress.nr_complete))
                                wordList.setNrWords(progress.nr_complete);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // This is the last interesting token in progress line,
                    // notify user
                    setProgress(jobs, progress);
                    break;
            }
        }
    }
}
