package com.map.android.service.communication.service;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.google.android.gms.analytics.HitBuilders;
import com.map.android.lib.drone.connection.ConnectionParameter;
import com.map.android.lib.drone.connection.ConnectionType;
import com.map.android.lib.gcs.link.LinkConnectionStatus;
import com.map.android.lib.model.ICommandListener;

import com.map.android.service.communication.connection.AndroidMavLinkConnection;
import com.map.android.service.communication.connection.AndroidTcpConnection;
import com.map.android.service.communication.connection.AndroidUdpConnection;
import com.map.android.service.communication.connection.BluetoothConnection;
import com.map.android.service.communication.connection.SoloConnection;
import com.map.android.service.communication.connection.usb.UsbConnection;
import com.map.android.service.communication.model.DataLink;
import com.map.android.service.core.MAVLink.connection.MavLinkConnection;
import com.map.android.service.core.MAVLink.connection.MavLinkConnectionListener;
import com.map.android.service.core.MAVLink.connection.MavLinkConnectionTypes;
import com.map.android.service.core.drone.manager.DroneCommandTracker;
import com.map.android.service.data.SessionDB;
import com.map.android.service.utils.analytics.GAUtils;
import com.map.android.service.utils.connection.WifiConnectionHandler;
import com.map.android.service.utils.file.DirectoryPath;
import com.map.android.service.utils.file.FileUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import timber.log.Timber;

/**
 * Provide a common class for some ease of use functionality
 */
public class MAVLinkClient implements DataLink.DataLinkProvider<MAVLinkMessage> {

    private static final int DEFAULT_SYS_ID = 255;
    private static final int DEFAULT_COMP_ID = 190;

    private static final String TLOG_PREFIX = "log";

    /**
     * Maximum possible sequence number for a packet.
     */
    private static final int MAX_PACKET_SEQUENCE = 255;

    private final MavLinkConnectionListener mConnectionListener = new MavLinkConnectionListener() {

        @Override
        public void onReceivePacket(final MAVLinkPacket packet) {
            listener.notifyReceivedData(packet);
        }

        @Override
        public void onConnectionStatus(final LinkConnectionStatus connectionStatus) {
            listener.onConnectionStatus(connectionStatus);

            switch (connectionStatus.getStatusCode()) {
                case LinkConnectionStatus.DISCONNECTED:
                    closeConnection();
                    break;

                case LinkConnectionStatus.CONNECTED:
                    Bundle extras = connectionStatus.getExtras();
                    if (extras != null) {
                        long connectionTime = extras.getLong(LinkConnectionStatus.EXTRA_CONNECTION_TIME);
                        if (connectionTime != 0) {
                            startLoggingThread(connectionTime);
                        }
                    }
                    break;
            }
        }
    };

    private AndroidMavLinkConnection mavlinkConn;

    private final DataLink.DataLinkListener<MAVLinkPacket> listener;
    private final SessionDB sessionDB;
    private final Context context;

    private int packetSeqNumber = 0;
    private final ConnectionParameter connParams;

    private DroneCommandTracker commandTracker;

    public MAVLinkClient(Context context, DataLink.DataLinkListener<MAVLinkPacket> listener,
                         ConnectionParameter connParams, DroneCommandTracker commandTracker) {
        this.context = context;
        this.listener = listener;

        if(connParams == null){
            throw new NullPointerException("Invalid connection parameter argument.");
        }

        this.connParams = connParams;
        this.sessionDB = new SessionDB(context);

        this.commandTracker = commandTracker;
    }

    private int getConnectionStatus(){
        return mavlinkConn == null
                ? MavLinkConnection.MAVLINK_DISCONNECTED
                : mavlinkConn.getConnectionStatus();
    }

