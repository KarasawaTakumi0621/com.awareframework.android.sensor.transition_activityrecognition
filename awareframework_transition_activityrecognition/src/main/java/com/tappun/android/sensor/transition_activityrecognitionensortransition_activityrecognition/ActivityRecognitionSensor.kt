package com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.transition.Transition
import com.awareframework.android.core.AwareSensor
import com.awareframework.android.core.model.SensorConfig
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnSuccessListener
import com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.model.ActivityRecognitionData
import java.util.*
import kotlin.collections.ArrayList

//import com.tappun.android.sensor.transition_activityrecognition_exampleapp.MainActivity



class ActivityRecognitionSensor: AwareSensor() {

    companion object {
        const val TAG = "AWARE::ActRec"

        const val ACTION_AWARE_ACTIVITYRECOGNITION = "ACTION_AWARE_ACTIVITYRECOGNITION"

        const val ACTION_AWARE_ACTIVITYRECOGNITION_START = "com.tappun.android.sensor.transition_activityrecognition.SENSOR_START"
        const val ACTION_AWARE_ACTIVITYRECOGNITION_STOP = "com.tappun.android.sensor.transition_activityrecognition.SENSOR_STOP"
        const val ACTION_AWARE_ACTIVITYRECOGNITION_SET_LABEL = "com.tappun.android.sensor.transition_activityrecognition.SET_LABEL"
        const val ACTION_AWARE_ACTIVITYRECOGNITION_SAVE = "com.tappun.android.sensor.transition_activityrecognition.SAVE"
        const val EXTRA_LABEL = "label"

        const val ACTION_AWARE_ACTIVITYRECOGNITION_SYNC = "com.tappun.android.sensor.transition_activityrecognition.SENSOR_SYNC"

        const val INTENT_ACTION = "com.tappun.android.sensor.transition_activityrecognition.ACTION_PROCESS_ACTIVITY_TRANSITIONS"

        val CONFIG = Config()

        @RequiresApi(Build.VERSION_CODES.O)
        fun start(context: Context, config: Config? = null) {
            println("called fun start")
            if (config != null)
                CONFIG.replaceWith(config)

            context.startService(Intent(context, ActivityRecognitionSensor::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ActivityRecognitionSensor::class.java))
        }

    }

    lateinit var mPendingIntent: PendingIntent

    private val receiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                ACTION_AWARE_ACTIVITYRECOGNITION_SET_LABEL -> {
                    intent.getStringExtra(EXTRA_LABEL)?.let {
                        CONFIG.label = it
                    }
                }

                ACTION_AWARE_ACTIVITYRECOGNITION_SYNC -> onSync(intent)
            }
        }
    }

    val arReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_AWARE_ACTIVITYRECOGNITION_SAVE -> {
                    val data:ActivityRecognitionData? = intent?.getSerializableExtra("ardata") as? ActivityRecognitionData
                    if(data != null){
                        dbEngine?.save(data, ActivityRecognitionData.TABLE_NAME)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    interface Observer {
        fun onDataChanged(data: ActivityRecognitionData){
            
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeDbEngine(CONFIG)

        registerReceiver(receiver, IntentFilter().apply {
            addAction(ACTION_AWARE_ACTIVITYRECOGNITION_SET_LABEL)
            addAction(ACTION_AWARE_ACTIVITYRECOGNITION_SYNC)
        })

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(arReceiver, IntentFilter(ACTION_AWARE_ACTIVITYRECOGNITION_SAVE))
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        println("called on start command")

        val intent = Intent(applicationContext, ActivityRecognitionBroadcastReceiver::class.java)
        intent.setAction(INTENT_ACTION)

        mPendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, 0)

        val transitions = ArrayList<ActivityTransition>()

        transitions.add(ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build())

        transitions.add(ActivityTransition.Builder()
            .setActivityType(DetectedActivity.STILL)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build())

        transitions.add(ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build())

        transitions.add(ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build())

        val request = ActivityTransitionRequest(transitions)

        val task = ActivityRecognition.getClient(applicationContext)
            .requestActivityTransitionUpdates(request, mPendingIntent)

        task.addOnSuccessListener(
            object: OnSuccessListener<Void>{
                override fun onSuccess(p0: Void?) {
                    println("onSuccess")
                }
            }
        )

        task.addOnFailureListener { e: Exception ->
            // Handle error
            println("error handle")
            println(e.message)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        dbEngine?.close()
        unregisterReceiver(receiver)
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(arReceiver)

    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(ActivityRecognitionData.TABLE_NAME)
    }


    data class Config(
        var sensorObserver: Observer? = null,

        ) : SensorConfig(dbPath = "activityrecognition_data") {

        override fun <T : SensorConfig> replaceWith(config: T) {
            super.replaceWith(config)

            if (config is Config) {
                sensorObserver = config.sensorObserver
            }
        }
    }

    class ActivityRecognitionBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return
            logd("Sensor broadcast received. action: " + intent?.action)

            val result = ActivityTransitionResult.extractResult(intent)!!
            for (event in result.transitionEvents) {
                // chronological sequence of events....
                var data = ActivityRecognitionData()
                data.detectedActivity = event.activityType
                data.activityTransiton = event.transitionType
                data.timestamp = System.currentTimeMillis() / 1000
                data.deviceId = CONFIG.deviceId
                data.label = CONFIG.label

                CONFIG.sensorObserver?.onDataChanged(data)

                Intent().also { intent ->
                    intent.setAction(ACTION_AWARE_ACTIVITYRECOGNITION_SAVE)
                    intent.putExtra("ardata", data)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                }
            }

            when (intent?.action) {
                SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        if (context != null) {
                            start(context)
                        }
                    }
                }

                ACTION_AWARE_ACTIVITYRECOGNITION_STOP,
                SENSOR_STOP_ALL -> {
                    if (context != null) {
                        stop(context)
                    }
                    logd("Stopping sensor.")
                }

                ACTION_AWARE_ACTIVITYRECOGNITION_START -> {
                    if (context != null) {
                        start(context)
                    }
                }
            }
        }
    }
}

private fun logd(text: String) {
    println(text)
    if (ActivityRecognitionSensor.CONFIG.debug) Log.d(ActivityRecognitionSensor.TAG, text)
}

private fun logw(text: String) {
    println(text)
    Log.w(ActivityRecognitionSensor.TAG, text)
}

