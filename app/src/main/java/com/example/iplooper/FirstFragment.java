package com.example.iplooper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import android.content.Intent;
import android.provider.Settings;


public class FirstFragment extends Fragment {
    private static final String TAG = "tag error";

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onClick(View view) {
                checkSystemWritePermission();
                if (android.os.Build.VERSION.SDK_INT > 9) {
                    // we don`'t care about running request in the same thread currently so disable this security
                    StrictMode.ThreadPolicy policy =
                            new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }
                // wait the ip to be the good one
                resetIpUntil();
            }
        });
    }

    public void resetIpUntil() {
        try {
            switchToAirplane(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    switchToAirplane(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Handler handler2 = new Handler();
                handler2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL url = new URL("https://api.ipify.org");
                            String ip = getUrlContent(url);
                            if(!ip.equals("212.65.104.4")){
                                resetIpUntil();
                            }
                            Log.d("ip is", ip);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 5000);
            }
        }, 5000);
    }

    public static void switchToAirplane(boolean enable) throws Exception {
        Process p = null;
        try{
            p = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(p.getOutputStream());
            dos.writeBytes("settings put global airplane_mode_on " + (enable ? "1" : "0") + "\n");
            dos.writeBytes("am broadcast -a android.intent.action.AIRPLANE_MODE" + "\n");
            dos.writeBytes("exit\n");
            ReadStream s1 = new ReadStream("stdin", p.getInputStream ());
            ReadStream s2 = new ReadStream("stderr", p.getErrorStream ());
            s1.start ();
            s2.start ();
            dos.flush();
            dos.close();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(p != null)
                p.destroy();
        }
    }

    private boolean checkSystemWritePermission() {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this.getContext());
            Log.d("TAG", "Can Write Settings: " + retVal);
            if(retVal){
                ///Permission granted by the user
            }else{
                //permission not granted navigate to permission screen
                openAndroidPermissionsMenu();
            }
        }
        return retVal;
    }

    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + this.getContext().getPackageName()));
        startActivity(intent);
    }

    private String getUrlContent(URL u) throws IOException {
        java.net.URLConnection c = u.openConnection();
        c.setConnectTimeout(2000);
        c.setReadTimeout(2000);
        c.connect();
        try (InputStream in = c.getInputStream()) {
            BufferedInputStream bis = new BufferedInputStream(in);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int result = bis.read();
            while(result != -1) {
                byte b = (byte)result;
                buf.write(b);
                result = bis.read();
            }
            return buf.toString();
        }
    }
}

class ReadStream implements Runnable {
    String name;
    InputStream is;
    Thread thread;
    public ReadStream(String name, InputStream is) {
        this.name = name;
        this.is = is;
    }
    public void start () {
        thread = new Thread (this);
        thread.start ();
    }
    public void run () {
        try {
            InputStreamReader isr = new InputStreamReader (is);
            BufferedReader br = new BufferedReader (isr);
            while (true) {
                String s = br.readLine ();
                if (s == null) break;
                System.out.println ("[" + name + "] " + s);
            }
            is.close ();
        } catch (Exception ex) {
            System.out.println ("Problem reading stream " + name + "... :" + ex);
            ex.printStackTrace ();
        }
    }
}