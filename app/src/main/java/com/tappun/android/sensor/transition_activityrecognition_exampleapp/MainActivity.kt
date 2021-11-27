package com.tappun.android.sensor.transition_activityrecognition_exampleapp

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.ActivityRecognitionSensor
import com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.model.ActivityRecognitionData

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityRecognitionSensor.start(applicationContext, ActivityRecognitionSensor.Config().apply {
            sensorObserver = object: ActivityRecognitionSensor.Observer{
                override fun onDataChanged(data: ActivityRecognitionData) {
                    print("pipipi")
                }
            }
        })
    }
}