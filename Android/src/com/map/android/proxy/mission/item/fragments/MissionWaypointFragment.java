package com.map.android.proxy.mission.item.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.map.android.proxy.mission.item.adapters.SelectFormatAdapter;
import com.map.android.utils.prefs.DroidPlannerPrefs;
import com.map.android.view.spinnerWheel.adapters.LengthWheelAdapter;
import com.map.android.view.spinnerWheel.adapters.NumericWheelAdapter;
import com.map.android.lib.drone.mission.MissionItemType;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.mission.item.spatial.Waypoint;

import org.beyene.sius.unit.length.LengthUnit;
import com.map.android.R;
import com.map.android.utils.unit.providers.length.LengthUnitProvider;
import com.map.android.view.spinnerWheel.CardWheelHorizontalView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class MissionWaypointFragment extends MissionDetailFragment implements
       CardWheelHorizontalView.OnCardWheelScrollListener
{
    private static final String KEY_SELECTED_FORMAT = "com.map.android.KEY_SELECTED_FORMAT";
    private static final String SELECTED_FORMAT = "com.map.android.SELECTED_FORMAT";
    private static final String ITEM_DETAIL_TAG = "Item Detail Window";
    MissionDetailFragment   itemDetailFragment;
    FragmentManager fragmentManager;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragmentManager = getFragmentManager();
        itemDetailFragment = (MissionDetailFragment) fragmentManager.findFragmentByTag(ITEM_DETAIL_TAG);

    }

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_waypoint;
    }

    @Override
    public void onApiConnected() {
        super.onApiConnected();
        DecimalFormatSymbols symbolsEN_US = DecimalFormatSymbols.getInstance(Locale.US);
       // NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
       // final DecimalFormat df_deg = (DecimalFormat) nf;
        final DecimalFormat df_deg = new DecimalFormat("#",symbolsEN_US);
        df_deg.setRoundingMode(RoundingMode.HALF_UP);

        final DecimalFormat df_min = new DecimalFormat("#.###",symbolsEN_US);
        df_min.setRoundingMode(RoundingMode.HALF_UP);

        final DecimalFormat df_sec = new DecimalFormat("#.###",symbolsEN_US);
        df_sec.setRoundingMode(RoundingMode.HALF_UP);

        final DecimalFormat df_Dd = new DecimalFormat("#.#####",symbolsEN_US);
        df_Dd.setRoundingMode(RoundingMode.HALF_UP);

        final View view = getView();
        final Context context = getContext();

       typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.WAYPOINT));

         NumericWheelAdapter delayAdapter = new NumericWheelAdapter(context, R.layout.wheel_text_centered, -1, 1, "%d");

         Button ConfirmButton = (Button)view.findViewById(R.id.newcoordconfirm);
         Button CancelButton = (Button)view.findViewById(R.id.newcoordcancel);
        ImageView menuSelectFormat = (ImageView)view.findViewById(R.id.menuSelectFormat);

        final EditText longedit   = (EditText)view.findViewById(R.id.altitudePickera);
        final EditText longdeg  = (EditText)view.findViewById(R.id.LongDegPic);
        final EditText longmin   = (EditText)view.findViewById(R.id.LongMinPic);
        final EditText longsec   = (EditText)view.findViewById(R.id.LongSecPic);



         final EditText latedit   = (EditText)view.findViewById(R.id.waypointDelayPickera);
        final EditText latdeg  = (EditText)view.findViewById(R.id.LatDegPic);
        final EditText latmin   = (EditText)view.findViewById(R.id.LatMinPic);
        final EditText latsec   = (EditText)view.findViewById(R.id.LatSecPic);
        final TextView LatTXT = (TextView)view.findViewById(R.id.LatText);

        final int position=readSharedPreference(KEY_SELECTED_FORMAT,SELECTED_FORMAT);
        switch (position){
            case 0:
                longmin.setVisibility(View.VISIBLE);
                longsec.setVisibility(View.VISIBLE);
                latmin.setVisibility(View.VISIBLE);
                latsec.setVisibility(View.VISIBLE);
                break;
            case -1:
            case 1:
                longmin.setVisibility(View.VISIBLE);
                longsec.setVisibility(View.GONE);
                latmin.setVisibility(View.VISIBLE);
                latsec.setVisibility(View.GONE);
                break;
            case 2:
                longmin.setVisibility(View.GONE);
                longsec.setVisibility(View.GONE);
                latmin.setVisibility(View.GONE);
                latsec.setVisibility(View.GONE);
                break;

        }

        for (MissionItem item : getMissionItems()) {
            double LatDum=((Waypoint) item).getCoordinate().getLatitude();
            double LongDum=((Waypoint) item).getCoordinate().getLongitude();
            double LatDegD=Math.floor(Math.abs(LatDum));
            double LongDegD=Math.floor(Math.abs(LongDum));
            double LatMinD=Math.floor((Math.abs(LatDum)-LatDegD)*60);
            double LongMinD=Math.floor((Math.abs(LongDum)-LongDegD)*60);
            double LatSecD=(((Math.abs(LatDum)-LatDegD)*60)-LatMinD)*60;
            double LongSecD=(((Math.abs(LongDum)-LongDegD)*60)-LongMinD)*60;
           // editText.setText("Google is your friend.", TextView.BufferType.EDITABLE);
            switch (position){
                case 0:
                    latdeg.setText(df_deg.format(Math.abs(LatDegD)), TextView.BufferType.EDITABLE);
                    longdeg.setText(df_deg.format(Math.abs(LongDegD)), TextView.BufferType.EDITABLE);

                    latmin.setText(df_deg.format(LatMinD), TextView.BufferType.EDITABLE);
                    longmin.setText(df_deg.format(LongMinD), TextView.BufferType.EDITABLE);

                    latsec.setText(df_sec.format(LatSecD), TextView.BufferType.EDITABLE);
                    longsec.setText(df_sec.format(LongSecD), TextView.BufferType.EDITABLE);
                    break;
                case -1:
                case 1:
                    latdeg.setText(df_deg.format(Math.abs(LatDegD)), TextView.BufferType.EDITABLE);
                    longdeg.setText(df_deg.format(Math.abs(LongDegD)), TextView.BufferType.EDITABLE);

                    latmin.setText(df_min.format((Math.abs(LatDum)-LatDegD)*60), TextView.BufferType.EDITABLE);
                    longmin.setText(df_min.format((Math.abs(LongDum)-LongDegD)*60), TextView.BufferType.EDITABLE);
                    break;
                case 2:
                    double xx=ConvertDMSsToDecimal(LatDegD,(Math.abs(LatDum)-LatDegD)*60,0);
                    double x=12.12345;
                    latdeg.setText(df_Dd.format(ConvertDMSsToDecimal(LatDegD,(Math.abs(LatDum)-LatDegD)*60,0)), TextView.BufferType.EDITABLE);
                    longdeg.setText(df_Dd.format(ConvertDMSsToDecimal(LongDegD,(Math.abs(LongDum)-LongDegD)*60,0)), TextView.BufferType.EDITABLE);
                    break;
            }


            if (LatDum<0){latedit.setText("S", TextView.BufferType.EDITABLE);}
            else {latedit.setText("N", TextView.BufferType.EDITABLE);}
            if (LongDum<0){longedit.setText("W", TextView.BufferType.EDITABLE);}
            else {longedit.setText("E", TextView.BufferType.EDITABLE);}
        }
        CancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragmentManager.beginTransaction().remove(itemDetailFragment).commit();
                getMissionProxy().notifyMissionUpdate();
            }
        });
        final String[] items = new String[] {"D M Ss", "D Mm", "Dd"};
        final int[] previousposition = {position};
        menuSelectFormat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(context);
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_select_format);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                ListView item_list = (ListView) dialog.findViewById(R.id.item_list);
                int position=readSharedPreference(KEY_SELECTED_FORMAT,SELECTED_FORMAT);


                if(position==-1)
                    item_list.setAdapter(new SelectFormatAdapter(context, items,1));
                else
                    item_list.setAdapter(new SelectFormatAdapter(context, items,position));

                item_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        writeSharedPreference(position,KEY_SELECTED_FORMAT,SELECTED_FORMAT);
