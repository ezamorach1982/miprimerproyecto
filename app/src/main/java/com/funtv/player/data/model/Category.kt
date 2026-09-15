package com.funtv.player.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("category_id") val categoryId: String = "",
    @SerializedName("category_name") val categoryName: String = "",
    @SerializedName("parent_id") val parentId: Int? = 0
)
