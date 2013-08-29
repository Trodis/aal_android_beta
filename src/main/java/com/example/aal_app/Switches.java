package com.example.aal_app;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Map;


/**
 * @author Ferhat Özmen
 */
public class Switches extends Activity{

    private Service service_switch_power;


    private boolean local_switch_state = false;
    private String gena_discription;
    private String gena_service_received_state_status;

    private static final String TAG = "TRODIS LOG: ";

    private DeviceDisplay device_display;
    private Device upnp_device;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);

        Bundle extras = getIntent().getExtras();
        int item_position = extras.getInt(MainActivity.EXTRA_MESSAGE);

        this.device_display = MainActivity.listAdapter.getItem(item_position);
        this.upnp_device = device_display.getDevice();

        if (device_display != null){
            for (Service services : upnp_device.getServices()) {

                if(services.getServiceType().getType().equals("SwitchPower")){
                    this.service_switch_power = upnp_device.findService(services.getServiceId());
                } else {

                    LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPActionElements);

                    TextView tv = new TextView(this);
                    tv.setText("No Service SwitchPower found, can not create Switches!");
                    tv.setTextColor(Color.rgb(200, 0, 0));
                    tv.setTextSize(22);

                    ll.addView(tv);
                    break;
                }
            }
            createUPnPServiceandActionInformations(device_display);
        }

        if ( this.service_switch_power != null) {

            showToast(service_switch_power.getServiceType().getType() + " Service ist vorhanden!", false);
            createSwitches();
        } else {

            showToast("Warnung! Service / ServiceID wurde nicht gefunden!", true);

            LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPActionElements);

            TextView tv = new TextView(this);
            tv.setText("Keine Switches weil: new UDAServiceID -> " + this.service_switch_power);
            tv.setTextColor(Color.rgb(200, 0, 0));

            ll.addView(tv);
        }


        SubscriptionCallback callback = new SubscriptionCallback(service_switch_power, 600) {

            @Override
            public void established(GENASubscription sub) {
                showToast("Established: " + sub.getSubscriptionId(), false);
            }

            @Override
            protected void failed(GENASubscription subscription,
                                  UpnpResponse responseStatus,
                                  Exception exception,
                                  String defaultMsg) {
                showToast(defaultMsg, true);
            }

            @Override
            public void ended(GENASubscription sub,
                              CancelReason reason,
                              UpnpResponse response) {
                assert reason == null;
            }

            public void eventReceived(GENASubscription sub) {

                gena_discription = sub.getCurrentSequence().getValue().toString();

                Map<String, StateVariableValue> values = sub.getCurrentValues();
                StateVariableValue status = values.get("status");

                gena_service_received_state_status = status.toString();

                if(status.toString().equals("1")){
                    local_switch_state = true;
                    gena_service_received_state_status = "UPnP Device ist derzeit eingeschaltet";
                }else{
                    local_switch_state = false;
                    gena_service_received_state_status = "UPnP Device ist derzeit ausgeschaltet";
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        Switch s = (Switch) findViewById(1);
                        s.setChecked(local_switch_state);
                    }
                });
            }

            public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
                showToast("Missed events: " + numberOfMissedEvents, false);
            }

        };

        MainActivity.upnpService.getControlPoint().execute(callback);
    }

    public void createSwitches()
    {
            LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPActionElements);

            Switch s = new Switch(this);
            s.setText(device_display.getServiceTypeOfDevice());
            s.setId(1);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (isChecked) {
                        executeAction(MainActivity.upnpService, service_switch_power, true);
                    } else {
                        executeAction(MainActivity.upnpService, service_switch_power, false);
                    }
                }
            });
            ll.addView(s);
    }

    public void createUPnPServiceandActionInformations(DeviceDisplay device_display){

        LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPDeviceInformations);
        TextView tv;

        for (Service services : device_display.getServices()){

            tv = new TextView(this);
            tv.setText("Service " + services.getServiceType().getType() + ":");
            tv.setHighlightColor(1);
            tv.setTextColor(Color.rgb(50, 205, 50));
            tv.setTextSize(17);
            ll.addView(tv);

            for(Action actions : services.getActions()){
                tv = new TextView(this);
                tv.setText(actions.getName());
                tv.setTextSize(15);
                ll.addView(tv);
            }
        }

        createUPnPGeneralInformations(device_display);
    }

    public void createUPnPGeneralInformations(DeviceDisplay deviceDisplay){

        LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPDeviceGeneralInformations);

        TextView tv;

        tv = new TextView(this);
        tv.setText("Device Name: " +deviceDisplay.getDeviceName());
        tv.setTextSize(15);

        tv = new TextView(this);
        tv.setText("Device Discription: " +deviceDisplay.getDeviceDescription());
        tv.setTextSize(15);

        ll.addView(tv);

    }

    protected void executeAction(AndroidUpnpService upnpService, Service switchPowerService, boolean switch_status){

        ActionInvocation setTargetInvocation =
                new SetTargetActionInvocation(switchPowerService, switch_status);

        // Executes asynchronous in the background
        upnpService.getControlPoint().execute(
                new ActionCallback(setTargetInvocation) {

                    @Override
                    public void success(ActionInvocation invocation) {
                        assert invocation.getOutput().length == 0;
                        showToast("Successfully called action!", false);
                    }

                    @Override
                    public void failure(ActionInvocation invocation,
                                        UpnpResponse operation,
                                        String defaultMsg) {
                        showToast(defaultMsg, true);
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