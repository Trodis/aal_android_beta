package com.example.aal_app;

import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.InvalidValueException;

import java.util.ArrayList;

/**
 * Diese Klasse dient dazu, den Befehl bzw. Wert an das gewünschte UPnP Gerät zu übergeben / senden. Dabei wird
 * geprüft ob es sich um einen Input oder Output Argument handelt.
 *
 * @author Ferhat Özmen
 * @version 0.1
 */


class SetTargetActionInvocation extends ActionInvocation {

    /**
     *
     * @param action die jeweilige Action des UPnP Gerätes.
     * @param action_argument das Action Argument des jeweiligen UPnP Gerätes.
     * @param input_value der Eingabewert für die Action.
     * @param isInput prüfen ob es sich um eine Input Wert handelt.
     */
    SetTargetActionInvocation(Action action, ActionArgument action_argument, ArrayList input_value, boolean isInput)
    {

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
                setOutput(action_argument.getName(), false);

            } catch (InvalidValueException ex){
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        }
    }
}