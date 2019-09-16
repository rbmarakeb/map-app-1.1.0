package com.map.android.service.communication.model;

import com.map.android.lib.gcs.link.LinkConnectionStatus;
import com.map.android.lib.model.ICommandListener;

public class DataLink {

    public interface DataLinkProvider<T> {

        void sendMessage(T message, ICommandListener listener);

        boolean isConnected();

        void openConnection();

        void closeConnection();

    }

    public interface DataLinkListener<T> {

        void notifyReceivedData(T packet);

        void onConnectionStatus(LinkConnectionStatus connectionStatus);
    }
}
