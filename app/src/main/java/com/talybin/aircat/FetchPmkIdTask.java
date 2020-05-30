package com.talybin.aircat;

import android.os.AsyncTask;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class FetchPmkIdTask extends AsyncTask<Void, String, String> {

    private static final String tcpdumpPath = "/data/data/com.talybin.aircat/executables/tcpdump";

    private RootShell rootShell;
    private String tcpdumpCmd;
    private String lastError;

    public FetchPmkIdTask() {
        super();

        this.lastError = null;
        this.rootShell = new RootShell();
        //this.tcpdumpCmd = tcpdumpPath + " " + generateEapolParams() + "&";
        this.tcpdumpCmd = "ls /";
    }

    String getLastError() {
        return lastError;
    }

    @Override
    protected void onPreExecute() {
        int ret = rootShell.openShell();
        if (ret != 0) {
            rootShell = null;
            lastError = "failed to execute root shell";
        }

    }

    @Override
    protected String doInBackground(Void... voids) {
        if (rootShell == null)
            return null;

        int ret = rootShell.runCommand(tcpdumpCmd);
        if (ret != 0) {
            lastError = "failed to execute command";
            return null;
        }
        if (ret == 0) {
            try {
                rootShell.getProcess().waitFor();
                if (rootShell.getProcess().exitValue() != 0) {
                    // error
                }
            } catch (InterruptedException e) {
                // error
            }
        }

        return "ok";
    }

    @Override
    protected void onProgressUpdate(String... values) {
    }

    @Override
    protected void onPostExecute(String result) {
        if (rootShell != null)
            rootShell.closeShell();
    }

    private static String generateEapolParams() {
        // Listen to EAPOL that has PMKID
        final String filter = " \"ether proto 0x888e and ether[0x15:2] > 0\"";

        // Defining a String which will contain the parameters.
        return  "-i " + getWirelessInterface()  // Recognizing the chosen interface
                + " -c 1"       // Read one packet
                + " -s 200"     // Set snaplen size to 200, EAPOL is just below this
                + " -entqx"     // Dump payload as hex
                + filter;
    }

    // Try to find wireless interface.
    // Return a string with name of wireless interface, otherwise, if
    // not found or found more than one, return "any".
    private static String getWirelessInterface() {
        String interfaceName = null;
        String devListPath = "/sys/class/net";
        File folder = new File(devListPath);

        // Iterate over all interfaces
        for (File devFile : Objects.requireNonNull(folder.listFiles())) {
            // Check "/sys/class/net/<dev>/wireless"
            boolean isWireless = Files.exists(
                    Paths.get(devListPath, devFile.getName(), "wireless"));
            if (isWireless) {
                if (interfaceName != null) {
                    // Found more than one
                    interfaceName = null;
                    break;
                }
                // First match
                interfaceName = devFile.getName();
            }
        }

        return (interfaceName != null) ? interfaceName : "any";
    }
}
