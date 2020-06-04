package com.talybin.aircat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.widget.Toast;

public class FetchPmkId {

    public interface Listener {
        void onReceive(Eapol eapol);
    }

    private Context context;
    private WifiManager wifiManager;

    private ApInfo apInfo;
    private Listener listener;

    private GetEapolTask task;
    private ProgressDialog waitDialog;

    // Give up timer
    private Handler giveUpHandler;
    private Runnable giveUpTask;

    // Store network id to remove network after fetch is complete
    private int networkId = -1;

    public FetchPmkId(Context context, ApInfo apInfo, Listener listener) {

        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        this.apInfo = apInfo;
        this.listener = listener;

        this.waitDialog = new ProgressDialog(context);
        this.giveUpHandler = new Handler();

        setup();
        run();
    }

    private void setup() {

        // Setup wait dialog

        this.waitDialog.setTitle(apInfo.getSSID());
        this.waitDialog.setMessage(context.getString(R.string.getting_pmkid));
        this.waitDialog.setCancelable(false);

        this.waitDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                context.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        abort();
                    }
                });

        // Give up after some amount of time

        this.giveUpTask = new Runnable() {
            @Override
            public void run() {
                abort();
                toast(R.string.no_answer, apInfo.getSSID());
            }
        };

        // Create PMKID fetcher

        this.task = new GetEapolTask(new GetEapolTask.Listener() {
            @Override
            public void onStart() {
                networkId = apInfo.connect(wifiManager);
            }

            @Override
            public void onComplete(GetEapolTask task, Eapol info) {
                cleanup();

                if (info == null) {
                    toast(R.string.error_occurred);
                }
                else if (!info.isValid()) {
                    toast(R.string.pmkid_not_supported, apInfo.getSSID());
                }
                else { // Successfully fetched PMKID
                    listener.onReceive(info);
                }
            }
        });
    }

    private void run() {
        waitDialog.show();
        giveUpHandler.postDelayed(giveUpTask, 10000);
        task.execute();
    }

    private void cleanup() {
        waitDialog.dismiss();
        giveUpHandler.removeCallbacks(giveUpTask);
        if (networkId != -1) {
            wifiManager.removeNetwork(networkId);
            networkId = -1;
        }
    }

    public void abort() {
        task.abort();
        cleanup();
    }

    private void toast(int resId, Object... formatArgs) {
        String txt = String.format(context.getString(resId), formatArgs);
        Toast.makeText(context, txt, Toast.LENGTH_LONG).show();
    }
}
