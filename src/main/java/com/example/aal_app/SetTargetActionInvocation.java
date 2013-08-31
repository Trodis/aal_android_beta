package com.example.aal_app;

import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.InvalidValueException;
/**
 * Created with IntelliJ IDEA.
 * User: trodis
 * Date: 26.08.13
 * Time: 15:39
 * To change this template use File | Settings | File Templates.
 */


class SetTargetActionInvocation extends ActionInvocation {

    SetTargetActionInvocation(Service service, Action action, ActionArgument action_argument, boolean switch_status) {
        super(action);
        try {

            // Throws InvalidValueException if the value is of wrong type
            setInput(action_argument.getName(), switch_status);


        } catch (InvalidValueException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }
}