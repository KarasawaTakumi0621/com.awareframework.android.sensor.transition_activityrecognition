package com.tappun.android.sensor.transition_activityrecognition_exampleapp

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.webkit.PermissionRequest
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.ActivityRecognitionSensor
import com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.model.ActivityRecognitionData
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, "android.permission.ACTIVITY_RECOGNITION")){
            val permissions = arrayOf("android.permission.ACTIVITY_RECOGNITION")
            ActivityCompat.requestPermissions(this, permissions, 1)
        }


        ActivityRecognitionSensor.start(applicationContext, ActivityRecognitionSensor.Config().apply {
            sensorObserver = object: ActivityRecognitionSensor.Observer{
                override fun onDataChanged(data: ActivityRecognitionData) {
                    print("pipipi")
                }
            }
        })
    }
}