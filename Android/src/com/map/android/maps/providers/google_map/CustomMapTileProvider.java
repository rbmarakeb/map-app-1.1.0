package com.map.android.maps.providers.google_map;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.map.android.data.DatabaseState;
import com.map.android.utils.file.DirectoryPath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by User on 15-Aug-16.
 */
public class CustomMapTileProvider implements TileProvider {
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
    private static final int BUFFER_SIZE = 16 * 1024;

    private AssetManager mAssets;
    private static final SparseArray<Rect> TILE_ZOOMS = new SparseArray<Rect>() {{

        put(15, new Rect(11967, 19997, 11969, 19996));
    }};
    public CustomMapTileProvider(AssetManager assets) {
        mAssets = assets;
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {

           // y = fixYCoordinate(y, zoom);
        Log.d(String.valueOf(x), String.valueOf(y) +"zooom" + String.valueOf(zoom));
        byte[] image = readTileImage(x, y, zoom);

            if (image == null || image.length == 0)
            {   image = readTileImage(11967, 19999, 15);
                return image == null ? null : new Tile(TILE_WIDTH, TILE_HEIGHT, image);}
            else{return image == null ? null : new Tile(TILE_WIDTH, TILE_HEIGHT, image);}
           // return NO_TILE;

    }

    private byte[] readTileImage(int x, int y, int zoom) {
        //InputStream in = null;
        FileInputStream in = null;
        ByteArrayOutputStream buffer = null;


        try {
            File file = new File(getTileFilename(x, y, zoom));
            in = new FileInputStream(file);
            //in = mAssets.open(getTileFilename(x, y, zoom));
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
        return DirectoryPath.getMapsPath() + zoom + '/' + y + '/' + x + ".jpg";
    }
    private boolean hasTile(int x, int y, int zoom) {
        Rect b = TILE_ZOOMS.get(zoom);
        return b == null ? false : (b.left <= x && x <= b.right && b.top <= y && y <= b.bottom);
    }
    private int fixYCoordinate(int y, int zoom) {
        int size = 1 << zoom; // size = 2^zoom
        return size - 1 - y;
    }
}