package com.talybin.aircat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CreateJobFragment extends Fragment implements ApListAdapter.ClickListener {

    private RecyclerView apList;
    private RecyclerView.Adapter adapter;
    private TextView emptyView;

    private Context context;

    private WifiManager wifiManager;
    private Timer scanTimer;
    private BroadcastReceiver scanResultsReceiver;

    private List<ApInfo> scanResults = new ArrayList<>();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_job, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = requireActivity();

        // Job list
        apList = view.findViewById(R.id.ap_list);
        apList.setLayoutManager(new LinearLayoutManager(context));
        apList.setHasFixedSize(true);

        // Specify an adapter
        adapter = new ApListAdapter(scanResults, this);
        apList.setAdapter(adapter);

        // Empty view (visible when ap list is empty)
        emptyView = view.findViewById(R.id.ap_empty_view);

        // Wifi manager
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        // Enable wifi if not already
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(context, R.string.enabling_wifi, Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        // Populate scan results on event
        scanResultsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getScanResults();
            }
        };
        context.registerReceiver(
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
    public void onDestroyView() {
        scanTimer.cancel();
        context.unregisterReceiver(scanResultsReceiver);

        super.onDestroyView();
    }

    private void getScanResults()
    {
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
        Collections.sort(scanResults, new Comparator<ApInfo>() {
            @Override
            public int compare(ApInfo o1, ApInfo o2) {
                return o2.level - o1.level;
            }
        });

        // Show empty view if no results available
        if (scanResults.isEmpty()) {
            apList.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
        else {
            apList.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        // Redraw ap list with transition
        ChangeBounds cb = new ChangeBounds();
        cb.setDuration(500);
        TransitionManager.beginDelayedTransition(apList, cb);

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(final ApInfo apInfo) {
        new FetchPmkId(requireContext(), apInfo, new FetchPmkId.Listener() {
            @Override
            public void onReceive(Eapol eapol) {
                JobManager.getInstance().add(apInfo.update(eapol));
                goBack();
            }
        });
    }

    private void goBack() {
        NavHostFragment.findNavController(this).popBackStack();
    }
}
