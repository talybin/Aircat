package com.talybin.aircat;

public class Job {

    public String apMac;
    public String clientMac;
    public String ssid;
    public String pmkId;

    public Job(ApInfo apInfo) {
        apMac = apInfo.bssid;
        clientMac = apInfo.clientMac;
        ssid = apInfo.ssid;
        pmkId = apInfo.pmkId;
    }

    // Constructor for testing
    public Job() {
        apMac = clientMac = ssid = pmkId = null;
    }

    //public String getHash() {
    //    return pmkId;
    //}
}
