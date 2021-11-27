package com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.model

import com.awareframework.android.core.model.AwareObject
import com.google.gson.Gson

data class ActivityRecognitionData(
    var eventTimestamp: Long = 0L,
    var detectedActivity: String = "",
    var activityTransiton: String = "",

) : AwareObject(jsonVersion = 1){
    companion object {
        const val TABLE_NAME = "ActivityRecognitionData"
    }
    override fun toString(): String = Gson().toJson(this)
}