package com.example.iplooper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;


public class IpLooper {
    private boolean stopLoop;

    public void setStopLoop(boolean bool){
        this.stopLoop = bool;
    }

    public String resetIp(ConnectivityManager cm) throws Exception {
        UtilNetwork.switchToAirplane(true);
        UtilNetwork.switchToAirplane(false);
        while(!has4GConnection(cm)) {
            Thread.sleep(500);
        }
        String ip = UtilNetwork.getPublicIp();
        Log.d("Borto ip is", ip);
        return ip;
    }

    private boolean has4GConnection(ConnectivityManager cm) {
        for (NetworkInfo ni : cm.getAllNetworkInfo()) {
            if (ni.getTypeName().equalsIgnoreCase("MOBILE") && ni.isConnected()) {
                return true;
            }
        }
        return false;
    }
}

