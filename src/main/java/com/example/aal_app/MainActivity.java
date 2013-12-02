package com.example.aal_app;

/**
 * Copyrigt (c) 2013, Hochschule Bochum
 * Diese Software wurde im Rahmen eines Software Projektes für die Hochschule Bochum entwickelt.
 * Dieser Code darf nicht ohne Einverständniss von Dr. Prof. Weidauer weiterentwickelt oder zu eigenen Zwecken
 * genutzt werden. Bitte kontaktieren Sie Herr Weidauer, für weitere Fragen.
 */


/**
 * Alle nötigen Bibliotheken werden eingebunden für das Android Framework und Cling Framework
 */

import android.os.Bundle;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.model.meta.*;
import org.teleal.cling.protocol.async.SendingNotificationAlive;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import java.util.Comparator;

/**
 * Diese Klasse ist die Hauptklasse, wo alle UPnP Geräte gesucht und aufgelistet werden. Der Nutzer soll nach der
 * Auflistung die Möglichkeit haben, eines der UPnP Geräte auszuwählen und es anschließend zu bedienen.
 *
 * Dieser Code wurde wie bereits oben erwähnt im Rahmen eines Software Projektes entwickelt. Erfahrene Entwickler,
 * werden viele verbesserungs Vorschläge und Kritik haben. Auch wurde vermutlich nicht jedes Problem,
 * auf die Weise gelöst, die ein erfahrener Entwickler gelöst hätte. Diesbezüglich bitte ich um Nachsicht und die
 * entsprechend herangehensweise beim Lesen des Codes.
 *
 * @author Ferhat Özmen
 * @version 0.1
 */

public class MainActivity extends ListActivity {

    public final static String EXTRA_MESSAGE = "UPNP Device";
    private ArrayAdapter<DeviceDisplay> listAdapter;
    private ListView list;
    private BrowseRegistryListener registryListener = new BrowseRegistryListener();

    private AndroidUpnpService upnpService;


    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        /**
         * Ein Service muss im Hintergrund laufen während die App läuft um neue UPnP Geräte automatisch zu finden
         * und wieder von der Liste zu entfernen sofern das UPnP Gerät vom Netzwerk getrennt wurde.
         *
         * @param className Für welche Klasse der Service gestartet werden soll
         * @param service Der eigentliche Service, der gebunden und im Hintergrund
         *                laufen soll
         */
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            upnpService = (AndroidUpnpService) service;

            // Refresh the list with all known devices
            listAdapter.clear();
            for (Device device : upnpService.getRegistry().getDevices())
            {
                registryListener.deviceAdded(device);
            }

            // Getting ready for future device advertisements
            upnpService.getRegistry().addListener(registryListener);

