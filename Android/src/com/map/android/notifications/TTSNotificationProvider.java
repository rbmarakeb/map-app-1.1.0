package com.map.android.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.map.android.activities.FlightActivity;
import com.map.android.activities.RemoteHelmActivity;
import com.map.android.activities.SettingsActivity;
import com.map.android.fragments.FlightDataFragment;
import com.map.android.fragments.RemoteHelmDataFragment;
import com.map.android.utils.prefs.DroidPlannerPrefs;
import com.map.android.client.Drone;
import com.map.android.lib.drone.attribute.AttributeEvent;
import com.map.android.lib.drone.attribute.AttributeEventExtra;
import com.map.android.lib.drone.attribute.AttributeType;
import com.map.android.lib.drone.attribute.error.ErrorType;
import com.map.android.lib.drone.property.Altitude;
import com.map.android.lib.drone.property.Battery;
import com.map.android.lib.drone.property.Gps;
import com.map.android.lib.drone.property.Signal;
import com.map.android.lib.drone.property.Speed;
import com.map.android.lib.drone.property.State;
import com.map.android.lib.drone.property.VehicleMode;

import com.map.android.R;
import com.map.android.fragments.SettingsFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements DroidPlanner audible notifications.
 */
public class TTSNotificationProvider implements OnInitListener,
        NotificationHandler.NotificationProvider {

    private static final String CLAZZ_NAME = TTSNotificationProvider.class.getName();
    private static final String TAG = TTSNotificationProvider.class.getSimpleName();

    private static final long WARNING_DELAY = 1500l; //ms

    private static final double BATTERY_DISCHARGE_NOTIFICATION_EVERY_PERCENT = 10;

    /**
     * Utterance id for the periodic status speech.
     */
    private static final String PERIODIC_STATUS_UTTERANCE_ID = "periodic_status_utterance";

    /**
     * Action used for message to be delivered by the tts speech engine.
     */
    public static final String ACTION_SPEAK_MESSAGE = CLAZZ_NAME + ".ACTION_SPEAK_MESSAGE";
    public static final String EXTRA_MESSAGE_TO_SPEAK = "extra_message_to_speak";

    private final static IntentFilter eventFilter = new IntentFilter();

    static {
        eventFilter.addAction(AttributeEvent.STATE_ARMING);
        eventFilter.addAction(AttributeEvent.BATTERY_UPDATED);
        eventFilter.addAction(AttributeEvent.STATE_VEHICLE_MODE);
        eventFilter.addAction(AttributeEvent.MISSION_SENT);
        eventFilter.addAction(AttributeEvent.GPS_FIX);
        eventFilter.addAction(AttributeEvent.MISSION_RECEIVED);
        eventFilter.addAction(AttributeEvent.HEARTBEAT_FIRST);
        eventFilter.addAction(AttributeEvent.HEARTBEAT_TIMEOUT);
        eventFilter.addAction(AttributeEvent.HEARTBEAT_RESTORED);
        eventFilter.addAction(AttributeEvent.MISSION_ITEM_UPDATED);
        eventFilter.addAction(AttributeEvent.FOLLOW_START);
        eventFilter.addAction(AttributeEvent.AUTOPILOT_ERROR);
        eventFilter.addAction(AttributeEvent.ALTITUDE_UPDATED);
        eventFilter.addAction(AttributeEvent.SIGNAL_WEAK);
        eventFilter.addAction(AttributeEvent.WARNING_NO_GPS);
        eventFilter.addAction(AttributeEvent.HOME_UPDATED);
    }

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (tts == null)
                return;

            final String action = intent.getAction();
            State droneState = drone.getAttribute(AttributeType.STATE);

            switch (action) {
                case AttributeEvent.STATE_ARMING:
                    if (droneState != null)
                        speakArmedState(droneState.isArmed());
                    break;

                case AttributeEvent.BATTERY_UPDATED:
                    Battery droneBattery = drone.getAttribute(AttributeType.BATTERY);
                    if (droneBattery != null)
                        batteryDischargeNotification(droneBattery.getBatteryRemain());
                    break;

                case AttributeEvent.STATE_VEHICLE_MODE:
                    if (droneState != null)
                        speakMode(droneState.getVehicleMode());
                    break;

                case AttributeEvent.MISSION_SENT:
                    FlightDataFragment  flightDataFragment=  FlightActivity.getFlightDataInstance();
                    if (flightDataFragment!=null)
                        flightDataFragment.dismissUploadDownloadDialog();
                    RemoteHelmDataFragment remoteHelmDataFragment=  RemoteHelmActivity.getInstance();
                    if (remoteHelmDataFragment!=null)
                        remoteHelmDataFragment.dismissUploadDownloadDialog();

                    SettingsFragment settingsFragment=  SettingsActivity.getSettingsFragmentInstance();
                    if (settingsFragment!=null)
                        settingsFragment.dismissUploadDownloadDialog();

                    // DrawerNavigationUI.dismissUploadDownloadDialog();
                    Toast.makeText(context, R.string.toast_mission_sent, Toast.LENGTH_SHORT).show();
                    speak(context.getString(R.string.speak_mission_sent));
                    break;

                case AttributeEvent.GPS_FIX:
                    Gps droneGps = drone.getAttribute(AttributeType.GPS);
                    if (droneGps != null)
                        speakGpsMode(droneGps.getFixType());
                    break;

                case AttributeEvent.MISSION_RECEIVED:
                    FlightDataFragment  flightDataFragment2=  FlightActivity.getFlightDataInstance();
                    if (flightDataFragment2!=null)
                        flightDataFragment2.dismissUploadDownloadDialog();
                    RemoteHelmDataFragment remoteHelmDataFragment2=  RemoteHelmActivity.getInstance();
                    if (remoteHelmDataFragment2!=null)
                        remoteHelmDataFragment2.dismissUploadDownloadDialog();

                    SettingsFragment settingsFragment2=  SettingsActivity.getSettingsFragmentInstance();
                    if (settingsFragment2!=null)
                        settingsFragment2.dismissUploadDownloadDialog();

                    //DrawerNavigationUI.dismissUploadDownloadDialog();
                    Toast.makeText(context, R.string.toast_mission_received, Toast.LENGTH_SHORT).show();
                    speak(context.getString(R.string.speak_mission_received));
                    break;

                case AttributeEvent.HEARTBEAT_FIRST:
                    speak(context.getString(R.string.speak_heartbeat_first));
                    break;

                case AttributeEvent.HEARTBEAT_TIMEOUT:
                    if (mAppPrefs.getWarningOnLostOrRestoredSignal()) {
                        speak(context.getString(R.string.speak_heartbeat_timeout));
                        handler.removeCallbacks(watchdogCallback);
                    }
                    break;

                case AttributeEvent.HEARTBEAT_RESTORED:
                    watchdogCallback.setDrone(drone);
                    scheduleWatchdog();
                    if (mAppPrefs.getWarningOnLostOrRestoredSignal()) {
                        speak(context.getString(R.string.speak_heartbeat_restored));
                    }
                    break;

                case AttributeEvent.MISSION_ITEM_UPDATED:
                    int currentWaypoint = intent.getIntExtra(AttributeEventExtra.EXTRA_MISSION_CURRENT_WAYPOINT, 0);
                    if (currentWaypoint != 0) {
                        //Zeroth waypoint is the home location.
                        speak(context.getString(R.string.speak_mission_item_updated, currentWaypoint));
                    }
                    break;

                case AttributeEvent.FOLLOW_START:
                    speak(context.getString(R.string.speak_follow_start));
                    break;

                case AttributeEvent.ALTITUDE_UPDATED:
                    final Altitude altitude = drone.getAttribute(AttributeType.ALTITUDE);
                    if (mAppPrefs.hasExceededMaxAltitude(altitude.getAltitude())) {
                        if (isMaxAltExceeded.compareAndSet(false, true)) {
                            handler.postDelayed(maxAltitudeExceededWarning, WARNING_DELAY);
                        }
                    } else {
                        handler.removeCallbacks(maxAltitudeExceededWarning);
                        isMaxAltExceeded.set(false);
                    }
                    break;

                case AttributeEvent.AUTOPILOT_ERROR:
                    if (mAppPrefs.getWarningOnAutopilotWarning()) {
                        String errorId = intent.getStringExtra(AttributeEventExtra.EXTRA_AUTOPILOT_ERROR_ID);
                        final ErrorType errorType = ErrorType.getErrorById(errorId);
                        if (errorType != null && errorType != ErrorType.NO_ERROR) {
                            speak(errorType.getLabel(context).toString());
                        }
                    }
                    break;

                case AttributeEvent.SIGNAL_WEAK:
                    if (mAppPrefs.getWarningOnLowSignalStrength()) {
                        speak(context.getString(R.string.speak_warning_signal_weak));
                    }
                    break;

                case AttributeEvent.WARNING_NO_GPS:
                    speak(context.getString(R.string.speak_warning_no_gps));
                    break;

                case AttributeEvent.HOME_UPDATED:
                    if (droneState.isFlying()) {
                        //Warn the user the home location was just updated while in flight.
                        if (mAppPrefs.getWarningOnVehicleHomeUpdate()) {
                            speak(context.getString(R.string.speak_warning_vehicle_home_updated));
                        }
                    }
                    break;
            }
        }
    };

    private final AtomicBoolean mIsPeriodicStatusStarted = new AtomicBoolean(false);
    /**
     * Listens for updates to the status interval.
     */
    private final BroadcastReceiver mSpeechIntervalUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (SettingsFragment.ACTION_UPDATED_STATUS_PERIOD.equals(action)) {
                scheduleWatchdog();
            } else if (ACTION_SPEAK_MESSAGE.equals(action)) {
                String msg = intent.getStringExtra(EXTRA_MESSAGE_TO_SPEAK);
                if (msg != null) {
                    speak(msg);
                }
            }
        }
    };

    /**
     * Monitors speech completion.
     */
    private final TextToSpeech.OnUtteranceCompletedListener mSpeechCompleteListener = new TextToSpeech.OnUtteranceCompletedListener() {
        @Override
        public void onUtteranceCompleted(String utteranceId) {
            if (PERIODIC_STATUS_UTTERANCE_ID.equals(utteranceId)) {
                mIsPeriodicStatusStarted.set(false);
            }
        }
    };

    private final AtomicBoolean isMaxAltExceeded = new AtomicBoolean(false);

    private final Runnable maxAltitudeExceededWarning = new Runnable() {
        @Override
        public void run() {
            speak(context.getString(R.string.speak_warning_max_alt_exceed));
            handler.removeCallbacks(maxAltitudeExceededWarning);
        }
    };

    /**
     * Stored the parameters to be passed to the tts `speak(...)` method.
     */
    private final HashMap<String, String> mTtsParams = new HashMap<String, String>();

    private TextToSpeech tts;
    private int lastBatteryDischargeNotification;

    private final Context context;
    private final DroidPlannerPrefs mAppPrefs;
    private final Handler handler = new Handler();
    private int statusInterval;

    private class Watchdog implements Runnable {

        private final StringBuilder mMessageBuilder = new StringBuilder();
        private Drone drone;

        public void run() {
            handler.removeCallbacks(watchdogCallback);

            if (drone != null) {
                final State droneState = drone.getAttribute(AttributeType.STATE);
                if (droneState.isConnected() && droneState.isArmed())
                    speakPeriodic(drone);
            }

            if (statusInterval != 0) {
                handler.postDelayed(watchdogCallback, statusInterval * 1000);
            }
        }

        // Periodic status preferences
        private void speakPeriodic(Drone drone) {
            // Drop the message if the previous one is not done yet.
            if (mIsPeriodicStatusStarted.compareAndSet(false, true)) {
                final Map<String, Boolean> speechPrefs = mAppPrefs.getPeriodicSpeechPrefs();

                mMessageBuilder.setLength(0);
                if (speechPrefs.get(DroidPlannerPrefs.PREF_TTS_PERIODIC_BAT_VOLT)) {
                    final Battery droneBattery = drone.getAttribute(AttributeType.BATTERY);
                    mMessageBuilder.append(context.getString(R.string.periodic_status_bat_volt,
                            droneBattery.getBatteryVoltage()));
                }

                if (speechPrefs.get(DroidPlannerPrefs.PREF_TTS_PERIODIC_ALT)) {
                    final Altitude altitude = drone.getAttribute(AttributeType.ALTITUDE);
                    mMessageBuilder.append(context.getString(R.string.periodic_status_altitude, (int) (altitude.getAltitude())));
                }

                if (speechPrefs.get(DroidPlannerPrefs.PREF_TTS_PERIODIC_AIRSPEED)) {
                    final Speed droneSpeed = drone.getAttribute(AttributeType.SPEED);
                    mMessageBuilder.append(context.getString(R.string.periodic_status_airspeed, (int) (droneSpeed.getAirSpeed())));
                }

                if (speechPrefs.get(DroidPlannerPrefs.PREF_TTS_PERIODIC_RSSI)) {
                    final Signal signal = drone.getAttribute(AttributeType.SIGNAL);
                    mMessageBuilder.append(context.getString(R.string.periodic_status_rssi, (int) signal.getRssi()));
                }

                speak(mMessageBuilder.toString(), true, PERIODIC_STATUS_UTTERANCE_ID);
            }
        }

        public void setDrone(Drone drone) {
            this.drone = drone;
        }
    }

    public final Watchdog watchdogCallback = new Watchdog();

    private final Drone drone;

    TTSNotificationProvider(Context context, Drone drone) {
        this.context = context;
        this.drone = drone;
        mAppPrefs =  DroidPlannerPrefs.getInstance(context);
    }

    @Override
    public void init() {
        tts = new TextToSpeech(context, this);
        LocalBroadcastManager.getInstance(context).registerReceiver(eventReceiver, eventFilter);
    }

    @Override
    public void onTerminate() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(eventReceiver);

        handler.removeCallbacks(watchdogCallback);
        speak(context.getString(R.string.speak_disconected));

        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
    }

    private void scheduleWatchdog() {
        handler.removeCallbacks(watchdogCallback);
        statusInterval = mAppPrefs.getSpokenStatusInterval();
        if (statusInterval != 0) {
            handler.postDelayed(watchdogCallback, statusInterval * 1000);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onInit(int status) {
        if (tts == null)
            return;

        if (status == TextToSpeech.SUCCESS) {
            // TODO: check if the language is available
            Locale ttsLanguage;
            final int sdkVersion = Build.VERSION.SDK_INT;
            if (sdkVersion >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                ttsLanguage = tts.getDefaultLanguage();
            } else {
                ttsLanguage = tts.getLanguage();
            }

            if (ttsLanguage == null || tts.isLanguageAvailable(ttsLanguage) == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsLanguage = Locale.US;
                if (sdkVersion >= Build.VERSION_CODES.LOLLIPOP) {
                    final Set<Locale> languagesSet = tts.getAvailableLanguages();
                    if (languagesSet != null && !languagesSet.isEmpty()) {
                        final List<Locale> availableLanguages = new ArrayList<>(languagesSet);
                        //Pick the first available language.
                        ttsLanguage = availableLanguages.get(0);
                    }
                }
            }

            if (tts.isLanguageAvailable(ttsLanguage) == TextToSpeech.LANG_MISSING_DATA) {
                context.startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }

            int supportStatus = tts.setLanguage(ttsLanguage);
            switch (supportStatus) {
                case TextToSpeech.LANG_MISSING_DATA:
                case TextToSpeech.LANG_NOT_SUPPORTED:
                    tts.shutdown();
                    tts = null;

                    Log.e(TAG, "TTS Language data is not available.");
                    Toast.makeText(context, R.string.toast_error_tts_lang,
                            Toast.LENGTH_LONG).show();
                    break;
            }

            if (tts != null) {
                tts.setOnUtteranceCompletedListener(mSpeechCompleteListener);

                // Register the broadcast receiver
                final IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ACTION_SPEAK_MESSAGE);
                intentFilter.addAction(SettingsFragment.ACTION_UPDATED_STATUS_PERIOD);

                LocalBroadcastManager.getInstance(context).registerReceiver(
                        mSpeechIntervalUpdateReceiver, intentFilter);

                //Announce the connection event
                watchdogCallback.setDrone(drone);
                scheduleWatchdog();
                speak(context.getString(R.string.speak_connected));
            }
        } else {
            // Notify the user that the tts engine is not available.
            Log.e(TAG, "TextToSpeech initialization failed.");
            Toast.makeText(
                    context,
                    R.string.warn_tts_accessibility, Toast.LENGTH_LONG).show();
        }
    }

    private void speak(String string) {
        speak(string, true, null);
    }

    private void speak(String string, boolean append, String utteranceId) {
        if (tts != null) {
            if (shouldEnableTTS()) {
                final int queueType = append ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH;

                mTtsParams.clear();
                if (utteranceId != null) {
                    mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                }

                tts.speak(string, queueType, mTtsParams);

            }
        }
    }

    private boolean shouldEnableTTS() {
        return mAppPrefs.isTtsEnabled();
    }

    private void speakArmedState(boolean armed) {
        if (armed) {
            speak(context.getString(R.string.speak_armed));
        } else {
            speak(context.getString(R.string.speak_disarmed));
        }
    }

    private void batteryDischargeNotification(double battRemain) {
        if (lastBatteryDischargeNotification > (int) ((battRemain - 1) / BATTERY_DISCHARGE_NOTIFICATION_EVERY_PERCENT)
                || lastBatteryDischargeNotification + 1 < (int) ((battRemain - 1) / BATTERY_DISCHARGE_NOTIFICATION_EVERY_PERCENT)) {
            lastBatteryDischargeNotification = (int) ((battRemain - 1) / BATTERY_DISCHARGE_NOTIFICATION_EVERY_PERCENT);
            speak(context.getString(R.string.speak_battery_notification, (int) battRemain));
        }
    }

    private void speakMode(VehicleMode mode) {
        if (mode == null)
            return;

        String modeString = context.getString(R.string.fly_mode_mode);
        switch (mode) {
            case PLANE_FLY_BY_WIRE_A:
                modeString += context.getString(R.string.fly_mode_wire_a);
                break;
            case PLANE_FLY_BY_WIRE_B:
                modeString += context.getString(R.string.fly_mode_wire_b);
                break;
            case COPTER_ACRO:
                modeString += context.getString(R.string.fly_mode_acro);
                break;
            case COPTER_ALT_HOLD:
                modeString += context.getString(R.string.fly_mode_alt_hold);
                break;
            case COPTER_POSHOLD:
                modeString += context.getString(R.string.fly_mode_pos_hold);
                break;
            case PLANE_RTL:
            case COPTER_RTL:
                modeString += context.getString(R.string.fly_mode_rtl);
                break;
            default:
                modeString += mode.getLabel();
                break;
        }
        speak(modeString);
    }

    private void speakGpsMode(int fix) {
        switch (fix) {
            case 2:
                speak(context.getString(R.string.gps_mode_2d_lock));
                break;
            case 3:
                speak(context.getString(R.string.gps_mode_3d_lock));
                break;
            case 4:
                speak(context.getString(R.string.gps_mode_3d_dgps_lock));
                break;
            case 5:
                speak(context.getString(R.string.gps_mode_3d_rtk_lock));
                break;
            default:
                speak(context.getString(R.string.gps_mode_lost_gps_lock));
                break;
        }
    }
}
