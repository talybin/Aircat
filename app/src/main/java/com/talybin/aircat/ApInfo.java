package com.talybin.aircat;

import android.net.wifi.ScanResult;

import java.util.HashSet;
import java.util.Set;

public class ApInfo {

    public boolean hidden;
    public String ssid;
    public String bssid;
    public int level;
    public Set<String> caps;

    ApInfo(ScanResult sr) {
        ssid = sr.SSID;
        hidden = ssid.isEmpty();
        bssid = sr.BSSID;
        level = sr.level;
        caps = new HashSet<>();

        setKnownCapabilities(sr);
    }

    String getSSID() {
        return hidden ? MainActivity.getContext().getString(R.string.hidden) : ssid;
    }

    void merge(ScanResult sr) {
        level = (sr.level + level) / 2;
        setKnownCapabilities(sr);
    }

    private void setKnownCapabilities(ScanResult sr)
    {
        String[] knownCaps = { "WPA3", "WPA2", "WPA", "WEP", "WPS" };
        String srCaps = sr.capabilities;

        for (int i = 0; (i = srCaps.indexOf('[', i) + 1) > 0;) {
            for (String known : knownCaps) {
                if (srCaps.startsWith(known, i)) {
                    caps.add(known);
                    i += known.length();
                    break;
                }
            }
        }

        // Detect OPEN network
        if (!srCaps.contains("WPA") && !srCaps.contains("WEP"))
            caps.add("OPEN");

        // Add frequency in GHz
        int f = sr.frequency / 1000;
        if (f == 2)
            caps.add("2.4 GHz");
        else
            caps.add(String.format("%d GHz", f));
    }

    /*
    private static int frequencyToChannel(int freq)
    {
        // see 802.11 17.3.8.3.2 and Annex J
        if (freq == 2484)
            return 14;
        else if (freq < 2484)
            return (freq - 2407) / 5;
        else if (freq >= 4910 && freq <= 4980)
            return (freq - 4000) / 5;
        else if (freq < 5945)
            return (freq - 5000) / 5;
        else if (freq <= 45000) // DMG band lower limit
            // see 802.11ax D4.1 27.3.22.2
            return (freq - 5940) / 5;
        else if (freq >= 58320 && freq <= 70200)
            return (freq - 56160) / 2160;
        else
            return 0;
    }
     */
}
