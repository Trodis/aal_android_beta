package com.example.aal_app;

import org.teleal.cling.model.meta.Device;


/**
 * Die Klasse DeviceDisplay hat die nur die Aufgabe eine formatierte Ausgabe der UPnP Geräte zurückzugeben. Es ist
 * eine Simple Java Beans Klasse. Die Methode toString() ist die entscheidende Methode um die Liste der UPnP Geräte
 * zu rendern. Mann kann mit dieser Klasse, jegliche Informationen des UPnP Gerätes anzeigen. Es müssen nur die
 * gewünschten Methode angelegt werden.
 *
 *
 * @author Ferhat Özmen
 * @version 0.1
 */

public class DeviceDisplay {

    private Device device;

    /**************************************************************************************/

    /**
     * Konstruktur um die Referenz, für das jeweilige UPnP Gerät.
     * @param device Das Ger&auml;te das ausgew&auml;rtet wurde mit seinen service meta Daten.
     */
    public DeviceDisplay(Device device) {

        this.device = device;

    }

    /**
     * Eine einfache Methode, um den UPnP Geräte Namen anzufordern.
     * @return liefert den lesbaren des UPnP Gerätes.
     */
    public Device getDevice() {

        return device;
    }

    public String getDeviceName(){

        return device.getDetails().getFriendlyName();
    }

    /**
     * Die equals Methode wird überschrieben, um die vorhandenen Geräte in der Liste hinzuzufügen oder zu entfernen.
     * @param o Referenz von Typ der Klasse Object.
     * @overrides equals in class Object
     * @return Falls das Gerät bereits in der Liste existiert wird ein true zurückgeliefert.
     */
    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceDisplay that = (DeviceDisplay) o;
        return device.equals(that.device);
    }

    /**
     * Der Haschode vom UPnP Gerät wird zurückgeliefert.
     *
     * @override die hashCode() Methode der Klasse Object.
     * @return Der Hashcode des Geräte wird zurückgeliefert.
     */
    @Override
    public int hashCode() {

        return device.hashCode();
    }

    /**
     * Diese Methode ist eine Darstellungsmöglichkeit, während das UPnP Gerät noch ausgewertet wird. Der volle
     * Name wird also nicht angezeigt wenn das Gerät noch ausgewertet wird, sondern mit einem kleinen Stern.
     *
     * @return liefert den Namen des UPnP Gerätes mit einem Stern zurück.
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