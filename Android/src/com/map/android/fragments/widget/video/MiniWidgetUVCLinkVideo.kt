package com.map.android.fragments.widget



import android.os.Bundle
import android.view.*
import android.widget.Toast
import com.map.android.R
import com.map.android.fragments.widget.video.BaseUVCVideoWidget
import kotlin.properties.Delegates


public class MiniWidgetUVCLinkVideo : BaseUVCVideoWidget() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_mini_widget_uvc_video, container, false)
        aspectRatio = ASPECT_RATIO_16_9
        adjustAspectRatio(textureView as TextureView)
    }

}