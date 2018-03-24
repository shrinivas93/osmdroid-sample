package com.shrinivas.osmdroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    MapView map = null;
    IMapController mapController = null;
    Context context = null;
    //    MyLocationNewOverlay locationOverlay = null;
    CompassOverlay compassOverlay = null;
    //    LatLonGridlineOverlay2 latLonGridlineOverlay = null;
    RotationGestureOverlay rotationGestureOverlay = null;
    LocationManager locationManager = null;
    ItemizedOverlayWithFocus<OverlayItem> mOverlay = null;
    MapEventsOverlay eventsOverlay = null;
    ArrayList<OverlayItem> markers;
    Geocoder geocoder = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //load/initialize the osmdroid configuration, this can be done
        context = getApplicationContext();

        // Initializing the Geocoder
        geocoder = new Geocoder(context);

        //handle permissions first, before map is created. not depicted here
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        // Configure Location Manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //inflate and create the map
        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setClickable(true);
        map.setHorizontalMapRepetitionEnabled(false);
        map.setVerticalMapRepetitionEnabled(false);

        mapController = map.getController();
        mapController.setZoom(15.0);


//        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map);
//        locationOverlay.enableMyLocation();
//        map.getOverlays().add(locationOverlay);

        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Toast.makeText(context, "Location - [" + location.getLatitude() + ", " + location.getLongitude() + "]", Toast.LENGTH_LONG).show();
            mapController.setCenter(new GeoPoint(location));

            //your markers
            markers = new ArrayList<OverlayItem>();
            markers.add(new OverlayItem("Current Location", "[" + location.getLatitude() + ", " + location.getLongitude() + "]", new GeoPoint(location))); // Lat/Lon decimal degrees

            //the overlay
            mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(context, markers,
                    new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                        @Override
                        public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                            Toast.makeText(context, markers.get(index).getTitle(), Toast.LENGTH_LONG).show();
                            return false;
                        }

                        @Override
                        public boolean onItemLongPress(final int index, final OverlayItem item) {
                            Toast.makeText(context, markers.get(index).getSnippet(), Toast.LENGTH_LONG).show();
                            return false;
                        }
                    });
            mOverlay.setFocusItemsOnTap(true);

            map.getOverlays().add(mOverlay);
        } catch (SecurityException se) {
            Toast.makeText(context, "Location Permission Error", Toast.LENGTH_LONG).show();
        }

        compassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

//        latLonGridlineOverlay = new LatLonGridlineOverlay2();
//        map.getOverlays().add(latLonGridlineOverlay);

        rotationGestureOverlay = new RotationGestureOverlay(map);
        rotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(rotationGestureOverlay);

        eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
                Toast.makeText(context, "Address - " + getAddressFromLocation(geoPoint), Toast.LENGTH_SHORT).show();
                Toast.makeText(context, geoPoint.getLatitude() + " - " + geoPoint.getLongitude(), Toast.LENGTH_LONG).show();
                mOverlay.addItem(new OverlayItem("Single Tap Location", "[" + geoPoint.getLatitude() + ", " + geoPoint.getLongitude() + "]", geoPoint));
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint geoPoint) {
                Toast.makeText(context, "Address - " + getAddressFromLocation(geoPoint), Toast.LENGTH_SHORT).show();
                Toast.makeText(context, geoPoint.getLatitude() + " - " + geoPoint.getLongitude(), Toast.LENGTH_LONG).show();
                mOverlay.addItem(new OverlayItem("Long Press Location", "[" + geoPoint.getLatitude() + ", " + geoPoint.getLongitude() + "]", geoPoint));
                return false;
            }
        });
        map.getOverlays().add(eventsOverlay);

    }

    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkAndRequestPermissions() {

        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(context, "Unable to fetch PackageInfo", Toast.LENGTH_SHORT).show();
        }

        String[] requestedPermissions = packageInfo.requestedPermissions;
        List<String> listPermissionsDenied = new ArrayList<>();

        for (String permission : requestedPermissions) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsDenied.add(permission);
            }
        }

        if (!listPermissionsDenied.isEmpty()) {
            requestPermissions(listPermissionsDenied.toArray(new String[listPermissionsDenied.size()]), 1);
            return false;
        }
        return true;
    }

    String getAddressFromLocation(GeoPoint geoPoint) {
        StringBuilder sb = new StringBuilder();
        try {
            List<Address> addresses = geocoder.getFromLocation(geoPoint.getLatitude(), geoPoint.getLongitude(), 1);
            if (addresses.isEmpty()) {
                return sb.toString();
            }
            Address address = addresses.get(0);
            int maxAddressLineIndex = address.getMaxAddressLineIndex();
            for (int i = 0; i <= maxAddressLineIndex; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                String line = address.getAddressLine(i);
                if (line != null) {
                    sb.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
