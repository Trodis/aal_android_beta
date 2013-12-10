package com.example.aal_app;

import org.teleal.cling.model.meta.Device;


/**
 * Die Klasse DeviceDisplay hat die nur die Aufgabe eine formatierte Ausgabe der UPnP Ger&auml;te zur&uuml;ckzugeben. Es ist
 * eine Simple Java Beans Klasse. Die Methode toString() ist die entscheidende Methode um die Liste der UPnP Ger&auml;te
 * zu rendern. Mann kann mit dieser Klasse, jegliche Informationen des UPnP Ger&auml;tes anzeigen. Es m&uuml;ssen nur die
 * gew&uuml;nschten Methode angelegt werden.
 *
 *
 * @author Ferhat &Ouml;zmen
 * @version 0.1
 */

public class DeviceDisplay {

    private Device device;

    /**************************************************************************************/

    /**
     * Konstruktur um die Referenz, f&uuml;r das jeweilige UPnP Ger&auml;t.
     * @param device Das Ger&auml;te das ausgew&auml;rtet wurde mit seinen service meta Daten.
     */
    public DeviceDisplay(Device device) {

        this.device = device;

    }

    /**
     * Eine einfache Methode, um den UPnP Ger&auml;te Namen anzufordern.
     * @return liefert den lesbaren des UPnP Ger&auml;tes.
     */
    public Device getDevice() {

        return device;
    }

    public String getDeviceName(){

        return device.getDetails().getFriendlyName();
    }

    /**
     * Die equals Methode wird &uuml;berschrieben, um die vorhandenen Ger&auml;te in der Liste hinzuzuf&uuml;gen oder zu entfernen.
     * @param o Referenz von Typ der Klasse Object.
     * @return Falls das Ger&auml;t bereits in der Liste existiert wird ein true zur&uuml;ckgeliefert.
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceDisplay that = (DeviceDisplay) o;
        return device.equals(that.device);
    }

    /**
     * Der Haschode vom UPnP Ger&auml;t wird zur&uuml;ckgeliefert.
     *
     * @return Der Hashcode des Ger&auml;te wird zur&uuml;ckgeliefert.
     */
    @Override
    public int hashCode() {

        return device.hashCode();
    }

    /**
     * Diese Methode ist eine Darstellungsm&ouml;glichkeit, w&auml;hrend das UPnP Ger&auml;t noch ausgewertet wird. Der volle
     * Name wird also nicht angezeigt wenn das Ger&auml;t noch ausgewertet wird, sondern mit einem kleinen Stern.
     *
     * @return liefert den Namen des UPnP Ger&auml;tes mit einem Stern zur&uuml;ck.
     */
    @Override
    public String toString() {

        String name =
                device.getDetails() != null && device.getDetails().getFriendlyName() != null
                        ? device.getDetails().getFriendlyName()
                        : device.getDisplayString();
        // Display a little star while the device is being loaded (see performance optimization earlier)
        return device.isFullyHydrated() ? name : name + " *";
    }

}