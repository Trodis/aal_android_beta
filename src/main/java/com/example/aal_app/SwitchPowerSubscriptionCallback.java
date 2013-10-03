package com.example.aal_app;

import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.Datatype;

import java.util.*;

public class SwitchPowerSubscriptionCallback extends SubscriptionCallback{

    Switches switches;
    StateVariable state_variable;
    int counter;
    public SwitchPowerSubscriptionCallback(StateVariable state_variable, Switches switches, int counter)
    {
        super(state_variable.getService());
        this.switches       = switches;
        this.state_variable = state_variable;
        this.counter = counter;
    }

    @Override
    public void established(GENASubscription sub)
    {
        showToast("Subscription with Service established! Listening for Events, renewing in seconds: "
                                                              + sub.getActualDurationSeconds(), false);

    }

    @Override
    protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
                                                                                      String defaultMsg)
    {
        showToast(defaultMsg, true);
    }

    @Override
    public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response)
    {
        showToast( "Subscription of Service " + sub.getService().getServiceType().getType(), false );
    }

    public void eventReceived(GENASubscription sub)
    {
        Map<String, StateVariableValue> values = sub.getCurrentValues();
        StateVariableValue state_Variable_Value = values.get(state_variable.getName());

        if (state_Variable_Value.getValue() != null)
        {
            if (state_Variable_Value.getDatatype().getBuiltin().equals( Datatype.Builtin.BOOLEAN ))
            {
                boolean received_value = (Boolean)state_Variable_Value.getValue();
                switches.addnewPoint(counter, Boolean.valueOf(received_value).compareTo(false));

                for (Action action : state_variable.getService().getActions())
                {
                    if (action.hasInputArguments())
                    {
                        switches.setSwitch(received_value, action);
                    }
                }
            }
            else if ( state_Variable_Value.getDatatype().getBuiltin().equals( Datatype.Builtin.UI1))
            {
                switches.addnewPoint(counter, Integer.parseInt(state_Variable_Value.getValue().toString()));

                for (Action action : state_variable.getService().getActions())
                {
                    if (action.hasInputArguments())
                    {
                        switches.setSeekBar(state_Variable_Value.getValue().toString(), action);
                    }
                }
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
