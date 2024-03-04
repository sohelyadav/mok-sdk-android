package com.unotag.mokone.pushNotification.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.unotag.mokone.R
import com.unotag.mokone.pushNotification.NotificationRenderer
import com.unotag.mokone.pushNotification.call.ui.IncomingCallActivity

object CallNotificationHandler {

    // Add other call notification utility functions as needed

    const val ACTION_ACCEPT_CALL = "ACTION_ACCEPT_CALL"
    const val ACTION_DECLINE_CALL = "ACTION_DECLINE_CALL"


    // Create a new call, setting the user as the caller.
    @RequiresApi(Build.VERSION_CODES.P)
    private val incomingCaller = Person.Builder()
        .setName("Jane Doe")
        .setImportant(true)
        .build()

    // Uncommented and modified buildCallNotificationAboveAPI31 function
    @RequiresApi(Build.VERSION_CODES.S)
    fun buildCallNotificationAboveAPI31(
        context: Context,
        callerName: String,
        channelId: String,
        contentIntent: PendingIntent?,
    ) {
        // Intent to launch IncomingCallActivity
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = Notification.Builder(context, channelId)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSmallIcon(NotificationRenderer.getSmallNotificationIcon())
            .setStyle(
                Notification.CallStyle.forIncomingCall(incomingCaller,
                    getCallActionIntent(context, ACTION_DECLINE_CALL),
                    getCallActionIntent(context, ACTION_ACCEPT_CALL))
            )
            .addPerson(incomingCaller)
            .build()

        notify(context, notificationBuilder)
    }


    // Modified the notify function to be public and accept context and Notification
    fun notify(context: Context, notification: Notification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = notification.channelId
            val channel = NotificationChannel(
                channelId, "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }


    fun buildCallNotificationBellowAPI31(
        context: Context,
        callerName: String,
        channelId: String,
        callActionIntent: PendingIntent?
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(NotificationRenderer.getSmallNotificationIcon())
            .setContentTitle("Incoming Call")
            .setContentText("Call from $callerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(callActionIntent, true)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .addAction(
                R.drawable.ic_call,
                "Accept",
                getCallActionIntent(context, ACTION_ACCEPT_CALL)
            )
            .addAction(
                R.drawable.ic_call,
                "Decline",
                getCallActionIntent(context, ACTION_DECLINE_CALL)
            )

        notify(context, builder)

    }

    private fun notify(context: Context, notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = notificationBuilder.build().channelId
            val channel = NotificationChannel(
                channelId, "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun getCallActionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, CallNotificationActionReceiver::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

}
