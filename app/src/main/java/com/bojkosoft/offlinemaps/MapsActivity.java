package com.bojkosoft.offlinemaps;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.bojkosoft.offlinemaps.tiles.CustomTileProvider;
import com.bojkosoft.offlinemaps.tiles.ReadMapLayers;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static String[] PERMISSIONS_GPS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;


    public static final String CONFIG_FILE_LOCATION = "/OfflineMaps/offline-tiles.json";
    public static final String MAP_ROTATION = "ROTATION";
    public static final String MAP_ZOOM = "ZOOM";
    public static final String MAP_CENTER_LATITUDE = "LATITUDE";
    public static final String MAP_CENTER_LONGITUDE = "LONGITUDE";
    public static final String LOADED_OVERLAY_NAME = "LOADED_OVERLAY_NAME";

    public static final float INIT_CENTER_LATITUDE = 42.684059f;
    public static final float INIT_CENTER_LONGITUDE = 23.329258f;
    public static final float INIT_ZOOM = 11f;
    public static final float INIT_ROTATION = 0.0f;


    private GoogleMap mMap;
    private TileOverlay mTileOverlay;
    private Map<String, CustomTileProvider> mCustomTileProviders;
    private String mLoadedOverlayName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        /*
         * INIT PREDEFINED LAYERS: google road, satellite....
         */
        this.mCustomTileProviders = CustomTileProvider.getPredefinedLayers();

        /*
         * CHECK APP PERMISSIONS: GPS and READ ACCESS to the storage
         */
        this.checkPermissions();

        /*
         * READ MAP LAYERS FROM CONFIG FILE
         */
        this.readMapLayers(CONFIG_FILE_LOCATION);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    protected void onPause() {
        if (this.mMap != null) {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            CameraPosition camera = this.mMap.getCameraPosition();
            editor.putFloat(MAP_ROTATION, camera.bearing);
            editor.putFloat(MAP_ZOOM, camera.zoom);
            editor.putFloat(MAP_CENTER_LATITUDE, (float) camera.target.latitude);
            editor.putFloat(MAP_CENTER_LONGITUDE, (float) camera.target.longitude);
            editor.putString(LOADED_OVERLAY_NAME, this.mLoadedOverlayName);
            editor.apply();
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (this.mMap != null) {
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

            double latitude = sharedPref.getFloat(MAP_CENTER_LATITUDE, INIT_CENTER_LATITUDE);
            double longitude = sharedPref.getFloat(MAP_CENTER_LONGITUDE, INIT_CENTER_LONGITUDE);
            float zoom = sharedPref.getFloat(MAP_ZOOM, INIT_ZOOM);
            float rotation = sharedPref.getFloat(MAP_ROTATION, INIT_ROTATION);

            // load last map position and overlay
            this.setMapCamera(latitude, longitude, zoom, rotation);
            this.activateTiledLayer(sharedPref.getString(LOADED_OVERLAY_NAME, CustomTileProvider.CUSTOM_ROAD_MAP));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (this.mMap != null) {
            CameraPosition camera = this.mMap.getCameraPosition();
            outState.putFloat(MAP_ROTATION, camera.bearing);
            outState.putFloat(MAP_ZOOM, camera.zoom);
            outState.putFloat(MAP_CENTER_LATITUDE, (float) camera.target.latitude);
            outState.putFloat(MAP_CENTER_LONGITUDE, (float) camera.target.longitude);
            outState.putString(LOADED_OVERLAY_NAME, this.mLoadedOverlayName);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (this.mMap != null) {
            double latitude = savedInstanceState.getDouble(MAP_CENTER_LATITUDE, INIT_CENTER_LATITUDE);
            double longitude = savedInstanceState.getDouble(MAP_CENTER_LONGITUDE, INIT_CENTER_LONGITUDE);
            float zoom = savedInstanceState.getFloat(MAP_ZOOM, INIT_ZOOM);
            float rotation = savedInstanceState.getFloat(MAP_ZOOM, INIT_ROTATION);

            // load last map position and overlay
            this.setMapCamera(latitude, longitude, zoom, rotation);
            this.activateTiledLayer(savedInstanceState.getString(LOADED_OVERLAY_NAME));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_add_overlay:
                this.chooseOverlayToLoad();
                break;
            case R.id.item_add_kml:
                break;
            case R.id.item_help:
                break;
            default:
                break;
        }
        return true;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;

        /*
         * SET MAP UI
         */
        this.mMap.getUiSettings().setZoomGesturesEnabled(true);
        this.mMap.getUiSettings().setScrollGesturesEnabled(true);
        this.mMap.getUiSettings().setRotateGesturesEnabled(true);
        this.mMap.getUiSettings().setTiltGesturesEnabled(false);

        this.mMap.getUiSettings().setZoomControlsEnabled(true);
        this.mMap.getUiSettings().setCompassEnabled(true);
        this.mMap.getUiSettings().setMyLocationButtonEnabled(true);

        //this.mMap.setOnMyLocationButtonClickListener(this);
        //this.mMap.setOnMyLocationClickListener(this);

        /*
         * SET MAP INITIAL CENTER
         */
        this.setMapCamera(INIT_CENTER_LATITUDE, INIT_CENTER_LONGITUDE, INIT_ZOOM, INIT_ROTATION);

        /*
         * ACTIVATE INITIAL LAYER
         */
        this.activateTiledLayer(CustomTileProvider.CUSTOM_ROAD_MAP);

        /*
         * CHECK FOR PERMISSIONS AND ENABLE MY LOCATION BUTTON
         */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.mMap.setMyLocationEnabled(true);
    }

    /**
     * Initialize map overlays, returned by ReadMapLayers async task
     *
     * @param layers - ab array of overlays, ready for use
     */
    public void initMapLayers(ArrayList<CustomTileProvider> layers) {
        for (CustomTileProvider layer : layers) {
            if (!this.mCustomTileProviders.containsKey(layer.getTitle())) {
                this.mCustomTileProviders.put(layer.getTitle(), layer);
            }
        }
    }


    /**
     * activate an overlay: if the overlay is found in config
     *
     * @param title - title for the overlay
     */
    private void activateTiledLayer(String title) {
        if (this.mCustomTileProviders.containsKey(title)) {

            if (this.mTileOverlay != null) {
                this.mTileOverlay.remove();
            }

            CustomTileProvider overlay = this.mCustomTileProviders.get(title);
            if (overlay != null) {
                this.mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                this.mTileOverlay = this.mMap.addTileOverlay(new TileOverlayOptions().tileProvider(overlay));
            } else {
                switch (title) {
                    case CustomTileProvider.ROAD_MAP:
                        this.mMap.setMapStyle(null);
                        this.mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                    case CustomTileProvider.SATELLITE_MAP:
                        this.mMap.setMapStyle(null);
                        this.mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        break;
                    case CustomTileProvider.TERRAIN_MAP:
                        this.mMap.setMapStyle(null);
                        this.mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        break;
                    case CustomTileProvider.CUSTOM_ROAD_MAP:
                    default:
                        this.mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
                        this.mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        break;
                }
            }

            this.mLoadedOverlayName = title;
        }
    }

    /**
     * select an overlay to load: choose from a list
     */
    private void chooseOverlayToLoad() {
        if (this.mCustomTileProviders != null && this.mCustomTileProviders.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.label_choose_basemap));

            final String[] layers = new String[this.mCustomTileProviders.size()];
            this.mCustomTileProviders.keySet().toArray(layers);
            Arrays.sort(layers);

            builder.setItems(layers, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activateTiledLayer(layers[which]);
                }
            });

            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * read overlays, defined in the config file
     *
     * @param path - path to config JSON file
     */
    private void readMapLayers(String path) {
        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists() || file.isDirectory()) {
            showAlert(String.format(getString(R.string.no_config), Environment.getExternalStorageDirectory() + path));
            //this.hideLoadingBox();
        } else {
            new ReadMapLayers(this).execute(file.getAbsolutePath());
        }
    }

    /**
     * set map camera - view direction and center
     *
     * @param latitude  - geographic latitude of map's center
     * @param longitude - geographic longitude of map's center
     * @param zoom      - map ozom level
     * @param rotation  - map rotation in degrees
     */
    private void setMapCamera(double latitude, double longitude, float zoom, float rotation) {
        LatLng sofia = new LatLng(latitude, longitude);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(sofia)
                .zoom(zoom)
                .bearing(rotation)
                .build();
        this.mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }


    private void showAlert(String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(message);
        alert.setCancelable(true);
        alert.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        alert.create().show();
    }

    private void checkPermissions() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // ask user for permissions
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_GPS,
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }

        permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // ask user for permissions
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                this.mMap.setMyLocationEnabled(true);
            } else {
                // Permission was denied. Display an error message.
            }
        }
    }
}