//                        Toast.makeText(context,
//                                "Click ListItem Number " + position, Toast.LENGTH_LONG)
//                                .show();
                        switch (position){
                            case 0:
                                longmin.setVisibility(View.VISIBLE);
                                longsec.setVisibility(View.VISIBLE);
                                latmin.setVisibility(View.VISIBLE);
                                latsec.setVisibility(View.VISIBLE);
                                if(previousposition[0] ==1)
                                {
                                    String ld= (longmin.getText().toString());
                                    String lld= (latmin.getText().toString());
                                    double LatDum=Double.parseDouble(lld);
                                    double LongDum=Double.parseDouble(ld);

                                    double LatMinD=Math.floor(LatDum);
                                    double LongMinD=Math.floor(LongDum);


                                    double LatSecD=(LatDum-LatMinD)*60;
                                    double LongSecD=(LongDum-LongMinD)*60;

                                    latmin.setText(df_deg.format(LatMinD), TextView.BufferType.EDITABLE);
                                    longmin.setText(df_deg.format(LongMinD), TextView.BufferType.EDITABLE);

                                    latsec.setText(df_sec.format(LatSecD), TextView.BufferType.EDITABLE);
                                    longsec.setText(df_sec.format(LongSecD), TextView.BufferType.EDITABLE);

                                }
                                else if(previousposition[0] ==2)
                                {
                                    String ld= (longdeg.getText().toString());
                                    String lld= (latdeg.getText().toString());

                                    double LatDum=Double.parseDouble(lld);
                                    double LongDum=Double.parseDouble(ld);
                                    double LatDegD=Math.floor(Math.abs(LatDum));
                                    double LongDegD=Math.floor(Math.abs(LongDum));
                                    double LatMinD=Math.floor((Math.abs(LatDum)-LatDegD)*60);
                                    double LongMinD=Math.floor((Math.abs(LongDum)-LongDegD)*60);
                                    double LatSecD=(((Math.abs(LatDum)-LatDegD)*60)-LatMinD)*60;
                                    double LongSecD=(((Math.abs(LongDum)-LongDegD)*60)-LongMinD)*60;

                                    latdeg.setText(df_deg.format(Math.abs(LatDegD)), TextView.BufferType.EDITABLE);
                                    longdeg.setText(df_deg.format(Math.abs(LongDegD)), TextView.BufferType.EDITABLE);


                                    latmin.setText(df_deg.format(LatMinD), TextView.BufferType.EDITABLE);
                                    longmin.setText(df_deg.format(LongMinD), TextView.BufferType.EDITABLE);

                                    latsec.setText(df_sec.format(LatSecD), TextView.BufferType.EDITABLE);
                                    longsec.setText(df_sec.format(LongSecD), TextView.BufferType.EDITABLE);
                                }

                                break;
                            case -1:
                            case 1:
                                longmin.setVisibility(View.VISIBLE);
                                longsec.setVisibility(View.GONE);
                                latmin.setVisibility(View.VISIBLE);
                                latsec.setVisibility(View.GONE);
                                if(previousposition[0] ==0)
                                {

                                    String lm = (longmin.getText().toString());
                                    String ls = (longsec.getText().toString());
                                    String llm = (latmin.getText().toString());
                                    String lls = (latsec.getText().toString());

                                    if (!(lm.matches("[0-9.]+"))) {
                                        lm = "0";
                                    }
                                    if (!(ls.matches("[0-9.]+"))) {
                                        ls = "0";
                                    }
                                    if (!(llm.matches("[0-9.]+"))) {
                                        llm = "0";
                                    }
                                    if (!(lls.matches("[0-9.]+"))) {
                                        lls = "0";
                                    }
                                    double longminutes = Double.parseDouble(lm);
                                    double longseconds = Double.parseDouble(ls);
                                    double latminutes = Double.parseDouble(llm);
                                    double latseconds = Double.parseDouble(lls);

                                    double latminutes_new = latminutes + (latseconds / 60);
                                    double longminutes_new = longminutes + (longseconds / 60);

                                    latmin.setText(df_min.format(latminutes_new), TextView.BufferType.EDITABLE);
                                    longmin.setText(df_min.format(longminutes_new), TextView.BufferType.EDITABLE);

                                    latsec.setText("0");
                                    longsec.setText("0");
                                }
                                if(previousposition[0] ==2)
                                {
                                    String ld= (longdeg.getText().toString());
                                    String lld= (latdeg.getText().toString());

                                    double LatDum=Double.parseDouble(lld);
                                    double LongDum=Double.parseDouble(ld);
                                    double LatDegD=Math.floor(Math.abs(LatDum));
                                    double LongDegD=Math.floor(Math.abs(LongDum));

                                    latdeg.setText(df_deg.format(Math.abs(LatDegD)), TextView.BufferType.EDITABLE);
                                    longdeg.setText(df_deg.format(Math.abs(LongDegD)), TextView.BufferType.EDITABLE);

                                    latmin.setText(df_min.format((Math.abs(LatDum)-LatDegD)*60), TextView.BufferType.EDITABLE);
                                    longmin.setText(df_min.format((Math.abs(LongDum)-LongDegD)*60), TextView.BufferType.EDITABLE);
                                }
                                break;
                            case 2:
                                longmin.setVisibility(View.GONE);
                                longsec.setVisibility(View.GONE);
                                latmin.setVisibility(View.GONE);
                                latsec.setVisibility(View.GONE);
                                if(previousposition[0] !=2) {
                                    String ld = (longdeg.getText().toString());
                                    String lm = (longmin.getText().toString());
                                    String ls = (longsec.getText().toString());
                                    String lld = (latdeg.getText().toString());
                                    String llm = (latmin.getText().toString());
                                    String lls = (latsec.getText().toString());
                                    if (!(ld.matches("[0-9.]+"))) {
                                        ld = "0";
                                    }
                                    if (!(lm.matches("[0-9.]+"))) {
                                        lm = "0";
                                    }
                                    if (!(ls.matches("[0-9.]+"))) {
                                        ls = "0";
                                    }
                                    if (!(lld.matches("[0-9.]+"))) {
                                        lld = "0";
                                    }
                                    if (!(llm.matches("[0-9.]+"))) {
                                        llm = "0";
                                    }
                                    if (!(lls.matches("[0-9.]+"))) {
                                        lls = "0";
                                    }
                                    double longdegress = Double.parseDouble(ld);
                                    double longminutes = Double.parseDouble(lm);
                                    double longseconds = Double.parseDouble(ls);
                                    double latdegress = Double.parseDouble(lld);
                                    double latminutes = Double.parseDouble(llm);
                                    double latseconds = Double.parseDouble(lls);

                                    latdeg.setText(df_Dd.format(ConvertDMSsToDecimal(latdegress, latminutes, latseconds)), TextView.BufferType.EDITABLE);
                                    longdeg.setText(df_Dd.format(ConvertDMSsToDecimal(longdegress, longminutes, longseconds)), TextView.BufferType.EDITABLE);
                                }
                                break;
                        }
                        dialog.dismiss();
                        previousposition[0] =position;
                    }
                });
                dialog.show();
            }
        });
        ConfirmButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        final int position=readSharedPreference(KEY_SELECTED_FORMAT,SELECTED_FORMAT);

                        String ld= (longdeg.getText().toString());
                        String lm= (longmin.getText().toString());
                        String ls= (longsec.getText().toString());
                        String lld= (latdeg.getText().toString());
                        String llm= (latmin.getText().toString());
                        String lls= (latsec.getText().toString());
                        String ns=latedit.getText().toString();
                        String ew= longedit.getText().toString();

                        if ( !(ld.matches("[0-9.]+")) )
                        {ld="0";
                          }
                        if( !(lm.matches("[0-9.]+")) )
                        {lm="0";}
                        if( !(ls.matches("[0-9.]+")) )
                        {ls="0";}
                        if( !(lld.matches("[0-9.]+")) )
                        {lld="0";}
                        if( !(llm.matches("[0-9.]+")) )
                        {llm="0";}
                        if( !(lls.matches("[0-9.]+")) )
                        {lls="0";}
                        if ( ld.matches("[0-9.]+") && lm.matches("[0-9.]+") && ls.matches("[0-9.]+") && lld.matches("[0-9.]+") && llm.matches("[0-9.]+") && lls.matches("[0-9.]+"))
                        {
                            double EW = 1;
                            double NS = 1;
                            if (ew.contains("E") || ew.contains("e")) {
                                EW = 1;
                            } else if (ew.contains("W")|| ew.contains("w")) {
                                EW = -1;
                            } else {
                                EW = 1;
                            }
                            if (ns.contains("N") || ns.contains("n")) {
                                NS = 1;
                            } else if (ns.contains("S") || ns.contains("s")) {
                                NS = -1;
                            } else {
                                NS = 1;
                            }
                            try {

                                double longdegress = Double.parseDouble(ld);
                                double longminutes = Double.parseDouble(lm);
                                double longseconds = Double.parseDouble(ls);
                                double longitudenew=0;
                                switch (position) {
                                    case 0:
                                        longitudenew = EW * (longdegress + longminutes / 60.0 + longseconds / 3600.0);
                                        break;
                                    case -1:
                                    case 1:
                                        longitudenew = EW * (longdegress + longminutes / 60.0 + longseconds / 3600.0);
                                        break;
                                    case 2:
                                        longitudenew = EW * (longdegress);
                                        break;
                                }

                                double latdegress = Double.parseDouble(lld);
                                double latminutes = Double.parseDouble(llm);
                                double latseconds = Double.parseDouble(lls);
                                double latidnew = 0;
                                switch (position) {
                                    case 0:
                                        latidnew = NS * (latdegress + latminutes / 60.0 + latseconds / 3600.0);
                                        break;
                                    case -1:
                                    case 1:
                                        latidnew = NS * (latdegress + latminutes / 60.0 + latseconds / 3600.0);
                                        break;
                                    case 2:
                                        latidnew = NS * (latdegress);
                                        break;
                                }


                                //double latidnew = Double.parseDouble(latedit.getText().toString());

                                for (MissionItem item : getMissionItems()) {
                                    ((Waypoint) item).getCoordinate().setLatitude(latidnew);
                                }
                                getMissionProxy().notifyMissionUpdate();
                                for (MissionItem item : getMissionItems()) {
                                    ((Waypoint) item).getCoordinate().setLongitude(longitudenew);
                                }
                                getMissionProxy().notifyMissionUpdate();
                                LatTXT.setText("Waypoint coordinates updated ");
                                latdeg.setHint("Deg");
                                longdeg.setHint("Deg");

                                latmin.setHint("Min");
                                longmin.setHint("Min");
                                latedit.setHint("N/S");
                                longedit.setHint("E/W");
                                fragmentManager.beginTransaction().remove(itemDetailFragment).commit();
                            }

                         catch (Exception e) {
                        // This will catch any exception, because they are all descended from Exception
                             LatTXT.setText("Error: Incorrect Format");

                    }
                        }
                        else
                        {LatTXT.setText("Error: Please ensure that all fields are entered");}
                    }
                });
        CardWheelHorizontalView<Integer> delayPicker = (CardWheelHorizontalView) view.findViewById(R.id
                .waypointDelayPicker);




        delayPicker.setViewAdapter(delayAdapter);
        delayPicker.addScrollListener(this);






        final LengthUnitProvider lengthUP = getLengthUnitProvider();
        final LengthWheelAdapter altitudeAdapter = new LengthWheelAdapter(context, R.layout.wheel_text_centered,
                lengthUP.boxBaseValueToTarget(MIN_ALTITUDE), lengthUP.boxBaseValueToTarget(MAX_ALTITUDE));
        CardWheelHorizontalView<LengthUnit> altitudePicker = (CardWheelHorizontalView) view.findViewById(R.id
                .altitudePicker);
        altitudePicker.setViewAdapter(altitudeAdapter);
        altitudePicker.addScrollListener(this);

        final Waypoint item = (Waypoint) getMissionItems().get(0);
        delayPicker.setCurrentValue((int) item.getDelay());
        altitudePicker.setCurrentValue(lengthUP.boxBaseValueToTarget(item.getCoordinate().getAltitude()));

    }
    public double ConvertDMSsToDecimal(double Degree,double Minute,double Second )
    {
    return  Degree/1.0 + Minute/60.0 + Second/3600.0;
    }

    @Override
    public void onScrollingStarted(CardWheelHorizontalView cardWheel, Object startValue) {

    }

    @Override
    public void onScrollingUpdate(CardWheelHorizontalView cardWheel, Object oldValue, Object newValue) {

    }

    @Override
    public void onScrollingEnded(CardWheelHorizontalView wheel, Object startValue, Object endValue) {
        switch (wheel.getId()) {
            case R.id.altitudePicker:
                //final double altitude = ((LengthUnit) endValue).toBase().getValue();
                //for (MissionItem item : getMissionItems()) {
                //    ((Waypoint) item).getCoordinate().setAltitude(altitude);
                //}
                //getMissionProxy().notifyMissionUpdate();
                break;

            case R.id.waypointDelayPicker:
                //final int delay = (Integer) endValue;
                //for (MissionItem item : getMissionItems()) {
                //    ((Waypoint) item).setDelay(delay);
                //}
                //getMissionProxy().notifyMissionUpdate();
                break;

        }

    }
    public int  readSharedPreference(String key,String s )
    {
        SharedPreferences sharedPref =getContext().getSharedPreferences(key,MODE_PRIVATE);
        //-1 is default_value if no vaule
        int  savedSetting = sharedPref.getInt(s,-1);

        return savedSetting;
    }
    public  void  writeSharedPreference(int savedSetting,String key,String s )
    {
        SharedPreferences sharedPref =getContext().getSharedPreferences(key,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(s, savedSetting);
        editor.commit();
    }

}
