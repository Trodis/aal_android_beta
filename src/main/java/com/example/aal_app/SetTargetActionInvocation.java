package com.example.aal_app;

import org.teleal.cling.model.action.ActionInvocation;
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

    SetTargetActionInvocation(Service service, boolean switch_status) {
        super(service.getAction("SetTarget"));
        try {

            // Throws InvalidValueException if the value is of wrong type
            setInput("NewTargetValue", switch_status);

        } catch (InvalidValueException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }
}
