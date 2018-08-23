package com.bojkosoft.offlinemaps.tiles;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CustomTileProvider extends UrlTileProvider {

    public static final String CUSTOM_ROAD_MAP = "Custom road map";
    public static final String ROAD_MAP = "Road map";
    public static final String SATELLITE_MAP = "Satellite map";
    public static final String TERRAIN_MAP = "Terrain map";

    private String mName;
    private String mTitle;
    private String mUrlTemplate;

    public CustomTileProvider(String name, String title, String urlTemplate) {
        super(256, 256);
        this.mName = name;
        this.mTitle = title;
        this.mUrlTemplate = urlTemplate
                .replaceAll("\\{z\\}", "%d")
                .replaceAll("\\{x\\}", "%d")
                .replaceAll("\\{y\\}", "%d");
    }

    public String getName() {
        return this.mName;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getUrlTemplate() {
        return this.mUrlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.mUrlTemplate = urlTemplate;
    }

    public static Map<String, CustomTileProvider> getPredefinedLayers() {
        Map<String, CustomTileProvider> layers = new HashMap<>();
        layers.put(CUSTOM_ROAD_MAP, null);
        layers.put(ROAD_MAP, null);
        layers.put(SATELLITE_MAP, null);
        layers.put(TERRAIN_MAP, null);

        return layers;
    }


    @Override
    public URL getTileUrl(int x, int y, int zoom) {
        try {
            String s = String.format(this.mUrlTemplate, zoom, x, y);
            return new URL(s);
        } catch (MalformedURLException e) {
            //throw new AssertionError(e);
            return null;
        }
    }
}
