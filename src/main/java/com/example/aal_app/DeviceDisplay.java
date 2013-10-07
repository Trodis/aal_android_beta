package com.example.aal_app;

import org.teleal.cling.model.meta.Device;


/**
 * Created with IntelliJ IDEA.
 * User: trodis
 * Date: 20.08.13
 * Time: 14:08
 * To change this template use File | Settings | File Templates.
 */

public class DeviceDisplay {

    private Device device;

    /**************************************************************************************/

    public DeviceDisplay(Device device) {

        this.device = device;

    }

    public Device getDevice() {

        return device;
    }

    public String getDeviceName(){

        return device.getDetails().getFriendlyName();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceDisplay that = (DeviceDisplay) o;
        return device.equals(that.device);
    }

    @Override
    public int hashCode() {

        return device.hashCode();
    }

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