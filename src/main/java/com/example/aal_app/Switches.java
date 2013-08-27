package com.example.aal_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ServiceId serviceId = new UDAServiceId("SwitchPower");


        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);


        Intent intent = getIntent();
        DeviceDisplay device = (DeviceDisplay) intent.getSerializableExtra("Device Object");


        /*
        TextView t;
        t = (TextView) findViewById(R.id.upnpSwitch);
        t.setText(device.getDeviceDescription());

        t = (TextView) findViewById(R.id.textDeviceName);
        t.setText("Gerät: " + device.getDeviceName());

        t = (TextView) findViewById(R.id.textDeviceDescription);
        t.setText("UPnP Beschreibung: " + device.getDeviceDescription() );
          */

    }

    public void onToggleClicked(View view) {

        Switch mySwitch = (Switch) view; //Referenz vom Switch Objekt, um damit zu interagieren
        boolean switch_is_on = mySwitch.isChecked();


        if (switch_is_on) {
            showToast("Button is on", false);
        } else {
            showToast("Button is off", false);
        }
    }

    protected void executeAction(AndroidUpnpService upnpService, Service switchPowerService){

        ActionInvocation setTargetInvocation =
                new SetTargetActionInvocation(switchPowerService);

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