    /**
     * Setup a MAVLink connection based on the connection parameters.
     */
    @Override
    public synchronized void openConnection() {
        if(isConnected() || isConnecting())
            return;

        final String tag = toString();

        //Create the mavlink connection
        final int connectionType = connParams.getConnectionType();
        final Bundle paramsBundle = connParams.getParamsBundle();

        if(mavlinkConn == null) {
            switch (connectionType) {
                case ConnectionType.TYPE_USB:
                    final int baudRate = paramsBundle.getInt(ConnectionType.EXTRA_USB_BAUD_RATE,
                            ConnectionType.DEFAULT_USB_BAUD_RATE);
                    mavlinkConn = new UsbConnection(context, baudRate);
                    Timber.i("Connecting over usb.");
                    break;

                case ConnectionType.TYPE_BLUETOOTH:
                    //Retrieve the bluetooth address to connect to
                    final String bluetoothAddress = paramsBundle.getString(ConnectionType.EXTRA_BLUETOOTH_ADDRESS);
                    mavlinkConn = new BluetoothConnection(context, bluetoothAddress);
                    Timber.i("Connecting over bluetooth.");
                    break;

                case ConnectionType.TYPE_TCP:
                    //Retrieve the server ip and port
                    final String tcpServerIp = paramsBundle.getString(ConnectionType.EXTRA_TCP_SERVER_IP);
                    final int tcpServerPort = paramsBundle.getInt(ConnectionType
                            .EXTRA_TCP_SERVER_PORT, ConnectionType.DEFAULT_TCP_SERVER_PORT);
                    mavlinkConn = new AndroidTcpConnection(context, tcpServerIp, tcpServerPort, new WifiConnectionHandler(context));
                    Timber.i("Connecting over tcp.");
                    break;

                case ConnectionType.TYPE_UDP:
                   // final int udpServerPort = paramsBundle
                    //        .getInt(ConnectionType.EXTRA_UDP_SERVER_PORT, ConnectionType.DEFAULT_UDP_SERVER_PORT);
                    final int udpServerPort=5444;
                    mavlinkConn = new AndroidUdpConnection(context, udpServerPort, new WifiConnectionHandler(context));
                    Timber.i("Connecting over udp.");
                    break;

                case ConnectionType.TYPE_SOLO: {
                    Timber.i("Creating solo connection");
                    final String soloLinkId = paramsBundle.getString(ConnectionType.EXTRA_SOLO_LINK_ID, null);
                    final String linkPassword = paramsBundle.getString(ConnectionType.EXTRA_SOLO_LINK_PASSWORD, null);
                    mavlinkConn = new SoloConnection(context, soloLinkId, linkPassword);
                    break;
                }

                default:
                    Timber.e("Unrecognized connection type: %s", connectionType);
                    return;
            }
        }

        mavlinkConn.addMavLinkConnectionListener(tag, mConnectionListener);

        //Check if we need to ping a server to receive UDP data stream.
        if (connectionType == ConnectionType.TYPE_UDP) {
            final String pingIpAddress = paramsBundle.getString(ConnectionType.EXTRA_UDP_PING_RECEIVER_IP);
            if (!TextUtils.isEmpty(pingIpAddress)) {
                try {
                    final InetAddress resolvedAddress = InetAddress.getByName(pingIpAddress);

                    final int pingPort = paramsBundle.getInt(ConnectionType.EXTRA_UDP_PING_RECEIVER_PORT);
                    final long pingPeriod = paramsBundle.getLong(ConnectionType.EXTRA_UDP_PING_PERIOD,
                            ConnectionType.DEFAULT_UDP_PING_PERIOD);
                    final byte[] pingPayload = paramsBundle.getByteArray(ConnectionType.EXTRA_UDP_PING_PAYLOAD);

                    ((AndroidUdpConnection) mavlinkConn).addPingTarget(resolvedAddress, pingPort, pingPeriod, pingPayload);

                } catch (UnknownHostException e) {
                    Timber.e(e, "Unable to resolve UDP ping server ip address.");
                }
            }
        }

        if (mavlinkConn.getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED) {
            mavlinkConn.connect();

            // Record which connection type is used.
            GAUtils.sendEvent(new HitBuilders.EventBuilder()
                    .setCategory(GAUtils.Category.MAVLINK_CONNECTION)
                    .setAction("MavLink connect")
                    .setLabel(connParams.toString()));
        }
    }

