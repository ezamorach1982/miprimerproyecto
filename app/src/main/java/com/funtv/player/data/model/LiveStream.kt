package com.funtv.player.data.model

import com.google.gson.annotations.SerializedName

data class LiveStream(
    @SerializedName("num") val num: Int? = 0,
    @SerializedName("name") val name: String? = "",
    @SerializedName("stream_type") val streamType: String? = "live",
    @SerializedName("stream_id") val streamId: Int = 0,
    @SerializedName("stream_icon") val streamIcon: String? = "",
    @SerializedName("epg_channel_id") val epgChannelId: String? = "",
    @SerializedName("category_id") val categoryId: String? = "",
    @SerializedName("tv_archive") val tvArchive: Int? = 0
)
