package com.talybin.aircat;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HashCatServiceOld2 extends Service implements Handler.Callback {

    // Commands to the service
    static final int MSG_SET_SETTINGS = 1;
    static final int MSG_START_JOBS = 2;
    static final int MSG_STOP_JOBS = 3;

    // Arguments to intent and bundle
    static final String ARG_PATH = "path";
    static final String ARG_RECEIVER = "receiver";
    static final String ARG_JOB_LIST = "jobs";

    // Setting keys
    static final String SETTING_POWER_USAGE = "hashcat_power_usage";
    static final String SETTING_REFRESH_INTERVAL = "hashcat_refresh_interval";

    // Event codes
    static final int EVENT_CODE_ERROR = 1;

    // Event arguments
    static final String EVENT_ARG_ERROR = "error";

    // Communication channels
    private Messenger messenger = null;
    private ResultReceiver resultReceiver = null;

    // Threading
    private ExecutorService poolExecutor = null;

    // Location path of hashcat
    private String hashCatPath = null;

    // Running hashcat process
    private Process hashCatProcess = null;

    // Queued jobs
    private List<Job> jobQueue = new ArrayList<>();

    // Current settings
    Bundle settings = new Bundle();

    @Override
    public void onCreate() {
        super.onCreate();
        poolExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        poolExecutor.shutdownNow();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        resultReceiver = intent.getParcelableExtra(ARG_RECEIVER);

        messenger = new Messenger(new Handler(this));
        return messenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        resultReceiver = null;
        messenger = null;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        hashCatPath = intent.getStringExtra(ARG_PATH);
        //return Service.START_REDELIVER_INTENT;
        Log.d("HashCatService", "---> path: " + hashCatPath);
        return Service.START_STICKY;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {

            case MSG_SET_SETTINGS:
                settings.putAll(msg.getData());
                break;

            case MSG_START_JOBS:
                startJobs(msg.getData().getParcelableArrayList(ARG_JOB_LIST));
                break;

            case MSG_STOP_JOBS:
                stopJobs(msg.getData().getParcelableArrayList(ARG_JOB_LIST));
                break;

            default:
                setError(new Exception("Unknown message id: " + msg.what));
                return false;
        }
        return true;
    }

    private void setError(Exception err) {
        if (resultReceiver != null) {
            Bundle args = new Bundle();
            args.putString(EVENT_ARG_ERROR, err.getMessage());

            resultReceiver.send(EVENT_CODE_ERROR, args);
        }
        else
            err.printStackTrace();
    }

    private void startJobs(List<Job> jobs) {
        if (jobs != null && !jobs.isEmpty()) {
            jobQueue.addAll(jobs);

            jobs.forEach(job -> Log.d("startJobs", "---> " + job));
            setError(new Exception("hehe"));
        }
    }

    private void stopJobs(List<Job> jobs) {
        if (jobs != null && !jobs.isEmpty()) {

            jobs.forEach(job -> Log.d("startJobs", "---> " + job));
        }
    }

    private void stopProcess() {
        synchronized (HashCatServiceOld2.class) {
            if (hashCatProcess != null) {
                hashCatProcess.destroy();
                hashCatProcess = null;
            }
        }
    }

    /*
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
                            "-w", settings.getString(SETTING_POWER_USAGE, "2"),

                            // Enable automatic update of the status screen.
                            // Sets seconds between status screen updates to X.
                            "--status",
                            "--status-timer=" + settings.getString(SETTING_REFRESH_INTERVAL, "3"),

                            // Display the status view in a machine-readable format
                            "--machine-readable",

                            // Enable removal of hashes once they are cracked.
                            // Remained jobs will run faster.
                            "--remove",

                            // Disable the logfile. Not reading it here.
                            "--logfile-disable",

                            // TODO remove password field from Job
                            // Do not write potfile. Using app database instead.
                            //"--potfile-disable",

                            hashFile.getPath()
                    },
                    // No environment variables
                    null,
                    // Working directory
                    new File(hashCatPath)
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

        HashCatServiceOld.Progress progress = new HashCatServiceOld.Progress();
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
                        uiHandler.post(() -> job.setPassword(password));

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
     */
}
