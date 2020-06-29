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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

class HashCat {

    private static HashCat instance = null;

    // Always same
    private static File workingDir = null;

    static HashCat getInstance() {
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

    public interface ErrorListener {
        void onError(Exception e);
    }

    private ExecutorService poolExecutor;

    // Handler for executing events on main ui thread
    private Handler uiHandler;

    // Queued jobs
    private List<Job> jobQueue;

    // Process is not null if currently running
    private Process hashCatProcess = null;

    // Used in UI thread only
    private boolean isRunning = false;

    private ErrorListener errorListener = null;

    private HashCat() {
        // Creates a thread pool that creates new threads as needed, but will
        // reuse previously constructed threads when they are available
        poolExecutor = Executors.newCachedThreadPool();

        uiHandler = new Handler();
        jobQueue = new ArrayList<>();
    }

    void setErrorListener(ErrorListener listener) {
        errorListener = listener;
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

    private void runNext() {
        if (isRunning)
            return;

        // Is there any job?
        if (!jobQueue.isEmpty()) {
            Uri uri = jobQueue.get(0).getUri();
            // Get all jobs with same uri
            List<Job> sameUriList = jobQueue.stream()
                    .filter(job -> job.getUri().equals(uri))
                    .collect(Collectors.toList());

            sameUriList.forEach(job -> job.setState(Job.State.STARTING));

            isRunning = true;
            poolExecutor.execute(() -> {
                processJobs(uri, sameUriList);

                uiHandler.post(() -> {
                    // Remove finished jobs
                    jobQueue.removeAll(sameUriList);
                    sameUriList.forEach(job -> job.setState(Job.State.NOT_RUNNING));
                    // Process next chunk
                    isRunning = false;
                    runNext();
                });
            });
        }
    }

    private void stopProcess() {
        if (hashCatProcess != null) {
            hashCatProcess.destroy();
            hashCatProcess = null;
        }
    }

    // Process a job group
    private void processJobs(Uri uri, List<Job> jobs) {
        File hashFile = null;
        try {
            hashFile = createHashFile(jobs);
            hashCatProcess = Runtime.getRuntime().exec(
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

            InputStream src = Utils.openInputStream(uri);
            OutputStream sink = hashCatProcess.getOutputStream();

            pipeStream(src, sink);
            parseOutput(hashCatProcess, uri, jobs);

            InputStream errStream = hashCatProcess.getErrorStream();
            if (errStream.available() > 0)
                throw new Exception(Utils.toString(errStream));
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
        uiHandler.post(() -> jobs.forEach(job -> job.setProgress(progress)));
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

    // Async find word list by uri. Also updates number of words
    // if not specified (needed for progress indication).
    private Future<WordList> getWordList(Uri uri) {
        return poolExecutor.submit(() -> {
            WordList wordList = WordListManager.getInstance().getOrCreate(uri);
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
                    setError(e);
                }
            }
            return wordList;
        });
    }

    // Async copy content of one stream to another.
    // Closing streams on complete.
    private void pipeStream(InputStream src, OutputStream sink) {
        poolExecutor.execute(() -> {
            // TODO uses classes instead such as ZipStreamProvider, GZip, PlainText
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
                Log.d("HashCat", token);

                String apMac = token.substring(0, pos);
                for (Job job : jobs) {
                    // Encode job to output format
                    String encJob = String.format("%s:%s:%s:",
                            job.getApMac().replace(":", ""),
                            job.getClientMac().replace(":", ""),
                            job.getSsid() != null ? job.getSsid() : "");

                    if (token.startsWith(encJob)) {
                        String password = token.substring(token.lastIndexOf(':') + 1);
                        uiHandler.post(() -> job.setPassword(password));
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
                    if (progress.nr_complete == 0)
                        setState(jobs, Job.State.RUNNING);

                    // Update total number if possible
                    if (wordListFuture.isDone()) {
                        try {
                            WordList wordList = wordListFuture.get();
                            Long cnt = wordList.getNrWords();

                            if (cnt != null) {
                                // Adjust to number of jobs
                                cnt *= jobs.size();

                                // Update progress total
                                if (progress.total == 0)
                                    progress.total = cnt;

                                // On exhausted state (gone thru word list but didn't find anything)
                                // update number of words to number completed. It will be used
                                // next time, as total number, for better precision.
                                if (progress.state == 5 && cnt != progress.nr_complete)
                                    wordList.setNrWords(progress.nr_complete / jobs.size());
                            }
                        }
                        catch (Exception e) {
                            setError(e);
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
