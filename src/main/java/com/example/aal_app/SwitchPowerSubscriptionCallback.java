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

/**
 * Die SwitchPowerSubscriptionCallback Klasse, dient nur f&uuml;r das Verwalten und Behandeln der GENA Events. Sobald ein
 * neuer GENA Event eingeht, wird er ausgewertet und der entsprechende Betroffene Button wird ermittelt. Anschlie&szlig;end
 * wird das betroffene Button oder das SeekBar mit dem neuen Wert aktualisiert. Dazu geh&ouml;rt auch das Diagramm. *
 *
 * @author Ferhat &Ouml;zmen
 * @version 0.1
 */

public class SwitchPowerSubscriptionCallback extends SubscriptionCallback{

    Switches switches;
    StateVariable state_variable;
    int counter;

    /**
     * Der Konstruktor erwartet die Statevariable, eine Referenz der Klasse Switches wo sich die GUI Elemente
     * befinden und mit welchem Wert der counter initialisiert werden soll. Das heisst wenn eine GENA Anmeldung
     * erfolgt dann wird mit diesem Counter sichergestellt, wo das Diagramm zuletzt aufgezeichnet hat bzw. bei
     * welchem GENA Zyklus die Verbindung unterbrochen wurde.
     *
     * @param state_variable Statevariable des UPnP Ger&auml;tes, das relevant ist.
     * @param switches Instanz der Klasse Switches, um auf die &ouml;ffentlichen Methode zugreifen zu k&ouml;nnen.
     * @param counter der Counter mit dem
     */
    public SwitchPowerSubscriptionCallback(StateVariable state_variable, Switches switches, int counter)
    {
        super(state_variable.getService());
        this.switches       = switches;
        this.state_variable = state_variable;
        this.counter = counter;
    }

    /**
     * In dieser Methode wird die Statevariable als GENA angemeldet.
     *
     * @param sub die Instanz für die erfolgreiche Anmeldung.
     */
    @Override
    public void established(GENASubscription sub)
    {
        showToast("Subscription of " + state_variable.getName() + " established!", false);
        switches.setStateVariableStatus(false);
        switches.updateGENAStatusTextView();
    }

    /**
     * Diese Methode
     * @param subscription die Instanz f&uuml;r eine fehlgeschlagene Anmeldung
     * @param responseStatus f&uuml;r eine entfernte Anmeldung, wenn &uuml;ber eine Antwort erhalten wurde,
     *                       ansonsten ist diese Instanz null.
     * @param exception f&uuml;r eine lokale Anmeldung, ansonsten ist die Instanz null.
     * @param defaultMsg eine Benutzerfreundliche Meldung f&uuml;r den Nutzer.
     */
    @Override
    protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
                                                                                      String defaultMsg)
    {
        showToast(defaultMsg, true);
    }

    /**
     * Wird aufgerufen wenn die GENA Anmeldung beendet wurde. Entweder gew&uuml;nscht oder aufgrund eines Fehlers.
     *
     * @param sub die beendete Instanz der GENA Anmeldung.
     * @param reason nur wenn die Anmeldung sauber beendet wurde, ansonsnten ist diese Instanz null.
     * @param response wenn die Ursache vom Ger&auml;t im Netzwerk verursacht wurde.
     */
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
            switches.setStateVariableStatus(true);
            switches.updateGENAStatusTextView();
        }
    }

    /**
     * Diese Methode behandelt den eingegangen GENA Event. Wenn die Statevariabel alle Kriterien erf&uuml;llt vom UPnP
     * Ger&auml;t, wird Sie weiterbehandelt. Um die eingegangen Event einem GUI Element zuordnen zu k&ouml;nnen,
     * wird hier alles nochmal gepr&uuml;ft und anschlie&szlig;end der Wert an die jeweilige Methode der Klasse Switches
     * &uuml;bergeben. Wo dann entweder die SeekBar oder der Button aktualisiert wird. Zudem wird das Diagramm
     * aktualisiert und der counter hochgez&auml;hlt um den Zyklus festzuhalten.
     *
     * @param sub Instanz f&uuml;r das neue GENA Event.
     */
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


    /**
     * Wenn aufgrund von Netzwerk Verz&ouml;gerungen, ein GENA Event verpasst wurde, wird der Nutzer dar&uuml;ber informiert
     * und die Anzahl der verpassten Events wird angezeigt.
     *
     * @param sub die Instanz f&uuml;r das verpasste GENA Event..
     * @param numberOfMissedEvents die Anzahl der verpassten GENA Events.
     */
    public void eventsMissed(GENASubscription sub, int numberOfMissedEvents)
    {
        showToast("Missed events: " + numberOfMissedEvents, false);
    }


    /**
     * Dient f&uuml;r die Anzeige von Informationen oder Nachrichten, um den Nutzer auf etwas aufmerksam zu machen.
     *
     * @param msg die Nachricht die angezeigt werden soll
     * @param longLength ob Sie kurz oder lang angezeigt werden soll.
     */
    protected void showToast(final String msg, final boolean longLength)
    {
        switches.showToast(msg, longLength);

    }
}
