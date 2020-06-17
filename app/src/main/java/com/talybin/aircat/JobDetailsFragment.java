package com.talybin.aircat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;


public class JobDetailsFragment extends Fragment implements Job.Listener {

    private static final int FILE_SELECT_CODE = 0;

    private BottomSheetDialog bottomDialog;

    private JobManager jobManager = null;
    private Job job = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_job_details, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        jobManager = JobManager.getInstance();

        // Retrieve the job
        Bundle args = getArguments();
        if (args != null)
            job = jobManager.getJobs().get(args.getInt("job_position"));
        if (job == null) {  // Should never happen
            Log.e(this.getClass().getName(), "Missing job argument");
            goBack();
            return;
        }

        View jobItem = view.findViewById(R.id.job_details_item);
        JobListAdapter.JobViewHolder viewHolder = new JobListAdapter.JobViewHolder(jobItem);
        viewHolder.bindData(job);

        // Alternatives for recovered password
        bottomDialog = new BottomSheetDialog(requireContext());
        bottomDialog.setContentView(R.layout.job_item_bottom_sheet);

        // Fill fields
        ((TextView)view.findViewById(R.id.job_details_mac_ap)).setText(job.getApMac());
        ((TextView)view.findViewById(R.id.job_details_mac_client)).setText(job.getClientMac());
        ((TextView)view.findViewById(R.id.job_details_pmkid)).setText(job.getHash());
        ((TextView)view.findViewById(R.id.job_details_wordlist)).setText(job.getWordListFileName());

        // Click listeners
        jobItem.setOnClickListener(v -> showBottomMenu());
        view.findViewById(R.id.job_details_hash_info).setOnClickListener(this::onViewClick);
        view.findViewById(R.id.job_details_wordlist_info).setOnClickListener(this::onViewClick);

        final int[] jobActions = {
                R.id.job_action_copy,
                R.id.job_action_connect,
        };
        for (int id : jobActions) {
            View v = Objects.requireNonNull(bottomDialog.findViewById(id));
            v.setOnClickListener(this::onViewClick);
        }

        job.addListener(this);
    }

    @Override
    public void onDestroyView() {
        job.removeListener(this);
        job = null;
        bottomDialog = null;

        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.job_detail_menu, menu);
        menu.findItem(R.id.action_select_all).setVisible(false);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (job != null) {
            boolean notRunning = job.getState() == Job.State.NOT_RUNNING;
            menu.findItem(R.id.action_pause).setVisible(!notRunning);
            menu.findItem(R.id.action_start).setVisible(notRunning);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start:
                startJob();
                return true;
            case R.id.action_pause:
                pauseJob();
                return true;
            case R.id.action_remove:
                removeJob();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goBack() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void startJob() {
        Context context = getContext();
        if (!job.start(context))
            Toast.makeText(context, R.string.failed_to_start_job, Toast.LENGTH_LONG).show();
    }

    private void pauseJob() {
    }

    private void removeJob() {
        jobManager.remove(job);
        goBack();
    }

    @Override
    public void onJobStateChange(Job job) {
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public void onHashCatProgressChange(Job job, HashCat.Progress progress) {}

    private void showBottomMenu() {
        // This menu is for actions on retrieved password
        if (job.getPassword() == null)
            return;

        bottomDialog.show();
    }

    private void onViewClick(View view) {
        bottomDialog.dismiss();
        Context ctx = requireContext();

        switch (view.getId()) {
            case R.id.job_action_copy:
                if (copyToClipboard(ctx.getString(R.string.password), job.getPassword()))
                    Toast.makeText(ctx, R.string.password_clipped, Toast.LENGTH_SHORT).show();
                break;

            case R.id.job_details_wordlist_info:
                browseForWordlist();
                break;

            case R.id.job_details_hash_info:
                if (copyToClipboard("hashcat", HashCat.makeHash(job)))
                    Toast.makeText(ctx, R.string.hash_clipped, Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(getContext(), R.string.not_implemented_yet, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private boolean copyToClipboard(String name, String data) {
        Context ctx = requireContext();
        ClipboardManager clipboard =
                (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(name, data));
            return true;
        }
        else
            Toast.makeText(ctx, R.string.operation_failed, Toast.LENGTH_LONG).show();
        return false;
    }

    private void browseForWordlist() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(
                    intent, getString(R.string.choose_wordlist)), FILE_SELECT_CODE);
        }
        catch (Exception e) {
            Toast.makeText(getContext(),
                    getString(R.string.error_msg, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FILE_SELECT_CODE)
            return;
        if (resultCode != RESULT_OK)
            return;

        Uri uri = data.getData();
        job.setWordList(uri);
        ((TextView)requireView().findViewById(
                R.id.job_details_wordlist)).setText(job.getWordListFileName());

        //Utils.retrieveNrRows(getContext(), uri);


        //ContentResolver cr = getContext().getContentResolver();

        Log.d("onActivityResult", "---> 1: " + uri.toString());
        //Log.d("onActivityResult", "---> 2: " + Utils.getUriSize(cr, job.getWordList()));
/*
        try {
            Uri uri2 = Uri.parse("http://downloads.skullsecurity.org/passwords/rockyou.txt.bz2");
            AssetFileDescriptor afd = cr.openAssetFileDescriptor(uri2, "r");
            Log.d("onActivityResult", "---> 2: " + afd.getLength());
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

 */

        //Log.d("onActivityResult", "---> 3: " + cr.canonicalize(uri).toString());

/*
        Log.d("onActivityResult", "---> File Uri: " + uri.toString());

        // TODO hashcat cat read from stdin
        //getContext().getContentResolver().openInputStream(uri)

        Log.d("onActivityResult", "---> env: " + Environment.getDataDirectory());
        Log.d("onActivityResult", "---> 1: " + uri.getHost());
        Log.d("onActivityResult", "---> 2: " + uri.getLastPathSegment());
        Log.d("onActivityResult", "---> 3: " + uri.getPath());
        Log.d("onActivityResult", "---> 4: " + uri.getEncodedPath());
        Log.d("onActivityResult", "---> 5: " + Uri.decode(uri.toString()));

        Uri uri2 = Uri.parse(uri.toString());
        try {
            Uri uri2 = Uri.parse("http://downloads.skullsecurity.org/passwords/rockyou.txt.bz2");
            InputStream is = getContext().getContentResolver().openInputStream(uri2);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Log.d("onActivityResult", "---> line: " + br.readLine().length());
            //String line;
            //while ((line = br.readLine()) != null) {
            //    Log.d("onActivityResult", "---> line: " + line);
            //}
        }
        catch (Exception e) {
            Log.d("onActivityResult", "---> error: " + e.getMessage());
        }
*/
    }
}