            // Search asynchronously for all devices
            upnpService.getControlPoint().search();
        }

        /**
         * Den UPnP Service im Hintergrund wieder deaktivieren, wenn die App beendet wird.
         * @param className FÜr welche Klasse der Service beendet werden.
         */
        public void onServiceDisconnected(ComponentName className)
        {
            upnpService = null;
        }
    };

    /**
     * onCreate Methode vom Android Framework (siehe Android Lifecycle auf google)
     * @param savedInstanceState Ein mapping von String Werten
     *                           und Elementen die vom Typ Parcelable sind.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        listAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        setListAdapter(listAdapter);

        list = getListView();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            /**
             * Diese Methode onItemClick wird dann aufgerufen, sobald ein UPnP Gerät aus der Liste ausgewählt wurde.
             * Die Parameter werden automatisch vom Android Framework an die Methode übergeben. Anschließend muss eine
             * Prüfung stattfinden, um was für ein UPnP Gerät es sich handelt. Wenn das UPnP Gerät nämlich, keine
             * Services hat, dann wird damit nicht weiter gearbeitet und entsprechende Fehlermeldung ausgegeben.
             *
             * @param parent Eltern AdapterView.
             * @param view   Basis Block für die Erzeugung und Interaktion
             *               mit grafischen Elementen.
             * @param position Welche Position ausgewählt wurde, aus der Liste der UPnP Geräte
             * @param id Welche id das jeweilige Element ausgewählte Element hat.
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String unique_device_identifier = listAdapter.getItem(position).getDevice().getIdentity().getUdn()
                                                                                           .getIdentifierString();

                if (listAdapter.getItem(position).getDevice().hasServices())
                {
                    Intent intent = new Intent(MainActivity.this, Switches.class);
                    intent.putExtra(EXTRA_MESSAGE, unique_device_identifier);
                    startActivity(intent);
                    showToast(listAdapter.getItem(position).getDeviceName()
                              + " selected!", false);
                }
                else
                {
                        showToast("Error: This UPnP Device has no Services! Cant handle it!", true);
                }
            }
        });
    }

    /**
     * Android Framework onStart() Methode (siehe Android Lifecycle auf google).
     */
    @Override
    protected void onStart()
    {
        super.onStart();
        getApplicationContext().bindService(
                new Intent(this, AndroidUpnpServiceImpl.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    /**
     * Android Framework onDestroy() Methode (siehe Android Lifecycle auf google).
     * Wenn die App beendet wird, wird diese Methode automatisch aufgerufen. Der UPnP Service wird beendet, um
     * alle nötigen Ressource für das Tablet wieder freizugeben, die von der App für das Service zur Laufzeit,
     * benötigt wurden.
     */
    @Override
    protected  void onDestroy()
    {
        super.onDestroy();
        if (upnpService != null)
        {
            upnpService.getRegistry().removeListener(registryListener);
        }
        getApplicationContext().unbindService(serviceConnection);
    }

    /**
     * Das Standard Menü der Activity wird initialisiert, wenn der User wünscht, eine manuelle Suche zu starten. Kann
     * der User über das Android Standard Activity Menü, eine neue Suche starten.
     * @param menu das Menü Parameter wird automatisch
     *             vom Framework zur Laufzeit übergeben.
     * @return Diese Methode muss true zurück liefern, damit das Menü angezeigt wird.
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, 0, 0, R.string.search_lan);
        return true;
    }

    /**
     * Wenn aus dem Menü das Element ausgewählt wurde, eine manuelle Suche zu starten.
     * Wird diese Methode vom Framework aufgerufen. Um anschließend die Methode searchNetwork() aufzurufen.
     *
     * @param item Welches menü Element ausgewählt wurde.
     *             Anhand der ID kann festgestellt welches menü Element ausgewählt wurde.
     * @return Der Rückgabewert ist false, sofern die ID nicht gefunden werden kann.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case 0:
                searchNetwork();
                break;
        }
        return false;
    }

    /**
     * Diese Methode startet die manuelle Suche nach UPnP Geräten.
     *
     */
    protected void searchNetwork()
    {
        if (upnpService == null)
        {
            return;
        }

        Toast.makeText(this, R.string.search_lan, Toast.LENGTH_SHORT).show();
        upnpService.getRegistry().removeAllRemoteDevices();
        upnpService.getControlPoint().search();
    }

    /**
     * Die Klasse BrowseRegistryListener, kümmert sich um die gefunden UPnP Geräte im Netzwerk. Diese Klasse hat die
     * Aufgabe alle UPnP Geräte der Liste hinzuzufügen um Sie anschließend auf dem Tablet darstellen zu können. Sofern
     * eine UPnP Gerät nicht mehr im Netzwerk ist, wird dieser auch wieder von der Liste entfernt. Der Auskommentierte
     * Code ist nur für sehr langsame Android Geräte, um die Verwaltung der UPnP Geräte nicht zu lange andauern zu
     * lassen.
     *
     */
    protected class BrowseRegistryListener extends DefaultRegistryListener
    {

        /* Discovery performance optimization for very slow Android devices! */
        /*
        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device)
        {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex)
        {

            showToast(
                    "Discovery failed of '" + device.getDisplayString() + "': " +
                            (ex != null ? ex.toString() : "Couldn't retrieve device/service descriptors"),
                    true
            );
            deviceRemoved(device);
        }

        /* End of optimization, you can remove the whole block if your
        Android handset is fast (>= 600 Mhz) */

        /**
         *
         * @param registry Die Cling Registry von allen Geräten und Services die dem lokalen UPnP Stack
         *                 bekannt sind.
         * @param device Das Geräte das ausgewärtet
         */
        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device)
        {

            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device)
        {

            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device)
        {

            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device)
        {

            deviceRemoved(device);
        }

        public void deviceAdded(final Device device)
        {

            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    DeviceDisplay d = new DeviceDisplay(device);

                    int position = listAdapter.getPosition(d);
                    if (position >= 0)
                    {
                        // Device already in the list, re-set new value at same position
                        listAdapter.remove(d);
                        listAdapter.insert(d, position);
                    }
                    else
                    {
                        listAdapter.add(d);
                    }

                    listAdapter.sort(DISPLAY_COMPARATOR);
                    listAdapter.notifyDataSetChanged();
                }
            });
        }

        public void deviceRemoved(final Device device)
        {

            runOnUiThread(new Runnable()
            {
                public void run()
                {
                    listAdapter.remove(new DeviceDisplay(device));
                }
            });
        }

    }

    protected void showToast(final String msg, final boolean longLength)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(
                        MainActivity.this,
                        msg,
                        longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
                ).show();
            }
        });
    }


    static final Comparator<DeviceDisplay> DISPLAY_COMPARATOR =
            new Comparator<DeviceDisplay>()
            {
                public int compare(DeviceDisplay a, DeviceDisplay b) {
                    return a.toString().compareTo(b.toString());
                }
            };


}