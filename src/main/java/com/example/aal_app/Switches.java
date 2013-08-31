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

    //private Service service_from_upnp_device;


    private boolean local_switch_state = false;
    private String gena_discription;
    private String gena_service_received_state_status;

    private static final String TAG = "TRODIS LOG: ";

    private DeviceDisplay device_display;
    private Device upnp_device;
    private Service current_upnp_device_service;
    private String action_of_upnp_device;
    private Action[] actions;
    private String argument_of_action_from_upnp_device;
    private String related_state_variable_input_action;
    private String related_state_variable_output_action;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);

        Bundle extras = getIntent().getExtras();
        int item_position = extras.getInt(MainActivity.EXTRA_MESSAGE);

        this.device_display = MainActivity.listAdapter.getItem(item_position);
        this.upnp_device = device_display.getDevice();

        if (device_display != null){
            for (Service service : upnp_device.getServices()) {
                for(Action action : service.getActions()){
                    generateUI(action);
                }
            createUPnPServiceandActionInformations(device_display);
          }
       }
    }


    public void createInputActions( final Action action, final ActionArgument action_argument)
    {
        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPActionElements);

        Switch s = new Switch(this);
        s.setText(action.getName());
        s.setId(action.hashCode());
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    executeAction(MainActivity.upnpService, action.getService(), action, action_argument, true);
                } else {
                    executeAction(MainActivity.upnpService, action.getService(), action, action_argument, false);
                }
            }
        });
        ll.addView(s);

        if(action.getService().hasStateVariables()){
            for (StateVariable state_variable : action.getService().getStateVariables()){
                if(state_variable.getEventDetails().isSendEvents()){
                    startEventlistening(action, state_variable, action.hashCode());
                    Log.v(TAG, "STATEVARIABLE: " + state_variable);
                }
            }
        }
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
        ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPDeviceInformations);

        TextView tv;

        tv = new TextView(this);
        tv.setText("Device Name: " +deviceDisplay.getDeviceName());
        tv.setTextSize(15);

        tv = new TextView(this);
        tv.setText("Device Discription: " +deviceDisplay.getDeviceDescription());
        tv.setTextSize(15);

        ll.addView(tv);

    }

    protected void executeAction(AndroidUpnpService upnpService, Service service, Action action, ActionArgument action_argument, boolean switch_status){

        ActionInvocation setTargetInvocation =
                new SetTargetActionInvocation(service, action, action_argument, switch_status);

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


    public void setSwitch(final boolean is_checked, final int switchId){

        runOnUiThread(new Runnable() {
            public void run() {
                Switch s = (Switch) findViewById(switchId);
                s.setChecked(is_checked);
            }
        });
    }

    private void startEventlistening(Action action, StateVariable state_variable, int buttonID ){
        if (action != null) {
            SubscriptionCallback callback = new SwitchPowerSubscriptionCallback(action, state_variable, this, buttonID);
            MainActivity.upnpService.getControlPoint().execute(callback);
        }
    }

    private void generateUI(Action action){
        if (action.hasInputArguments()){
            for (ActionArgument action_argument : action.getInputArguments()){
                if(action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.BOOLEAN)){
                    createInputActions(action, action_argument);
                }
            }
        }
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