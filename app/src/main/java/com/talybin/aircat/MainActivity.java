package com.talybin.aircat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Make sure both JobManager and WordListManager initialize in UI thread
        WordListManager.getInstance();
        JobManager.getInstance();

        installDependencies();

        // Listen to activity specific settings changes
        App.settings().registerOnSharedPreferenceChangeListener(this);

        // Apply settings
        onSharedPreferenceChanged(App.settings(), "keep_screen_on");
        //Log.d("MainActivity", "---> onCreate");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.settings().unregisterOnSharedPreferenceChangeListener(this);

        // Activity is about to be destroyed.
        // Leaving listeners attached to UI result in crash.
        JobManager.getInstance().removeListeners();
        //Log.d("MainActivity", "---> onDestroy");
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

    // Setting up the navigation back button
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, (DrawerLayout) null);
    }

    private void installDependencies() {
        // Check if already installed
        String[] installed = getFilesDir().list();
        if (installed != null && installed.length > 0)
            return;

        // Install in background, should not take a long time
        App.getThreadPool().execute(() -> {
            final String[] executables = {
                    "hashcat/hashcat", "tcpdump",
            };
            if (Utils.unpackRawZip(R.raw.assets, executables))
                Log.d("MainActivity", "install complete");
            else
                runOnUiThread(() -> new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.extraction_error)
                        .setMessage(R.string.extraction_error_msg)
                        .setNegativeButton(android.R.string.ok, null)
                        .show());
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (key.equals("keep_screen_on")) {
            boolean value = pref.getBoolean(key, false);
            int flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

            if (value)
                getWindow().addFlags(flags);
            else
                getWindow().clearFlags(flags);
        }
        else if (key.equals("wake_lock")) {
            boolean value = pref.getBoolean(key, false);
            HashCatService.enableWakeLock(value);
        }
    }
}
