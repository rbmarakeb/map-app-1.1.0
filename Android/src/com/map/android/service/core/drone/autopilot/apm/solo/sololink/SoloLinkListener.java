package com.map.android.service.core.drone.autopilot.apm.solo.sololink;

import com.map.android.service.core.drone.autopilot.apm.solo.AbstractLinkManager;
import com.map.android.lib.drone.companion.solo.tlv.SoloButtonSetting;
import com.map.android.lib.drone.companion.solo.tlv.TLVPacket;

/**
 * Created by Fredia Huya-Kouadio on 7/10/15.
 */
public interface SoloLinkListener extends AbstractLinkManager.LinkListener {

    void onTlvPacketReceived(TLVPacket packet);

    void onPresetButtonLoaded(int buttonType, SoloButtonSetting buttonSettings);
}
