package com.example.aal_app;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
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
    public final static String EXTRA_MESSAGE = "UPNP Device";
    private static final String TAG = "TRODIS LOG: ";

    private Device upnp_device;
    private ArrayList input_value;
    private ArrayAdapter<DeviceDisplay> listAdapter;

    private AndroidUpnpService upnpService;
    private SubscriptionCallback callback;
    private String unique_device_identifier;
    private Bundle savedInstanceState;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {

            upnpService = (AndroidUpnpService) service;
            onResume();
        }

        public void onServiceDisconnected(ComponentName className) {

            upnpService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.switches);

        Bundle extras = getIntent().getExtras();


        this.unique_device_identifier = extras.getString(EXTRA_MESSAGE);
        this.savedInstanceState = savedInstanceState;
        input_value = new ArrayList();

    }

    @Override
    protected void onResume(){
        super.onResume();
        if (upnpService != null && savedInstanceState == null)
            upnp_device = upnpService.getRegistry().getDevice(UDN.valueOf(unique_device_identifier), true);

        if (this.upnp_device != null){
            createUPnPServiceandActionInformations(upnp_device);
            for (Service service : upnp_device.getServices()) {
                for(Action action : service.getActions()){
                    generateUI(action);
                }
            }
        } else {
            this.savedInstanceState = null;
            onCreate(savedInstanceState);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override protected void onDestroy(){
        super.onDestroy();
        if (upnpService != null){

            getApplicationContext().unbindService(serviceConnection);
            this.listAdapter    = null;
            this.upnp_device = null;
            this.upnpService = null;
            this.input_value = null;
        }

        if (callback != null){
            callback.end();
        }

    }


    @Override
    protected void onPause(){
        super.onPause();
        onDestroy();
    }


    protected void executeAction(AndroidUpnpService upnpService, Service service, Action action, ActionArgument action_argument, ArrayList input_value, boolean isInput){

        ActionInvocation setTargetInvocation =
                new SetTargetActionInvocation(service, action, action_argument, input_value, isInput);


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

    public void createInputActions( final Action action, final ActionArgument action_argument) {
        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPActionElements);

        Switch button = new Switch(this);
        button.setText(action.getName());
        button.setTag(action.getName());
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    input_value.add(true);
                    executeAction(upnpService, action.getService(), action, action_argument, input_value, true);
                    input_value.clear();
                } else {
                    input_value.add(false);
                    executeAction(upnpService, action.getService(), action, action_argument, input_value, true);
                    input_value.clear();
                }
            }
        });
        ll.addView(button);
    }

    public void createOutPutActions( final Action action, final ActionArgument action_argument){

        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPOutputActionElements);

        Button button = new Button(this);
        button.setText(action.getName());
        button.setTag(action.getName());
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeAction(upnpService, action.getService(), action, action_argument, input_value, false);
            }
        });

        ll.addView(button);

    }

    public void createSeekBarActions(final  Action action, final ActionArgument action_argument){
        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPSeekBar);

        SeekBar sb = new SeekBar(this);
        sb.setTag(action.getName());
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //To change body of implemented methods use File | Settings | File Templates.
                input_value.add(String.valueOf(progress));
                executeAction(upnpService, action.getService(), action, action_argument, input_value, true);
                input_value.clear();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        ll.addView(sb);
    }


    public void setSwitch(final boolean is_checked,final Action action){

        runOnUiThread(new Runnable() {
            public void run() {

                final View ll = findViewById(R.id.LinearLayoutUPnPActionElements);
                Switch mySwitch;
                mySwitch = (Switch) ll.findViewWithTag(action.getName());
                mySwitch.setChecked(is_checked);
            }
        });
    }



    private void startEventlistening(Action action, StateVariable state_variable){
            this.callback = new SwitchPowerSubscriptionCallback(action, state_variable, this);
            upnpService.getControlPoint().execute(callback);
    }



    public void createSwitchPowerSubscription(Action action){
        if(action.getService().hasStateVariables()){
            for (StateVariable state_variable : action.getService().getStateVariables()){
                if(state_variable.getEventDetails().isSendEvents()){
                    startEventlistening(action, state_variable);
                    Log.v(TAG, "STATEVARIABLE: " + state_variable);
                }
            }
        }
    }

    private void generateUI(Action action){

            for (ActionArgument action_argument : action.getInputArguments()){

                if(action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.BOOLEAN)) {

                    createInputActions(action, action_argument);
                    createSwitchPowerSubscription(action);

                } else if (action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.UI1)) {

                    createSeekBarActions(action, action_argument);
                    createSwitchPowerSubscription(action);

                }
            }


            for (ActionArgument action_argument : action.getOutputArguments()){

                createOutPutActions(action, action_argument);
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

    public void createUPnPServiceandActionInformations(Device upnp_device){

        LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPDeviceInformations);
        TextView tv;

        for (Service services : upnp_device.getServices()){

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
    }

    public void createUPnPGeneralInformations(Service service){

        LinearLayout ll;
        ll = (LinearLayout) findViewById(R.id.LinearLayoutUPnPDeviceInformations);

        TextView tv;

        tv = new TextView(this);
        tv.setText("Device Name: " + service.getDevice().getDetails().getFriendlyName());
        tv.setTextSize(15);

        tv = new TextView(this);
        tv.setText("Device Discription: " +service.getDevice().getDetails().getManufacturerDetails());
        tv.setTextSize(15);

        ll.addView(tv);

    }

}