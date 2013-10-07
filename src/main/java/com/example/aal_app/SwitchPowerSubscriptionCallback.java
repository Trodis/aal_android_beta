package com.example.aal_app;

import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
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
        showToast("Subscription of " + state_variable.getName() + " established!", false);

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
        if (reason == null)
        {
            showToast( "Subscription of " + sub.getService().getStateVariable(state_variable.getName()).getName()
                       + " ended", false );
        }
        else
        {
            showToast( "Subscription canceled! Reason: " + reason.name(), true);
        }
    }

    public void eventReceived(GENASubscription sub)
    {

        Map<String, StateVariableValue> values = sub.getCurrentValues();
        StateVariableValue state_Variable_Value = values.get(state_variable.getName());

        if (state_Variable_Value.getValue() != null)
        {
            if (state_Variable_Value.getDatatype().getBuiltin().equals(Datatype.Builtin.BOOLEAN ))
            {
                boolean received_value = (Boolean)state_Variable_Value.getValue();
                switches.addnewPoint(counter, Boolean.valueOf(received_value).compareTo(false));

                for (Action action : state_variable.getService().getActions())
                {
                    if(action.hasInputArguments())
                    {
                        for( ActionArgument action_argument : action.getInputArguments())
                        {
                            if(action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.BOOLEAN))
                            {
                                switches.setSwitch(received_value, action);
                            }
                        }
                    }
                }
            }
            else if (state_Variable_Value.getDatatype().getBuiltin().equals( Datatype.Builtin.UI1))
            {
                switches.addnewPoint(counter, Integer.parseInt(state_Variable_Value.getValue().toString()));

                for (Action action : state_variable.getService().getActions())
                {
                    if (action.hasInputArguments())
                    {
                        for( ActionArgument action_argument : action.getInputArguments())
                        {
                            if(action_argument.getDatatype().getBuiltin().equals(Datatype.Builtin.UI1))
                            {
                                switches.setSeekBar(state_Variable_Value.getValue().toString(), action);
                            }
                        }
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
