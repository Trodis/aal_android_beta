package com.example.aal_app;

/**
 * Copyrigt (c) 2013, Hochschule Bochum
 * Diese Software wurde im Rahmen eines Software Projektes für die Hochschule Bochum entwickelt.
 * Dieser Code darf nicht ohne Einverständniss von Dr. Prof. Weidauer weiterentwickelt oder zu eigenen Zwecken
 * genutzt werden. Bitte kontaktieren Sie Herr Weidauer, für weitere Fragen.
 * @author Ferhat Özmen
 * @version 0.1
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
 * Diese Klasse ist die Hauptklasse, wo alle UPnP Ger&auml;te gesucht und aufgelistet werden. Der Nutzer soll nach der
 * Auflistung die M&ouml;glichkeit haben, eines der UPnP Ger&auml;te auszuw&auml;hlen und es anschlie&szlig;end zu bedienen.
 *
 * Dieser Code wurde wie bereits oben erw&auml;hnt im Rahmen eines Software Projektes entwickelt. Erfahrene Entwickler,
 * werden viele verbesserungs Vorschl&auml;ge und Kritik haben. Auch wurde vermutlich nicht jedes Problem,
 * auf die Weise gel&ouml;st, die ein erfahrener Entwickler gel&ouml;st h&auml;tte. Diesbez&uuml;glich bitte ich um Nachsicht und die
 * entsprechend herangehensweise beim Lesen des Codes.
 *
 * @author Ferhat &Ouml;zmen
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
         * Ein Service muss im Hintergrund laufen w&auml;hrend die App l&auml;uft um neue UPnP Ger&auml;te automatisch zu finden
         * und wieder von der Liste zu entfernen sofern das UPnP Ger&auml;t vom Netzwerk getrennt wurde.
         *
         * @param className F&uuml;r welche Klasse der Service gestartet werden soll
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
         * @param className F&Uuml;r welche Klasse der Service beendet werden.
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
     *
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
     * alle n&ouml;tigen Ressource f&uuml;r das Tablet wieder freizugeben, die von der App f&uuml;r das Service zur Laufzeit,
     * ben&ouml;tigt wurden.
     *
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
     * Das Standard Men&uuml; der Activity wird initialisiert, wenn der User w&uuml;nscht, eine manuelle Suche zu starten. Kann
     * der User &uuml;ber das Android Standard Activity Men&uuml;, eine neue Suche starten.
     * @param menu das Men&uuml; Parameter wird automatisch
     *             vom Framework zur Laufzeit &uuml;bergeben.
     * @return Diese Methode muss true zur&uuml;ck liefern, damit das Men&uuml; angezeigt wird.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, 0, 0, R.string.search_lan);
        return true;
    }

    /**
     * Wenn aus dem Men&uuml; das Element ausgew&auml;hlt wurde, eine manuelle Suche zu starten.
     * Wird diese Methode vom Framework aufgerufen. Um anschlie&szlig;end die Methode searchNetwork() aufzurufen.
     *
     * @param item Welches men&uuml; Element ausgew&auml;hlt wurde.
     *             Anhand der ID kann festgestellt welches men&uuml; Element ausgew&auml;hlt wurde.
     * @return Der R&uuml;ckgabewert ist false, sofern die ID nicht gefunden werden kann.
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
     * Die Klasse BrowseRegistryListener, k&uuml;mmert sich um die gefunden UPnP Ger&auml;te im Netzwerk. Diese Klasse hat die
     * Aufgabe alle UPnP Ger&auml;te der Liste hinzuzuf&uuml;gen um Sie anschlie&szlig;end auf dem Tablet darstellen zu k&ouml;nnen. Sofern
     * eine UPnP Ger&auml;t nicht mehr im Netzwerk ist, wird dieser auch wieder von der Liste entfernt. Der Auskommentierte
     * Code ist nur f&uuml;r sehr langsame Android Ger&auml;te, um die Verwaltung der UPnP Ger&auml;te nicht zu lange andauern zu
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
         * Neues Ger&auml;t aus dem Netzwerk, soll hinzugef&uuml;gt werden.
         *
         * @param registry Die Cling Registry von allen Ger&auml;ten und Services die dem lokalen UPnP Stack
         *                 bekannt sind.
         * @param device Das Ger&auml;te das ausgew&auml;rtet wurde mit seinen service meta Daten.
         */
        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device)
        {

            deviceAdded(device);
        }

        /**
         * Vorhandenes Ger&auml;t soll aus der Liste wird entfernt, welches nicht mehr im Netzwerk ist.
         *
         * @param registry Die Cling Registry von allen Ger&auml;ten und Services die dem lokalen UPnP Stack
         *                 bekannt sind.
         * @param device Das UPnP Ger&auml;t das ausgew&auml;rtet wurde mit seinen service meta Daten.
         *
         */
        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device)
        {

            deviceRemoved(device);
        }

        /**
         * Lokales Ger&auml;t soll hinzugef&uuml;gt werden.
         *
         * @param registry Die Cling Registry von allen Ger&auml;ten und Services die dem lokalen UPnP Stack
         *                 bekannt sind.
         * @param device Das Ger&auml;te das ausgew&auml;rtet wurde mit seinen service meta Daten.
         */
        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device)
        {

            deviceAdded(device);
        }

        /**
         * Lokales Ger&auml;t wird entfernt.
         *
         * @param registry Die Cling Registry von allen Ger&auml;ten und Services die dem lokalen UPnP Stack
         *                   bekannt sind.
         * @param device Das Ger&auml;te das ausgew&auml;rtet wurde mit seinen service meta Daten.
         */
        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device)
        {

            deviceRemoved(device);
        }

        /**
         * Das UPnP Ger&auml;t welches gefunden wurde, wird nun dem listAdapter hinzugef&uuml;gt. Um es sp&auml;ter
         * auflisten zu k&ouml;nnen. Dabei findet das hinzuf&uuml;gen in einem runOnUiThread statt,
         * da Android es sonst nicht erlaubt
         * ohne diesen Thread das UI zur Laufzeit zu ver&auml;ndern.
         * @param device Das Ger&auml;te das ausgew&auml;rtet wurde mit seinen service meta Daten.
         */
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

        /**
         * Das UPnP Ger&auml;t wird &uuml;ber diese Methode aus dem listAdapter und somit aus der Liste entfernt. Auch hier ist
         * ein Eingriff zur Laufzeit nur m&ouml;glich wenn es &uuml;ber den runOnUiThread stattfindet,
         * da die UI ver&auml;ndert werden soll.
         *
         * @param device Das Ger&auml;te das ausgew&auml;rtet wurde mit seinen service meta Daten.
         */
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

    /**
     * Diese Methode dient dazu um die Toast Messages auf dem Android Ger&auml;t auszugeben. Es kann f&uuml;r Fehlermeldung und
     * andere Meldungen genutzt werden um Informationen dem User mitzuteilen.
     *
     * @param msg Die Nachricht die als Toast ausgegeben werden soll.
     * @param longLength Es kann True &uuml;bergeben werden die dauer der Toast Message lange andauern soll, oder
     *                   False falls die Toast Nachricht nur kurz bestehen soll.
     */
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

    /**
     * Diese Methode dient nur dazu um die Liste der UPnP Ger&auml;t zu nach Ihrem Namen zu sortieren.
     */
    static final Comparator<DeviceDisplay> DISPLAY_COMPARATOR =
            new Comparator<DeviceDisplay>()
            {
                public int compare(DeviceDisplay a, DeviceDisplay b) {
                    return a.toString().compareTo(b.toString());
                }
            };


}