package com.talybin.aircat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class NewJobActivity extends AppCompatActivity implements ApListAdapter.ClickListener {

    static final String EXTRA_PMKID = "PMKID";
    static final String EXTRA_SSID = "SSID";
    static final String EXTRA_AP_MAC = "AP_MAC";
    static final String EXTRA_CLIENT_MAC = "CLIENT_MAC";

    private RecyclerView apList;
    private RecyclerView.Adapter adapter;
    private TextView emptyView;

    private WifiManager wifiManager;
    private Timer scanTimer;
    private BroadcastReceiver scanResultsReceiver;

    private List<ApInfo> scanResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_job);

        // Job list
        apList = findViewById(R.id.ap_list);
        apList.setLayoutManager(new LinearLayoutManager(this));
        apList.setHasFixedSize(true);

        // Specify an adapter
        adapter = new ApListAdapter(scanResults, this);
        apList.setAdapter(adapter);

        // Empty view (visible when ap list is empty)
        emptyView = findViewById(R.id.ap_empty_view);

        // Wifi manager
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Enable wifi if not already
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, R.string.enabling_wifi, Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        // Populate scan results on event
        scanResultsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getScanResults();
            }
        };
        registerReceiver(
                scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // Initiate scan every 10 seconds
        scanTimer = new Timer();
        scanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                wifiManager.startScan();
            }
        }, 5000, 10000);

        // Initially read in current scan results
        getScanResults();

    }

    @Override
    protected void onDestroy() {
        scanTimer.cancel();
        unregisterReceiver(scanResultsReceiver);

        super.onDestroy();
    }

    private void getScanResults() {
        Map<String, ApInfo> filterMap = new HashMap<>();
        for (ScanResult sr : wifiManager.getScanResults()) {
            // Filter on WPA only
            if (sr.capabilities.contains("WPA")) {
                // Merge results with the same SSID except hidden ones
                String key = sr.SSID.isEmpty() ? sr.BSSID : sr.SSID;
                ApInfo info = filterMap.get(key);
                // Add if not exist
                if (info == null)
                    filterMap.put(key, new ApInfo(sr));
                else
                    info.merge(sr);
            }
        }
        scanResults.clear();
        scanResults.addAll(filterMap.values());

        // Sort by signal level
        Collections.sort(scanResults, (o1, o2) -> o2.level - o1.level);

        // Show empty view if no results available
        if (scanResults.isEmpty()) {
            apList.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            apList.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        // Redraw ap list with transition
        ChangeBounds cb = new ChangeBounds();
        cb.setDuration(500);
        TransitionManager.beginDelayedTransition(apList, cb);

        adapter.notifyDataSetChanged();
    }

    private interface EapolListener {
        void onResult(Eapol eapol, String err);
    }

    private Process fetchEapol(ApInfo apInfo, EapolListener callback) {
        String[] args = {
                "su",  "-c", getFilesDir() + "/tcpdump/tcpdump",
                "-i", Utils.getWirelessInterface(),
                "-c", "1",      // Read one packet
                "-s", "200",    // Set snaplen size to 200, EAPOL is just below this
                "-entqx",       // Dump payload as hex
                "\"ether proto 0x888e and ether[0x15:2] > 0\"", // Listen to EAPOL that has PMKID
        };
        try {
            Process process = Runtime.getRuntime().exec(args);
            InputStream is = process.getInputStream();
            int networkId = apInfo.connect(wifiManager);

            CompletableFuture.supplyAsync(() -> Eapol.fromStream(is)).handle((res, ex) -> {
                process.destroy();

                runOnUiThread(() -> {
                    // Clean up
                    wifiManager.removeNetwork(networkId);

                    if (ex != null)
                        callback.onResult(null, ex.getMessage());
                    else if (!res.isValid())
                        callback.onResult(null, getString(R.string.pmkid_not_supported, apInfo.getSSID()));
                    else
                        callback.onResult(res, null);
                });
                return null;
            });

            return process;
        }
        catch (Exception e) {
            callback.onResult(null, e.getMessage());
            return null;
        }
    }

    @Override
    public void onClick(ApInfo apInfo) {
        ProgressDialog waitDialog = new ProgressDialog(this);
        Handler giveUpHanlder = new Handler();
        Utils.Holder<Runnable> giveUpTask = new Utils.Holder<>();
        AtomicBoolean canceled = new AtomicBoolean(false);

        Process process = fetchEapol(apInfo, ((eapol, err) -> {
            giveUpHanlder.removeCallbacks(giveUpTask.get());
            waitDialog.dismiss();

            if (err != null) {
                if (!canceled.get())
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            }
            else {
                Intent replyIntent = new Intent();

                replyIntent.putExtra(EXTRA_PMKID, eapol.pmkId);
                replyIntent.putExtra(EXTRA_SSID, apInfo.ssid);
                replyIntent.putExtra(EXTRA_AP_MAC, eapol.apMac);
                replyIntent.putExtra(EXTRA_CLIENT_MAC, eapol.clientMac);

                setResult(RESULT_OK, replyIntent);
                finish();
            }
        }));

        if (process != null) {
            Runnable cancel = () -> {
                canceled.set(true);
                process.destroy();
            };
            // Give up after some amount of time
            giveUpTask.set(() -> {
                cancel.run();
                Toast.makeText(this, getString(R.string.no_answer, apInfo.getSSID()), Toast.LENGTH_LONG).show();
            });
            giveUpHanlder.postDelayed(giveUpTask.get(), 10000);

            // Show wait dialog
            waitDialog.setTitle(apInfo.getSSID());
            waitDialog.setMessage(getString(R.string.getting_pmkid));
            waitDialog.setCancelable(false);

            waitDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    getString(android.R.string.cancel),
                    (dialog, which) -> cancel.run());

            waitDialog.show();
        }
    }
}
