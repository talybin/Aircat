package com.talybin.aircat;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Eapol {

    public String apMac = null;
    public String clientMac = null;
    public String pmkId = null;

    @Override
    public String toString() {
        return "Eapol{" +
                "apMac='" + apMac + '\'' +
                ", clientMac='" + clientMac + '\'' +
                ", pmkId='" + pmkId + '\'' +
                '}';
    }

    public boolean isValid() {
        if (apMac == null || clientMac == null || pmkId == null)
            return false;
        // PMKID may be present but some APs do not support it
        // and clears it with zeroes
        for (char ch : pmkId.toCharArray()) {
            if (ch != '0')
                return true;
        }
        // PMKID is invalid
        return false;
    }

    static Eapol fromStream(InputStream is) {
        return fromStream(new InputStreamReader(is));
    }

    static Eapol fromStream(InputStreamReader irs) {

        Eapol info = new Eapol();
        Scanner scanner = new Scanner(irs);

        // Initial line example:
        // 38:ea:a7:7b:b2:91 > 6c:c7:ec:95:3d:63, EAPOL, length 113: EAPOL key (3) v1, len 95
        info.apMac = scanner.next();
        scanner.next();
        info.clientMac = scanner.next();
        info.clientMac = info.clientMac.substring(0, info.clientMac.length() - 1);

        // Go to data lines
        scanner.nextLine();

        // Data lines example:
        //	0x0010:  0166 03d4 9ecb 2c4b e465 cd40 0010 4870
        StringBuilder data = new StringBuilder();
        while (scanner.hasNext()) {
            String token = scanner.next();
            if (!token.startsWith("0x"))
                data.append(token);
        }

        scanner.close();

        if (data.length() >= 32)
            info.pmkId = data.substring(data.length() - 32);

        return info;
    }

    static Eapol fromLogcatStream(ApInfo apInfo, InputStream is) throws IOException {

        Eapol info = new Eapol();
        String line = new BufferedReader(new InputStreamReader(is)).readLine();

        Log.d("fetchEapol", "---> line [" + line + "]");

        String pmkId = line.substring(line.lastIndexOf(':') + 1).replace(" ", "");

        Log.d("fetchEapol", "---> pmkId [" + pmkId + "]");

        info.apMac = apInfo.bssid;
        // TODO get device mac
        info.clientMac = null;
        info.pmkId = pmkId;

        return info;
    }
}
