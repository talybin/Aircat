package com.talybin.aircat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static WeakReference<MainActivity> weakContext;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weakContext = new WeakReference<>(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add navigation back button to action bar
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController);

        // Following permissions need to be both in Manifest and at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // 87 = PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 87);
            // After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }

        installDependencies();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // May return null if context is closed
    public static MainActivity getContext() {
        return weakContext.get();
    }

    // Setting up the navigation back button
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, (DrawerLayout) null);
    }

    private void installDependencies() {
        File filesDir = getFilesDir();
        String filesPath = filesDir.toString();

        // Setup constants
        //HashCat2.setExePath(filesPath + "/hashcat/hashcat");
        TcpDump.setExePath(filesPath + "/tcpdump/tcpdump");
        //WordLists.setBuiltInPath(filesPath + "/wordlists/built-in.txt");
        //WordLists.setBuiltInPath(filesPath + "/wordlists/english.txt");

        // Check if already installed
        String[] installed = filesDir.list();
        if (installed != null && installed.length > 0)
            return;

        // Install in background, should not take a long time
        new Thread() {
            @Override
            public void run() {
                final String[] executables = {
                        "hashcat/hashcat",
                        "tcpdump/tcpdump",
                };
                if (Utils.unpackRawZip(getContext(), R.raw.assets, executables))
                    Log.d("MainActivity", "install complete");
                else
                    runOnUiThread(() -> new AlertDialog.Builder(getContext())
                            .setTitle(R.string.extraction_error)
                            .setMessage(R.string.extraction_error_msg)
                            .setNegativeButton(android.R.string.ok, null)
                            .show());
            }
        }.start();
    }
}
