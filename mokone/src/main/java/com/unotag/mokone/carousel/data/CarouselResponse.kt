package com.unotag.mokone.carousel.data

data class CarouselResponse(
    val message: String?,
    val data: List<CarouselItem>?
)

data class CarouselItem(
    val caraousel_id: String?,
    val org_id: String?,
    val created_at: String?,
    val updated_at: String?,
    val ClientId: String?,
    val div_id: String?,
    val caraousel_content: List<CarouselContent>?,
    val div_id_android: DivIdAndroid?,
    val div_id_ios: DivIdIos?
)

data class CarouselContent(
    val id: String?,
    val type: String?,
    val url: String?
)

data class DivIdAndroid(
    val screen_name: String?,
    val identifier: String?
)

data class DivIdIos(
    val screen_name: String?,
    val identifier: String?
)