    /**
     * Disconnect the MAVLink connection for the given listener.
     */
    @Override
    public synchronized void closeConnection() {
        if (isDisconnected())
            return;

        mavlinkConn.removeMavLinkConnectionListener(toString());
        if(mavlinkConn.getMavLinkConnectionListenersCount() == 0){
            Timber.i("Disconnecting...");
            mavlinkConn.disconnect();
            GAUtils.sendEvent(new HitBuilders.EventBuilder()
                    .setCategory(GAUtils.Category.MAVLINK_CONNECTION)
                    .setAction("MavLink disconnect")
                    .setLabel(connParams.toString()));
        }

        stopLoggingThread(System.currentTimeMillis());

        listener.onConnectionStatus(new LinkConnectionStatus(LinkConnectionStatus.DISCONNECTED, null));
    }

    @Override
    public synchronized void sendMessage(MAVLinkMessage message, ICommandListener listener) {
        sendMavMessage(message, DEFAULT_SYS_ID, DEFAULT_COMP_ID, listener);
    }

    protected void sendMavMessage(MAVLinkMessage message, int sysId, int compId, ICommandListener listener){
        if (isDisconnected() || message == null) {
            return;
        }

        final MAVLinkPacket packet = message.pack();
        packet.sysid = sysId;
        packet.compid = compId;
        packet.seq = packetSeqNumber;

        mavlinkConn.sendMavPacket(packet);

        packetSeqNumber = (packetSeqNumber + 1) % (MAX_PACKET_SEQUENCE + 1);

        if (commandTracker != null && listener != null) {
            commandTracker.onCommandSubmitted(message, listener);
        }
    }

    public synchronized boolean isDisconnected(){
        return getConnectionStatus() == MavLinkConnection.MAVLINK_DISCONNECTED;
    }

    @Override
    public synchronized boolean isConnected() {
        return getConnectionStatus() == MavLinkConnection.MAVLINK_CONNECTED;
    }

    private boolean isConnecting(){
        return getConnectionStatus() == MavLinkConnection.MAVLINK_CONNECTING;
    }

    private File getTLogDir(String appId) {
        return DirectoryPath.getTLogPath(this.context, appId);
    }

    private File getTempTLogFile(String appId, long connectionTimestamp) {
        return new File(getTLogDir(appId), getTLogFilename(connectionTimestamp));
    }

    private String getTLogFilename(long connectionTimestamp) {
        return TLOG_PREFIX + "_" + MavLinkConnectionTypes.getConnectionTypeLabel(this.connParams.getConnectionType()) +
                "_" + FileUtils.getTimeStamp(connectionTimestamp) + FileUtils.TLOG_FILENAME_EXT;
    }

    /**
     * Register a log listener.
     *
     * @param appId             Tag for the listener.
     */
    public synchronized void addLoggingFile(String appId){
        if(isConnecting() || isConnected()) {
            final File logFile = getTempTLogFile(appId, System.currentTimeMillis());
            mavlinkConn.addLoggingPath(appId, logFile.getAbsolutePath());
        }
    }

    /**
     * Unregister a log listener.
     *
     * @param appId        Tag for the listener.
     */
    public synchronized void removeLoggingFile(String appId){
        if(isConnecting() || isConnected()){
            mavlinkConn.removeLoggingPath(appId);
        }
    }

    private void startLoggingThread(long startTime) {
        //log into the database the connection time.
        final String connectionType = MavLinkConnectionTypes.getConnectionTypeLabel(connParams.getConnectionType());
        this.sessionDB.startSession(new Date(startTime), connectionType);
    }

    private void stopLoggingThread(long stopTime) {
        //log into the database the disconnection time.
        final String connectionType = MavLinkConnectionTypes.getConnectionTypeLabel(connParams.getConnectionType());
        this.sessionDB.endSession(new Date(stopTime), connectionType, new Date());
    }
}
