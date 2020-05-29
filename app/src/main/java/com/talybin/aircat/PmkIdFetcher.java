package com.talybin.aircat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class PmkIdFetcher extends RootShell implements Runnable {

    private static final String tcpdumpBinaryPath = "/data/data/com.talybin.aircat/bin/tcpdump";

    public PmkIdFetcher() {
        super();
    }

    @Override
    public void run() {
        int ret = openShell();
        if (ret != 0)
            return;

        ret = runCommand(tcpdumpBinaryPath + generateEapolParams());
        if (ret == 0) {
            try {
                process.waitFor();
                if (process.exitValue() != 0) {
                    // error
                }
            } catch (InterruptedException e) {
                // error
            }
        }

        closeShell();
    }

    public void abort() {
        closeShell();
    }

    private static String generateEapolParams() {
        // Listen to EAPOL that has PMKID
        final String filter = " \"ether proto 0x888e and ether[0x15:2] > 0\"";

        // Defining a String which will contain the parameters.
        return  " -i " + getWirelessInterface()  // Recognizing the chosen interface
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
