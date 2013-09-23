package com.example.aal_app;


import android.text.format.Time;
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.*;

public class SwitchPowerSubscriptionCallback extends SubscriptionCallback implements Runnable {

    Switches switches;
    Action action;
    StateVariable state_variable;
    int counter = 0;
    private long time;
    private int turnedOn;
    private DynamicSeries data;
    public SwitchPowerSubscriptionCallback(Action action,
                                           StateVariable state_variable,
                                           Switches switches)
    {
        super(action.getService(), 600);
        this.action         = action;
        this.switches       = switches;
        this.state_variable = state_variable;
        this.data = data;
    }

    @Override
    public void established(GENASubscription sub)
    {
        showToast("Subscription with Service established! Listening for " +
                  "Events, renewing in seconds: "
                + sub.getActualDurationSeconds(), true);

    }

    @Override
    protected void failed(GENASubscription subscription,
                          UpnpResponse responseStatus,
                          Exception exception,
                          String defaultMsg)
    {
        showToast(defaultMsg, true);
    }

    @Override
    public void ended(GENASubscription sub,
                      CancelReason reason,
                      UpnpResponse response)
    {
        showToast( "Subscription of " + action.getName() + " ended", false );
    }

    public void eventReceived(GENASubscription sub) {
        Map<String, StateVariableValue> values = sub.getCurrentValues();
        StateVariableValue state_Variable_Value = values
                .get( state_variable.getName() );

        if (state_Variable_Value.getValue() != null)
        {
            if (state_Variable_Value.getDatatype().getBuiltin().equals
                    (Datatype.Builtin.BOOLEAN ) && action.hasInputArguments())
            {
                if((Boolean) state_Variable_Value.getValue())

                {
                    switches.setSwitch(true, action);
                    switches.addnewPoint(counter, 1);

                }
                else
                {
                    switches.setSwitch(false, action);
                    switches.addnewPoint(counter, 0);
                }

            }
            else if (state_Variable_Value.getDatatype().getBuiltin().equals
                    (Datatype.Builtin.UI1) && action.hasInputArguments())
            {
                switches.setSeekBar(state_Variable_Value.getValue().toString(),
                                    action);
            }
            else if (state_Variable_Value.getDatatype().getBuiltin().equals(
                    Datatype.Builtin.BOOLEAN ))
            {
                switches.addnewPoint(counter, Boolean
                        .valueOf((Boolean)state_Variable_Value
                                .getValue()).compareTo( false ));
            }
            counter++;
        }
    }


    public void eventsMissed(GENASubscription sub, int numberOfMissedEvents)
    {
        showToast("Missed events: " + numberOfMissedEvents, false);
    }


    protected void showToast(final String msg, final boolean longLength)
    {
        switches.showToast(msg, longLength);
    }
}
