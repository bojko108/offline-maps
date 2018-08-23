package com.bojkosoft.offlinemaps.tiles;

import android.os.AsyncTask;

import com.bojkosoft.offlinemaps.MapsActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ReadMapLayers extends AsyncTask<String, Integer, ArrayList<CustomTileProvider>> {
    private WeakReference<MapsActivity> activityReference;

    // only retain a weak reference to the activity
    public ReadMapLayers(MapsActivity context) {
        activityReference = new WeakReference<>(context);
    }

    @Override
    protected ArrayList<CustomTileProvider> doInBackground(String... inputs) {
        ArrayList<CustomTileProvider> result = new ArrayList<CustomTileProvider>();

        try {
            File file = new File(inputs[0]);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            String jsonData = new String(data, "UTF-8");

            JSONArray jsonArray = new JSONArray(jsonData);

            for (int i = 0; i < jsonData.length(); ++i) {
                JSONObject layer = jsonArray.getJSONObject(i);
                CustomTileProvider customTileProvider = new CustomTileProvider(
                        layer.getString("name"),
                        layer.getString("title"),
                        layer.getString("url"));
                result.add(customTileProvider);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    @Override
    protected void onPostExecute(ArrayList<CustomTileProvider> result) {
        MapsActivity activity = activityReference.get();
        //activity.hideLoadingBox();
        activity.initMapLayers(result);
    }
}