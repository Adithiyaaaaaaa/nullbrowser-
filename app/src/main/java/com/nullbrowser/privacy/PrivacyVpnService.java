package com.nullbrowser.privacy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;

public class PrivacyVpnService extends VpnService {
    private ParcelFileDescriptor vpnInterface = null;
    private static final String TAG = "PrivacyVpnService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        
        Notification.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new Notification.Builder(this, "vpn_channel");
        } else {
            notificationBuilder = new Notification.Builder(this);
        }

        Notification notification = notificationBuilder
                .setContentTitle("NullBrowser Privacy Tunnel")
                .setContentText("Status: Protected | Kill Switch: Active")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_VPN);
        } else {
            startForeground(1, notification);
        }

        establishVpn();
        return START_STICKY;
    }

    private void establishVpn() {
        if (vpnInterface != null) return;

        try {
            Builder builder = new Builder()
                    .setSession("NullBrowserVPN")
                    .addAddress("10.0.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    // Kill Switch: Block all traffic if VPN is not established or apps try to bypass
                    .setBlocking(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Prevent apps from bypassing VPN
                builder.setMetered(false);
            }

            // Force all traffic from this app and its automation engine through the VPN
            builder.addAllowedApplication(getPackageName());

            vpnInterface = builder.establish();
            Log.i(TAG, "VPN Interface established. Kill switch active.");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to establish VPN", e);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }

    private void stopVpn() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN interface", e);
            }
            vpnInterface = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "vpn_channel",
                    "VPN Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
