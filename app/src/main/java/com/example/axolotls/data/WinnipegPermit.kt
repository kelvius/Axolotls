package com.example.axolotls.data

import com.google.gson.annotations.SerializedName

data class WinnipegPermit(
    @SerializedName("permit_number") val permitNumber: String? = null,
    @SerializedName("permit_group") val permitGroup: String? = null,
    @SerializedName("permit_type") val permitType: String? = null,
    @SerializedName("sub_type") val subType: String? = null,
    @SerializedName("work_type") val workType: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("neighbourhood_name") val neighbourhoodName: String? = null,
    @SerializedName("community") val community: String? = null,
    @SerializedName("ward") val ward: String? = null,
    @SerializedName("issue_date") val issueDate: String? = null,
    @SerializedName("application_received_date") val applicationReceivedDate: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("applicant_business_name") val applicantBusinessName: String? = null,
    @SerializedName("major_project") val majorProject: String? = null,
    @SerializedName("economic_development_category") val economicCategory: String? = null,
    @SerializedName("type_of_structure") val typeOfStructure: String? = null,
    @SerializedName("point") val point: GeoPoint? = null
)

data class GeoPoint(
    @SerializedName("type") val type: String? = null,
    @SerializedName("coordinates") val coordinates: List<Double>? = null
) {
    /** Longitude is first in GeoJSON */
    val longitude: Double? get() = coordinates?.getOrNull(0)
    val latitude: Double? get() = coordinates?.getOrNull(1)
}
