package com.example.aal_app;

import android.util.Log;
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.InvalidValueException;
import org.teleal.cling.model.types.UnsignedIntegerOneByteDatatype;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: trodis
 * Date: 26.08.13
 * Time: 15:39
 * To change this template use File | Settings | File Templates.
 */


class SetTargetActionInvocation extends ActionInvocation {

    SetTargetActionInvocation(Service service, Action action, ActionArgument action_argument, ArrayList input_value, boolean isInput) {

        super(action);

        if (isInput){

            try {
                setInput(action_argument.getName(), input_value.get(0) );
            } catch (InvalidValueException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        } else {
            try{
                //setOutput(action_argument.getName(), null);
                ActionArgumentValue value = getOutput(action_argument.getName());
                if (value != null){
                    Log.v("LOG LOG::::", value.getValue().toString());
                }

            } catch (InvalidValueException ex){
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        }
    }
}