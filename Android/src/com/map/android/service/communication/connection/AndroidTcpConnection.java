package com.map.android.service.communication.connection;

import android.content.Context;

import com.map.android.lib.gcs.link.LinkConnectionStatus;

import com.map.android.service.core.MAVLink.connection.TcpConnection;
import com.map.android.service.core.model.Logger;
import com.map.android.service.utils.connection.WifiConnectionHandler;

import java.io.IOException;

public class AndroidTcpConnection extends AndroidIpConnection {

    private final TcpConnection mConnectionImpl;

    private final String serverIp;
    private final int serverPort;

    public AndroidTcpConnection(Context context, String tcpServerIp, int tcpServerPort, WifiConnectionHandler wifiHandler){
        super(context, wifiHandler);

        this.serverIp = tcpServerIp;
        this.serverPort = tcpServerPort;

        mConnectionImpl = new TcpConnection() {
            @Override
            protected int loadServerPort() {
                return serverPort;
            }

            @Override
            protected String loadServerIP() {
                return serverIp;
            }

            @Override
            protected Logger initLogger() {
                return AndroidTcpConnection.this.initLogger();
            }

            @Override
            protected void onConnectionOpened() {
                AndroidTcpConnection.this.onConnectionOpened();
            }

            @Override
            protected void onConnectionStatus(LinkConnectionStatus connectionStatus) {
                AndroidTcpConnection.this.onConnectionStatus(connectionStatus);
            }
        };
    }

    public AndroidTcpConnection(Context context, String tcpServerIp, int tcpServerPort) {
        this(context, tcpServerIp, tcpServerPort, null);
    }

    @Override
    protected void onCloseConnection() throws IOException {
        mConnectionImpl.closeConnection();
    }

    @Override
    protected void loadPreferences() {
        mConnectionImpl.loadPreferences();
    }

    @Override
    protected void onOpenConnection() throws IOException {
        mConnectionImpl.openConnection();
    }

    @Override
    protected int readDataBlock(byte[] buffer) throws IOException {
        return mConnectionImpl.readDataBlock(buffer);
    }

    @Override
    protected void sendBuffer(byte[] buffer) throws IOException {
        mConnectionImpl.sendBuffer(buffer);
    }

    @Override
    public int getConnectionType() {
        return mConnectionImpl.getConnectionType();
    }
}
