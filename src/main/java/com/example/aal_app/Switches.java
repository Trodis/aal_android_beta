package com.example.aal_app;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.UDAServiceId;

import java.util.ArrayList;


/**
 * @author Ferhat Özmen
 */
public class Switches extends Activity {

    Device mydevice;
    Service switchPower;
    ServiceId serviceId = new UDAServiceId("SwitchPower");

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);

        Bundle extras = getIntent().getExtras();
        int position = extras.getInt(MainActivity.EXTRA_MESSAGE);

        DeviceDisplay deviceDisplay = MainActivity.listAdapter.getItem(position);
        this.mydevice = deviceDisplay.getDevice();

        TextView t;
        t = (TextView) findViewById(R.id.upnpSwitch);
        t.setText(deviceDisplay.getServiceTypeOfDevice());

        t = (TextView) findViewById(R.id.textDeviceName);
        t.setText("Gerät: " + deviceDisplay.getDeviceName());

        t = (TextView) findViewById(R.id.textDeviceDescription);
        t.setText("UPnP Beschreibung: " + deviceDisplay.getDeviceDescription() );

    }

    public void onToggleClicked(View view) {

        Switch mySwitch = (Switch) view; //Referenz vom Switch Objekt, um damit zu interagieren
        boolean switch_is_on = mySwitch.isChecked();

        if (switch_is_on) {
            executeAction(MainActivity.upnpService, switchPower, true);
            showToast("Button is on", false);
        } else {
            executeAction(MainActivity.upnpService, switchPower, false);
            showToast("Button is off", false);
        }
    }

    protected void executeAction(AndroidUpnpService upnpService, Service switchPowerService, boolean value){

        ActionInvocation setTargetInvocation =
                new SetTargetActionInvocation(switchPowerService, value);

        // Executes asynchronous in the background
        upnpService.getControlPoint().execute(
                new ActionCallback(setTargetInvocation) {

                    @Override
                    public void success(ActionInvocation invocation) {
                        assert invocation.getOutput().length == 0;
                        System.out.println("Successfully called action!");
                    }

                    @Override
                    public void failure(ActionInvocation invocation,
                                        UpnpResponse operation,
                                        String defaultMsg) {
                        System.err.println(defaultMsg);
                    }
                }
        );
    }


    protected void showToast(final String msg, final boolean longLength) {

        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(
                        Switches.this,
                        msg,
                        longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                ).show();
            }
        });
    }




}
