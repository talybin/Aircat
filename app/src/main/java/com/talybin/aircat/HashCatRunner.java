package com.talybin.aircat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

public class HashCatRunner {

    private static final String LOG_TAG = "HashCatRunner";

    public interface Listener {
    }

    private class Task extends AsyncTask<Job, String, String> {

        private Process hashCatProcess = null;

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

            Path workingDir = Paths.get(Utils.hashCatPath).getParent();
            publishProgress(Utils.hashCatPath, workingDir.toString(), "cwd: " + Paths.get(".").toAbsolutePath().normalize().toString());

            try {
                // Create hash file
                File hashFile = File.createTempFile("hashcat-", ".hash");
                hashFile.deleteOnExit();

                // Store hash
                bufferedWriter = new BufferedWriter(new FileWriter(hashFile));
                bufferedWriter.write(hash);
                bufferedWriter.close();
                bufferedWriter = null;

                //publishProgress(hashFile.getAbsolutePath(), job.wordlistPath, "[" + hash + "]");

                //String args = " -m 16800 -a 0 --status --status-timer=10 --machine-readable " +
                //        hashFile.getAbsolutePath() + " " + job.wordlistPath;

                ProcessBuilder processBuilder = new ProcessBuilder(
                        //Utils.hashCatPath,
                        "./hashcat",
                        "-m", "16800",
                        "-a", "0",
                        "--status", "--status-timer=10",
                        "--machine-readable",
                        hashFile.getAbsolutePath(),
                        job.wordlistPath
                );

                Map<String, String> env = processBuilder.environment();
                env.put("LD_LIBRARY_PATH", workingDir.toString());

                processBuilder.directory(workingDir.toFile());

                hashCatProcess = processBuilder.start();

/*
                hashCatProcess = Runtime.getRuntime().exec(
                        Utils.hashCatPath + args,
                        new String[] {
                                "LD_LIBRARY_PATH=" + workingDir.toString(),
                        },
                        workingDir.toFile()
                );
 */

                publishProgress(hashCatProcess.toString(), processBuilder.directory().toString(), processBuilder.command().toString());

                //hashCatProcess = Runtime.getRuntime().exec(Utils.hashCatPath + args);
                //iss = new InputStreamReader(hashCatProcess.getInputStream());
                iss = new InputStreamReader(hashCatProcess.getErrorStream());
                scanner = new Scanner(iss);

                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    Log.d(LOG_TAG, "---> line [" + line + "]");
                }

            } catch (IOException e) {
                e.printStackTrace();

                if (bufferedWriter != null)
                    try { bufferedWriter.close();  } catch (IOException ignored) {}
                // Abort
                if (hashCatProcess != null && hashCatProcess.isAlive())
                    hashCatProcess.destroy();
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
    }

    public HashCatRunner(Job job) {
        new Task().execute(job);
    }
}
