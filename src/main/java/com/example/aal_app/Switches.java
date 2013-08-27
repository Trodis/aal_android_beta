package com.example.aal_app;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
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
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.meta.StateVariable;
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

    private Service switchPower;
    private ServiceId serviceId = new UDAServiceId("SwitchPower");
    private boolean local_switch_state = false;
    private static final String TAG = "TRODIS LOG: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);

        Bundle extras = getIntent().getExtras();
        int position = extras.getInt(MainActivity.EXTRA_MESSAGE);

        DeviceDisplay deviceDisplay = MainActivity.listAdapter.getItem(position);
        Device mydevice = deviceDisplay.getDevice();

        TextView t;
        t = (TextView) findViewById(R.id.upnpSwitch);
        t.setText(deviceDisplay.getServiceTypeOfDevice());

        t = (TextView) findViewById(R.id.textDeviceName);
        t.setText("Gerät: " + deviceDisplay.getDeviceName());

        t = (TextView) findViewById(R.id.textDeviceDescription);
        t.setText("UPnP Beschreibung: " + deviceDisplay.getDeviceDescription() );

        this.switchPower = mydevice.findService(serviceId);

        if ( this.switchPower != null) {
           showToast(deviceDisplay.getServiceTypeOfDevice() + " Service ist vorhanden!", false);
        }

        SubscriptionCallback callback = new SubscriptionCallback(switchPower, 600) {

            @Override
            public void established(GENASubscription sub) {
                showToast("Established: " + sub.getSubscriptionId(), false);
            }

            @Override
            protected void failed(GENASubscription subscription,
                                  UpnpResponse responseStatus,
                                  Exception exception,
                                  String defaultMsg) {
                System.err.println(defaultMsg);
            }

            @Override
            public void ended(GENASubscription sub,
                              CancelReason reason,
                              UpnpResponse response) {
                assert reason == null;
            }

            public void eventReceived(GENASubscription sub) {

                showToast("Event: " + sub.getCurrentSequence().getValue(), false);

                Map<String, StateVariableValue> values = sub.getCurrentValues();
                StateVariableValue status = values.get("Status");

                //assertEquals(status.getDatatype().getClass(), BooleanDatatype.class);
                //assertEquals(status.getDatatype().getBuiltin(), Datatype.Builtin.BOOLEAN);

                if(status.toString().equals("1")){
                    local_switch_state = true;
                }else{
                    local_switch_state = false;
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        Switch s = (Switch) findViewById(R.id.upnpSwitch);
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

    public void onToggleClicked(View view) {
        Switch mySwitch = (Switch) view; //Referenz vom Switch Objekt, um damit zu interagieren
        boolean switch_is_on = mySwitch.isChecked();

        if (switch_is_on) {
            executeAction(MainActivity.upnpService, switchPower, true);
        } else {
            executeAction(MainActivity.upnpService, switchPower, false);
        }
        //MainActivity.upnpService.getControlPoint().execute(getStatusCallback);
        //showToast(String.valueOf(switch_status), true);
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
