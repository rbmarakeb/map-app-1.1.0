package com.map.android.proxy.mission.item.fragments;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.map.android.R;
import com.map.android.lib.drone.mission.MissionItemType;
import com.map.android.lib.drone.mission.item.MissionItem;
import com.map.android.lib.drone.mission.item.POIItem;
import com.map.android.lib.drone.mission.item.spatial.Waypoint;
import com.map.android.utils.unit.providers.length.LengthUnitProvider;
import com.map.android.view.spinnerWheel.CardWheelHorizontalView;
import com.map.android.view.spinnerWheel.adapters.LengthWheelAdapter;
import com.map.android.view.spinnerWheel.adapters.NumericWheelAdapter;

import org.beyene.sius.unit.length.LengthUnit;

public class MissionPOIFragment extends MissionDetailFragment {

    private POIItem item;

    @Override
    protected int getResource() {
        return R.layout.fragment_editor_detail_poi;
    }

    @Override
    public void onApiConnected() {
//        super.onApiConnected();

        final View view = getView();
        final Context context = getContext();

//       typeSpinner.setSelection(commandAdapter.getPosition(MissionItemType.WAYPOINT));

        Button ConfirmButton = (Button)view.findViewById(R.id.btn_confirm);
        Button CancelButton = (Button)view.findViewById(R.id.btn_cancel);

        final EditText etPOIName   = (EditText)view.findViewById(R.id.et_poi_name);

        final EditText latedit   = (EditText)view.findViewById(R.id.et_latitude_ns);
        final EditText latdeg  = (EditText)view.findViewById(R.id.LatDegPic);
        final EditText latmin   = (EditText)view.findViewById(R.id.LatMinPic);
        final EditText latsec   = (EditText)view.findViewById(R.id.LatSecPic);
        final TextView LatTXT = (TextView)view.findViewById(R.id.LatText);

        final EditText longedit   = (EditText)view.findViewById(R.id.et_longitude_ew);
        final EditText longdeg  = (EditText)view.findViewById(R.id.LongDegPic);
        final EditText longmin   = (EditText)view.findViewById(R.id.LongMinPic);
        final EditText longsec   = (EditText)view.findViewById(R.id.LongSecPic);

        etPOIName.setText(item.getName());
//        for (MissionItem item : getMissionItems()) {
            double LatDum=item.getLatitude();
            double LongDum=item.getLongitude();
            double LatDegD=Math.floor(Math.abs(LatDum));
            double LongDegD=Math.floor(Math.abs(LongDum));
            // editText.setText("Google is your friend.", TextView.BufferType.EDITABLE);
            latdeg.setText(String.valueOf(Math.abs(LatDegD)), TextView.BufferType.EDITABLE);
            longdeg.setText(String.valueOf(Math.abs(LongDegD)), TextView.BufferType.EDITABLE);

            latmin.setText(String.valueOf((Math.abs(LatDum)-LatDegD)*60), TextView.BufferType.EDITABLE);
            longmin.setText(String.valueOf((Math.abs(LongDum)-LongDegD)*60), TextView.BufferType.EDITABLE);

            if (LatDum<0){latedit.setText("S", TextView.BufferType.EDITABLE);}
            else {latedit.setText("N", TextView.BufferType.EDITABLE);}
            if (LongDum<0){longedit.setText("W", TextView.BufferType.EDITABLE);}
            else {longedit.setText("E", TextView.BufferType.EDITABLE);}
//        }

        ConfirmButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
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
                                double longitudenew = EW * (longdegress + longminutes / 60.0 + longseconds / 3600.0);

                                double latdegress = Double.parseDouble(lld);
                                double latminutes = Double.parseDouble(llm);
                                double latseconds = Double.parseDouble(lls);
                                double latidnew = NS * (latdegress + latminutes / 60.0 + latseconds / 3600.0);

                                String name = etPOIName.getText().toString();
                                POIItem newOne = new POIItem(name, latidnew, longitudenew);
                                //double latidnew = Double.parseDouble(latedit.getText().toString());

//                                for (MissionItem item : getMissionItems()) {
//                                    ((Waypoint) item).getCoordinate().setLatitude(latidnew);
//                                }
//                                getMissionProxy().notifyMissionUpdate();
//                                for (MissionItem item : getMissionItems()) {
//                                    ((Waypoint) item).getCoordinate().setLongitude(longitudenew);
//                                }
                                getMissionProxy().notifyPOIUpdate(item, newOne);
                                LatTXT.setText("POI informations updated ");
                                latdeg.setHint("Deg");
                                longdeg.setHint("Deg");

                                latmin.setHint("Min");
                                longmin.setHint("Min");
                                latedit.setHint("N/S");
                                longedit.setHint("E/W");
                            } catch (Exception e) {
                                // This will catch any exception, because they are all descended from Exception
                                LatTXT.setText("Error: Incorrect Format");
                            }
                        }
                        else {
                            LatTXT.setText("Error: Please ensure that all fields are entered");
                        }
                        dismiss();
                    }
                });

        CancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getMissionProxy().removePOIItem(item);
                dismiss();
            }
        });
    }

    public void setPOIItem(POIItem item) {
        this.item = item;
    }

    public POIItem getItem() {
        return item;
    }
}
