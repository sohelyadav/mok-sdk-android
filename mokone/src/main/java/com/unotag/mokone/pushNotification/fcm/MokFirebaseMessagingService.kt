package com.unotag.mokone.pushNotification.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.ktx.messaging
import com.unotag.mokone.MokSDK
import com.unotag.mokone.R
import com.unotag.mokone.pushNotification.NotificationRenderer
import com.unotag.mokone.pushNotification.call.CallNotificationHandler
import com.unotag.mokone.utils.MokLogger
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MokFirebaseMessagingService : FirebaseMessagingService() {

    val imagesList: MutableList<String> =
        arrayListOf(
            "https://www.defenceconnect.com.au/images-backup/Technology/Medical-injections.jpg",
            "https://image.freepik.com/free-psd/plastic-pill-bottle-with-colorful-vitamins-mockup_7956-755.jpg",
            "https://images.squarespace-cdn.com/content/v1/5ab6762de2ccd18f9039391d/1532011051373-ZG46GDbQiz7iBIgNCzklBDD2o6CESiqIlH5ssNFrtmA/image-asset.jpeg"
        )

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        MokLogger.log(MokLogger.LogLevel.DEBUG, "From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            MokLogger.log(MokLogger.LogLevel.DEBUG, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]

            MokLogger.log(MokLogger.LogLevel.DEBUG, "MokFirebaseMessagingService type : call")
            val requestCode = 0
            val launchIntent = getLaunchIntent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                CallNotificationHandler.buildCallNotificationAboveAPI31(this,"test", getString(R.string.call_notification_channel_id), getPendingIntent(launchIntent, requestCode) )
            }

//            if (title.isNullOrEmpty()) {
//                MokLogger.log(MokLogger.LogLevel.DEBUG, "MokFirebaseMessagingService type : in app")
//
//                val popupConfigs =
//                    Gson().fromJson(remoteMessage.data["popup_configs"], PopupConfigs::class.java)
//                val getInAppMsgData = popupConfigs?.getInAppMsgData ?: false
//
//                if (getInAppMsgData) {
//                    handleInAppNotification()
//                }
//
//            } else if (remoteMessage.data.containsKey("image")) {
//                MokLogger.log(MokLogger.LogLevel.DEBUG, "MokFirebaseMessagingService type : image")
//                val image = remoteMessage.data["image"]
//                val bigIcon = remoteMessage.data["icon"]
//                val bigPicture = getBitmapFromUrl(image)
//                val bigLargeIcon: Bitmap? = getBitmapFromUrl(bigIcon)
//                sendNotification(title, body, bigPicture, bigLargeIcon)
//            } else if (remoteMessage.data.containsKey("call")) {
//                   MokLogger.log(MokLogger.LogLevel.DEBUG, "MokFirebaseMessagingService type : call")
//                val requestCode = 0
//                val launchIntent = getLaunchIntent()
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    CallNotificationHandler.buildCallNotificationAboveAPI31(this,"test", getString(R.string.call_notification_channel_id), getPendingIntent(launchIntent, requestCode) )
//                }
//            } else {
//                MokLogger.log(MokLogger.LogLevel.DEBUG, "MokFirebaseMessagingService type : default")
//                sendNotification(title, body)
//                setNotificationSliderData("test", "body", imagesList)
//            }
        }
    }

    // Check if message contains a notification payload.
