package com.talybin.aircat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;

public class TcpDump extends AsyncTask<Void, String, Eapol> {

    public static String getExePath() {
        return App.getContext().getFilesDir() + "/tcpdump/tcpdump";
    }

    public interface Listener {
        // When process is started onStart() is invoked.
        // Note, invokes from AsyncTask thread, not from UI thread
        void onStart();

        // Invokes when Eapol information available
        void onComplete(TcpDump task, Eapol info);
    }

    private static final String LOG_TAG = "TcpDump";

    private Listener listener;
    private Process process = null;

    public TcpDump(Listener listener) {
        super();
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        process = null;
    }

    @Override
    protected Eapol doInBackground(Void... voids) {

        InputStreamReader iss = null;
        Eapol info = null;

        try {
            process = Runtime.getRuntime().exec(
                    "su -c " + getExePath() + generateEapolParams());
            iss = new InputStreamReader(process.getInputStream());

            listener.onStart();

            info = Eapol.fromStream(iss);
        }
        catch (Exception e) {
            if (!isCancelled()) {
                Log.e(LOG_TAG, "error: " + e.toString());
                // Abort
                if (process != null && process.isAlive())
                    process.destroy();
            }
        }
        finally {
            if (iss != null)
                try { iss.close(); } catch (IOException ignored) {}
        }

        Log.d(LOG_TAG, "process ended");
        return info;
    }

    @Override
    protected void onProgressUpdate(String... values) {
    }

    @Override
    protected void onPostExecute(Eapol result) {
        listener.onComplete(this, result);
    }

    public void abort() {
        if (process != null && process.isAlive()) {
            // Set canceled flag
            cancel(false);
            process.destroy();
        }
    }

    private static String generateEapolParams() {
        // Listen to EAPOL that has PMKID
        final String filter = " \"ether proto 0x888e and ether[0x15:2] > 0\"";

        // Defining a String which will contain the parameters.
        return  " -i " + Utils.getWirelessInterface()  // Recognizing the chosen interface
                + " -c 1"       // Read one packet
                + " -s 200"     // Set snaplen size to 200, EAPOL is just below this
                + " -entqx"     // Dump payload as hex
                + filter;
    }
}
