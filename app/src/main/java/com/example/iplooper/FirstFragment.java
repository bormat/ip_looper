package com.example.iplooper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.provider.Settings;
import android.widget.TextView;

public class FirstFragment extends Fragment {
    private static final String TAG = "tag error";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        IpLooper ipLooper = new IpLooper();
        Context context = this.getContext();
        TextView tv =  view.findViewById(R.id.ip_display2);
        tv.setText("hello2");
        FirstFragment that = this;
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View view) {
                if(checkSystemWritePermission()) {
                    new MyTask(tv, cm).execute();
                }
            }
        });

        view.findViewById(R.id.stop_loop).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View view) {
                if(checkSystemWritePermission()) {
                    // wait the ip to be the good one
                    ipLooper.setStopLoop(true);
                }
            }
        });
    }

    private boolean checkSystemWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this.getContext())) {
            Log.d("TAG", "Permission not granted navigate to permission screen");
            openAndroidPermissionsMenu();
            return false;
        }
        return true;
    }

    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + this.getContext().getPackageName()));
        startActivity(intent);
    }

    // allow to have an update of the UI and a background process in another thread
    private static class MyTask extends AsyncTask<Void, String, String> {

        private TextView tv;
        private IpLooper ipLooper;
        private ConnectivityManager cm;
        private int loopDone;
        MyTask(TextView tv, ConnectivityManager cm) {
            ipLooper = new IpLooper();
            // everything must be week reference to avoid memory leak concerning the activity
            this.tv = tv;
            this.cm = cm;
        }

        @Override
        protected String doInBackground(Void... params) {
            // do some long running task...
            String ip = "";
            for (int i = 0; i < 3000; i++) {
                try {
                    ip = ipLooper.resetIp(this.cm);
                } catch (Exception e) {
                    ip = e.toString();
                    e.printStackTrace();
                }
                // update the ui
                this.loopDone = i;
                publishProgress(ip);
                if (isCancelled() || ip.equals("212.65.104.4")) {
                    break;
                }
            }
            return ip;
        }

        protected void onProgressUpdate(String... ips) {
            tv.setText("Ip is " + ips[0] + "current loop is " + loopDone);
        }

        protected void onPostExecute(String ip) {
            tv.setText("Final Ip is " + ip + "current loop is " + loopDone);
        }
    }
}