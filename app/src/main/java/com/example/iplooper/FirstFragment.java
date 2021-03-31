package com.example.iplooper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
                    StrictMode.ThreadPolicy policy =
                            new StrictMode.ThreadPolicy.Builder().permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                }
                URL url = null;
                try {
                    SetAirplaneMode(true);
                    //SetAirplaneMode(false);
                    url = new URL("https://api.ipify.org");
                    String ip = getUrlContent(url);
                    Log.d("ip is", ip);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
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


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void SetAirplaneMode(boolean enabled){
        //---toggle Airplane mode---
       Settings.Global.putInt(getContext().getContentResolver(),
             Settings.Global.AIRPLANE_MODE_ON, enabled ? 1 : 0);
       // disable4GNetwork();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setMobileConnectionEnabled(getContext(), false);
        }
        // Post an intent to reload.
        //Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        //intent.putExtra("state", !enabled);
        //getContext().sendBroadcast(intent);
    }

    private void disable4GNetwork() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(getContext().CONNECTIVITY_SERVICE);
        NetworkRequest.Builder request;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            request = new NetworkRequest.Builder();

            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            connectivityManager.requestNetwork(request.build(), new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ConnectivityManager.setProcessDefaultNetwork(network);
                    }
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean setMobileConnectionEnabled(Context context, boolean enabled)
    {
        try{
            // Requires: android.permission.CHANGE_NETWORK_STATE
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD){
                // pre-Gingerbread sucks!
                final TelephonyManager telMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                final Method getITelephony = telMgr.getClass().getDeclaredMethod("getITelephony");
                getITelephony.setAccessible(true);
                final Object objITelephony = getITelephony.invoke(telMgr);
                final Method toggleDataConnectivity = objITelephony.getClass()
                        .getDeclaredMethod(enabled ? "enableDataConnectivity" : "disableDataConnectivity");
                toggleDataConnectivity.setAccessible(true);
                toggleDataConnectivity.invoke(objITelephony);
            }
            // Requires: android.permission.CHANGE_NETWORK_STATE
            else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                final ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                // Gingerbread to KitKat inclusive
                final Field serviceField = connMgr.getClass().getDeclaredField("mService");
                serviceField.setAccessible(true);
                final Object connService = serviceField.get(connMgr);
                try{
                    final Method setMobileDataEnabled = connService.getClass()
                            .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                    setMobileDataEnabled.setAccessible(true);
                    setMobileDataEnabled.invoke(connService, Boolean.valueOf(enabled));
                }
                catch(NoSuchMethodException e){
                    // Support for CyanogenMod 11+
                    final Method setMobileDataEnabled = connService.getClass()
                            .getDeclaredMethod("setMobileDataEnabled", String.class, Boolean.TYPE);
                    setMobileDataEnabled.setAccessible(true);
                    setMobileDataEnabled.invoke(connService, context.getPackageName(), Boolean.valueOf(enabled));
                }
            }
            // Requires: android.permission.MODIFY_PHONE_STATE (System only, here for completions sake)
            else{
                // Lollipop and into the Future!
                final TelephonyManager telMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
                final Method setDataEnabled = telMgr.getClass().getDeclaredMethod("setDataEnabled", Boolean.TYPE);
                setDataEnabled.setAccessible(true);
                setDataEnabled.invoke(telMgr, Boolean.valueOf(enabled));
            }
            return true;
        }
        catch(NoSuchFieldException e){
            Log.e(TAG, "setMobileConnectionEnabled", e);
        }
        catch(IllegalAccessException e){
            Log.e(TAG, "setMobileConnectionEnabled", e);
        }
        catch(IllegalArgumentException e){
            Log.e(TAG, "setMobileConnectionEnabled", e);
        }
        catch(NoSuchMethodException e){
            Log.e(TAG, "setMobileConnectionEnabled", e);
        }
        catch(InvocationTargetException e){
            Log.e(TAG, "setMobileConnectionEnabled", e);
            Log.e(TAG, "setMobileConnectionEnabled cause iiiiis", e.getCause());

        }
        return false;
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