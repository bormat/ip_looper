package com.example.iplooper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public final class UtilNetwork {
       
    public static void switchToAirplane(boolean enable) throws Exception {
        try{
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(p.getOutputStream());
            dos.writeBytes("settings put global airplane_mode_on " + (enable ? "1" : "0")
                    + "\nam broadcast -a android.intent.action.AIRPLANE_MODE\nexit\n");
            (new Thread (() -> { logStream("stdin", p.getInputStream ()); })).start();
            (new Thread (() -> { logStream("stderr", p.getErrorStream ()); })).start();
            dos.flush();
            dos.close();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    

    static String getPublicIp() throws IOException {
        return getUrlContent(new URL("https://api.ipify.org"));
    }
    

    private static String getUrlContent(URL u) {
        String[] ip = new String[1];
        Thread th = (new Thread (() -> {
            java.net.URLConnection c = null;
            try {
                c = u.openConnection();
                c.setConnectTimeout(2000);
                c.setReadTimeout(2000);
                c.connect();
                InputStream in = c.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(in);
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                int result = bis.read();
                while(result != -1) {
                    buf.write((byte) result);
                    result = bis.read();
                }
                ip[0] = buf.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        th.start();
        try {
            th.join();  // wait the end of the thread
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ip[0];
    }

    private static void logStream(String name, InputStream is) {
        try {
            BufferedReader br = new BufferedReader (new InputStreamReader (is));
            while (true) {
                String s = br.readLine ();
                if (s == null) break;
                System.out.println ("ReadStream[" + name + "] " + s);
            }
            is.close ();
        } catch (Exception ex) {
            System.out.println ("Problem with ReadStream[\" + name + \"] " + name + "... :" + ex);
            ex.printStackTrace ();
        }
    }
}