//        remoteMessage.notification?.let {
//            MokLogger.log(MokLogger.LogLevel.DEBUG, "Message Notification Body: ${it.body}")
//            it.body?.let { body -> sendNotification() }
//        }

    // [START on_new_token]
    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        MokLogger.log(MokLogger.LogLevel.DEBUG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }
    // [END on_new_token]


    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        MokLogger.log(MokLogger.LogLevel.DEBUG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(
        title: String?,
        messageBody: String?,
        bigPicture: Bitmap? = null,
        bigLargeIcon: Bitmap? = null
    ) {
        val requestCode = 0
        val launchIntent = getLaunchIntent()
        val pendingIntent = getPendingIntent(launchIntent, requestCode)
        val channelId = getString(R.string.default_notification_channel_id)
        val notificationBuilder = buildNotification(
            title,
            messageBody,
            bigPicture,
            bigLargeIcon,
            channelId,
            pendingIntent
        )
        notify(notificationBuilder)
    }

    private fun getLaunchIntent(): Intent? {
        val launchIntent =
            applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return launchIntent
    }

    private fun getPendingIntent(intent: Intent?, requestCode: Int): PendingIntent? {
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildNotification(
        title: String?,
        messageBody: String?,
        bigPicture: Bitmap?,
        bigLargeIcon: Bitmap?,
        channelId: String,
        pendingIntent: PendingIntent?
    ): NotificationCompat.Builder {
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        return NotificationCompat.Builder(this, channelId)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .bigLargeIcon(bigLargeIcon)
            )
            .setSmallIcon(NotificationRenderer.getSmallNotificationIcon())
            .setContentTitle(title)
            .setContentText(messageBody)
            .setLargeIcon(bigLargeIcon)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
    }

    private fun notify(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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


    private fun setNotificationSliderData(
        title: String,
        notificationMessage: String?,
        imagesList: MutableList<String>
    ) {
        // parent notification layout
        val expandedView = RemoteViews(packageName, R.layout.notification_image_slider_layout)
// notification title
        expandedView.setTextViewText(R.id.textViewTitle, title)
        for (imgUrl: String in imagesList) {
            //for loop start
            val viewFlipperImage = RemoteViews(packageName, R.layout.view_notification_image)
            if (TextUtils.isEmpty(imgUrl)) {
                // set default image
                viewFlipperImage.setImageViewResource(
                    R.id.imageView, NotificationRenderer.getSmallNotificationIcon()
                )
            } else {
                try {
                    val remotePicture = getBitmapFromUrl(imgUrl)
                    viewFlipperImage.setImageViewBitmap(
                        R.id.imageView, remotePicture
                    )
                } catch (e: IOException) {
                    // set default image
                    viewFlipperImage.setImageViewResource(
                        R.id.imageView,
                        NotificationRenderer.getSmallNotificationIcon()
                    )
                }
            }
            // Adding each image view in the viewflipper.
            expandedView.addView(R.id.viewFlipper, viewFlipperImage)
        } // for loop finish
        showNotificationForNotificationList(
            title,
            notificationMessage!!,
            expandedView
        )
    }


    private fun showNotificationForNotificationList(
        title: String,
        notificationDesc: String,
        expandedView: RemoteViews
    ) {
        generateBigTextNotificationForNotificationList(
            this,
            title,
            "test",
            expandedView
        )
    }

    fun generateBigTextNotificationForNotificationList(
        context: Context,
        notificationTitle: String?,
        notificationMessage: String?,
        expandedView: RemoteViews
    ) {
        val channelId = "my_channel_id"
        val notification_id = 0
        val requestCode = 0
        val launchIntent = getLaunchIntent()
        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.bigText(notificationMessage)
        bigTextStyle.setSummaryText(notificationTitle)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(NotificationRenderer.getSmallNotificationIcon())
            .setLargeIcon(
                BitmapFactory.decodeResource
                    (context.resources, NotificationRenderer.getSmallNotificationIcon())
            ) // optional

            .setContentIntent(getPendingIntent(launchIntent, requestCode))
            .setContentTitle(notificationTitle)
            .setContentText(notificationMessage)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(expandedView)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)


        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
    }


    fun buildCallNotification(){

    }



    private fun getBitmapFromUrl(imageUrl: String?): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            MokLogger.log(
                MokLogger.LogLevel.ERROR,
                "Error in getting notification image: " + e.localizedMessage
            )
            null
        }
    }

    fun getFCMToken(callback: (String?, String?) -> Unit) {
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                MokLogger.log(MokLogger.LogLevel.DEBUG, "FCM token: $token")
                callback(token, null)
            } else {
                MokLogger.log(
                    MokLogger.LogLevel.ERROR,
                    "Fetching FCM registration token failed",
                    task.exception
                )
                callback(null, task.exception?.localizedMessage)
            }
        }
    }

    private fun handleInAppNotification() {
        val mokSDK = MokSDK.getInstance(applicationContext)
        mokSDK.requestIAMFromServerAndShow()
    }





}