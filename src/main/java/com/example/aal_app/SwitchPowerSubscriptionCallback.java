package com.example.aal_app;

import android.app.Activity;
import android.renderscript.Element;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.Datatype;

import java.util.Map;

public class SwitchPowerSubscriptionCallback extends SubscriptionCallback {

    Service service;
    Switches switches;
    ActionArgument action_argument;
    Action action;
    StateVariable state_variable;
    int buttonID;

    public SwitchPowerSubscriptionCallback(Action action, StateVariable state_variable, Switches switches, int buttonID){

        super(action.getService(), 600);
        this.action         = action;
        this.switches       = switches;
        this.state_variable = state_variable;
        this.buttonID       = buttonID;
    }

    @Override
    public void established(GENASubscription sub) {
        showToast("Subscription with Service established! Listening for Events, renewing in seconds: "
                + sub.getActualDurationSeconds(), true);
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

        Map<String, StateVariableValue> values = sub.getCurrentValues();
        //StateVariableValue state_Variable_Value = values.get(action_argument.getRelatedStateVariableName());
        StateVariableValue state_Variable_Value = values.get(state_variable.getName());
        Log.v("", "DEBUG DEBUG:::::::"+state_variable.getName());

        if (state_Variable_Value.getValue() != null){
            Log.v("", "DEBUG DEBUG:::::::"+state_Variable_Value.getValue());
            if (state_Variable_Value.getDatatype().getBuiltin().equals(Datatype.Builtin.BOOLEAN)){

                if((Boolean) state_Variable_Value.getValue()){
                    switches.setSwitch(true, buttonID);
                } else {
                    switches.setSwitch(false, buttonID);
                }


            } else if (state_Variable_Value.getDatatype().getBuiltin().equals(Datatype.Builtin.UI1)){
                Log.v("STRING STATEVARIABLE:::", ":::::"+state_Variable_Value.getValue() );


            }

        }
    }

    public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
        showToast("Missed events: " + numberOfMissedEvents, false);
    }


    protected void showToast(final String msg, final boolean longLength) {
        switches.showToast(msg, longLength);
    }

}
