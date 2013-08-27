package com.example.aal_app;

import android.os.Parcel;
import android.os.Parcelable;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: trodis
 * Date: 20.08.13
 * Time: 14:08
 * To change this template use File | Settings | File Templates.
 */

public class DeviceDisplay implements Serializable {

    Device device;

    private AndroidUpnpService upnpService;


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

    public String getDeviceDescription(){
        return device.getDetails().getModelDetails().getModelDescription();
    }

    public String getServiceTypeOfDevice(){

        Service[] service = device.getServices();

        return service[0].getServiceType().getType();
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