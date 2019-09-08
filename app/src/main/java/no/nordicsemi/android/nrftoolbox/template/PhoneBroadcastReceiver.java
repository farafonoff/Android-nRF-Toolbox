package no.nordicsemi.android.nrftoolbox.template;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static no.nordicsemi.android.nrftoolbox.profile.BleProfileService.EXTRA_DEVICE_ADDRESS;

public class PhoneBroadcastReceiver extends BroadcastReceiver {
    TemplateManager bracelet;
    public PhoneBroadcastReceiver() {

    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String phoneNr = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
        if (phoneNr == null) phoneNr = "Null number";
        String state = bundle.getString(TelephonyManager.EXTRA_STATE);
        /*if (bracelet == null) {
            Intent serviceIntent = new Intent(context, TemplateService.class);
            serviceIntent.putExtra(EXTRA_DEVICE_ADDRESS, "46:31:35:80:04:A8");
            TemplateService.TemplateBinder myBinder = (TemplateService.TemplateBinder) this.peekService(context, serviceIntent);
            int rings = 3;
            if (state.equals(TelephonyManager.CALL_STATE_OFFHOOK)) {
                rings = 1;
            }
            myBinder.notifyCall(phoneNr, rings);
        }
        if (bracelet != null && bracelet.isConnected()) {
            bracelet.notifyCall(phoneNr);
        }*/
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            TemplateService.startForCall(context, phoneNr);
            Toast.makeText(context,
                    "Incoming: "+state+phoneNr,
                    Toast.LENGTH_LONG).show();
        }
    }
}
