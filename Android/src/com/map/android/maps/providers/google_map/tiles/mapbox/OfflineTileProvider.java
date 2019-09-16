package com.map.android.maps.providers.google_map.tiles.mapbox;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.map.android.data.DatabaseState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Fredia Huya-Kouadio on 5/11/15.
 */
public class OfflineTileProvider implements TileProvider {

    private static final String TAG = OfflineTileProvider.class.getSimpleName();

    private final Context context;
    private final String mapboxId;
    private final String mapboxAccessToken;
    private final int maxZoomLevel;
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
    private static final int BUFFER_SIZE = 16 * 1024;
    private AssetManager mAssets;

    public OfflineTileProvider(Context context, String mapboxId, String mapboxAccessToken, int maxZoomLevel, AssetManager assets) {
        this.context = context;
        this.mapboxId = mapboxId;
        this.mapboxAccessToken = mapboxAccessToken;
        this.maxZoomLevel = maxZoomLevel;
        mAssets = assets;
    }
    @Override
    public Tile getTile(int x, int y, int zoom) {
        if (zoom > maxZoomLevel) {
            return TileProvider.NO_TILE;
        }

        final String tileUri = MapboxUtils.getMapTileURL(mapboxId, mapboxAccessToken, zoom, x, y);
        byte[] data = readTileImage(x, y, zoom);

        if (data == null || data.length == 0)
            return TileProvider.NO_TILE;

        return data == null ? null : new Tile(TILE_WIDTH, TILE_HEIGHT, data);
    }
    private byte[] readTileImage(int x, int y, int zoom) {
        y = fixYCoordinate(y, zoom);
        InputStream in = null;
        ByteArrayOutputStream buffer = null;

        try {
            in = mAssets.open(getTileFilename(x, y, zoom));
            buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[BUFFER_SIZE];

            while ((nRead = in.read(data, 0, BUFFER_SIZE)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            return buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (buffer != null) try { buffer.close(); } catch (Exception ignored) {}
        }
    }

    private String getTileFilename(int x, int y, int zoom) {
        return "map/" + zoom + '/' + x + '/' + y + ".png";



    }
    private int fixYCoordinate(int y, int zoom) {
        int size = 1 << zoom; // size = 2^zoom
        return size - 1 - y;
    }
}
