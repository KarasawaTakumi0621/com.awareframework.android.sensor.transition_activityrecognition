package com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition

import android.app.PendingIntent
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
import com.awareframework.android.core.AwareSensor
import com.awareframework.android.core.model.SensorConfig
import com.tappun.android.sensor.transition_activityrecognitionensortransition_activityrecognition.model.ActivityRecognitionData

class ActivityRecognitionSensor: AwareSensor() {

    companion object {
        const val TAG = "AWARE::ActivityRecognition"

        const val ACTION_AWARE_ACTIVITYRECOGNITION = "ACTION_AWARE_ACTIVITYRECOGNITION"

        const val ACTION_AWARE_ACTIVITYRECOGNITION_START = "com.tappun.android.sensor.transition_activityrecognition.SENSOR_START"
        const val ACTION_AWARE_ACTIVITYRECOGNITION_STOP = "com.tappun.android.sensor.transition_activityrecognition.SENSOR_STOP"
        const val ACTION_AWARE_ACTIVITYRECOGNITION_SET_LABEL = "com.tappun.android.sensor.transition_activityrecognition.SET_LABEL"
        const val EXTRA_LABEL = "label"

        const val ACTION_AWARE_ACTIVITYRECOGNITION_SYNC = "com.tappun.android.sensor.transition_activityrecognition.SENSOR_SYNC"

        val CONFIG = Config()

        @RequiresApi(Build.VERSION_CODES.O)
        fun start(context: Context, config: Config? = null) {
            if (config != null)
                CONFIG.replaceWith(config)

            context.startForegroundService(Intent(context, ActivityRecognitionSensor::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ActivityRecognitionSensor::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(ActivityRecognitionData.TABLE_NAME)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    interface Observer {
        fun onDataChanged(data: ActivityRecognitionData)
    }

    data class Config(
        var sensorObserver: Observer? = null,
        var interval: Int = 10000,

        ) : SensorConfig(dbPath = "activityrecognition_data") {

        override fun <T : SensorConfig> replaceWith(config: T) {
            super.replaceWith(config)

            if (config is Config) {
                sensorObserver = config.sensorObserver
                interval = config.interval
            }
        }
    }

    class ActivityRecognitionBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return
            logd("Sensor broadcast received. action: " + intent?.action)

            when (intent?.action) {
                SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        start(context)
                    }
                }

                ACTION_AWARE_ACTIVITYRECOGNITION_STOP,
                SENSOR_STOP_ALL -> {
                    stop(context)
                    logd("Stopping sensor.")
                }

                ACTION_AWARE_ACTIVITYRECOGNITION_START -> {
                    start(context)
                }
            }
        }
    }
}

private fun logd(text: String) {
    if (ActivityRecognitionSensor.CONFIG.debug) Log.d(ActivityRecognitionSensor.TAG, text)
}

private fun logw(text: String) {
    Log.w(ActivityRecognitionSensor.TAG, text)
}

