package com.unotag.mokone.inAppMessage

import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.google.gson.Gson
import com.unotag.mokone.db.InAppMessageEntity
import com.unotag.mokone.db.MokDb
import com.unotag.mokone.inAppMessage.data.InAppMessageData
import com.unotag.mokone.inAppMessage.data.InAppMessageItem
import com.unotag.mokone.inAppMessage.ui.InAppMessageBaseActivity
import com.unotag.mokone.network.MokApiCallTask
import com.unotag.mokone.network.MokApiConstants
import com.unotag.mokone.pip.ui.PiPActivity
import com.unotag.mokone.utils.MokLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class InAppMessageHandler(private val context: Context, private val userId: String) {

    fun fetchIAMFromServerAndSaveToDB(
        callback: (inAppMessageData: InAppMessageData?, error: String?) -> Unit
    ) {
        val apiCallTask = MokApiCallTask()
        apiCallTask.performApiCall(
            //MokApiConstants.BASE_URL + MokApiConstants.URL_IN_APP_MESSAGE_DATA + "?external_player_id=$userId",
            MokApiConstants.BASE_URL + MokApiConstants.URL_PENDING_IN_APP_MESSAGE + userId,
            MokApiCallTask.HttpMethod.GET,
            MokApiCallTask.MokRequestMethod.READ
        ) { result ->
            when (result) {
                is MokApiCallTask.ApiResult.Success -> {
                    val response = result.response
                    try {
                        val gson = Gson()
                        val inAppMessageData =
                            gson.fromJson(response.toString(), InAppMessageData::class.java)

                        MokLogger.log(MokLogger.LogLevel.DEBUG, "Before async tasks")

                        // Use async to save each in-app message sequentially
                        val deferredResults = inAppMessageData.data?.map { inAppMessage ->
                            val inAppMessageId = inAppMessage?.inAppId
                            val inAppMessageAsString = Gson().toJson(inAppMessage)
                            CoroutineScope(Dispatchers.IO).async {
                                MokLogger.log(
                                    MokLogger.LogLevel.DEBUG,
                                    "Start async task for $inAppMessageId"
                                )

                                if (inAppMessage?.jsonData?.title.isNullOrEmpty() &&
                                    inAppMessage?.jsonData?.text.isNullOrEmpty() &&
                                    inAppMessage?.jsonData?.image.isNullOrEmpty() &&
                                    inAppMessage?.jsonData?.popupConfigs?.videoUrl.isNullOrEmpty() &&
                                    inAppMessage?.jsonData?.popupConfigs?.webUrl.isNullOrEmpty()
                                ) {
                                    MokLogger.log(MokLogger.LogLevel.DEBUG, "IAM has no data, skip saving in local DB")
                                } else {
                                    saveInAppMessageInLocalDb(
                                        inAppMessageId,
                                        inAppMessageAsString
                                    ).await()
                                }
                                MokLogger.log(
                                    MokLogger.LogLevel.DEBUG,
                                    "Async task completed for $inAppMessageId"
                                )
                            }
                        }
                        // Await all the deferred results
                        runBlocking {
                            deferredResults?.awaitAll()
                        }

                        MokLogger.log(
                            MokLogger.LogLevel.DEBUG,
                            "In-App Message data fetched successfully"
                        )
                        callback(inAppMessageData, null)
                    } catch (e: Exception) {
                        MokLogger.log(
                            MokLogger.LogLevel.DEBUG,
                            e.localizedMessage ?: "Failed to parse the API response"
                        )
                        callback(null, "Failed to parse the API response")
                    }
                }

                is MokApiCallTask.ApiResult.Error -> {
                    val error = result.exception
                    callback(null, error.localizedMessage)
                }

                else -> {
                    callback(null, "Something went wrong")
                }
            }
        }
    }


    private suspend fun saveInAppMessageInLocalDb(
        inAppMessageId: String?,
        inAppMessageAsString: String?
    ): Deferred<Unit> {
        return CoroutineScope(Dispatchers.IO).async {
            val db = Room.databaseBuilder(context, MokDb::class.java, "mok-database").build()

            try {
                val inAppMessageEntity =
                    InAppMessageEntity(inAppMessageId ?: "", inAppMessageAsString)
                val inAppMessageDao = db.inAppMessageDao()
                inAppMessageDao.insert(inAppMessageEntity)
            } catch (e: Exception) {
                MokLogger.log(
                    MokLogger.LogLevel.ERROR,
                    "Error inserting in-app message: ${e.message}"
                )
            } finally {
                //db.close()
            }
        }
    }

    fun markIAMReadInLocalAndServer(
        inAppMessageId: String,
        callback: ((inAppMessageData: String?, error: String?) -> Unit)?
    ) {
        val apiCallTask = MokApiCallTask()
        apiCallTask.performApiCall(
            MokApiConstants.BASE_URL + MokApiConstants.URL_MARK_READ_IN_APP_MESSAGE + userId + "?in_app_id=$inAppMessageId",
            MokApiCallTask.HttpMethod.PATCH,
            MokApiCallTask.MokRequestMethod.WRITE
        ) { result ->
            when (result) {
                is MokApiCallTask.ApiResult.Success -> {
                    val response = result.response
                    try {
                        callback?.invoke(response.toString(), null)


                        MokLogger.log(
                            MokLogger.LogLevel.DEBUG,
                            "In-App Message mark as read successfully in server"
                        )
                    } catch (e: Exception) {
                        callback?.invoke(null, "Failed to parse the API response")

                    }
                }

                is MokApiCallTask.ApiResult.Error -> {
                    val error = result.exception
                    callback?.invoke(null, error.localizedMessage)
                }

                else -> {
                    callback?.invoke(null, "Something went wrong")
                }
            }
        }
    }


    fun deleteSeenIAMAsLocally(inAppMessageId: String): Deferred<Unit> {
        return CoroutineScope(Dispatchers.IO).async {
            val db = Room.databaseBuilder(context, MokDb::class.java, "mok-database").build()

            try {
                val inAppMessageDao = db.inAppMessageDao()
                inAppMessageDao.deleteInAppMessage(inAppMessageId)
                MokLogger.log(
                    MokLogger.LogLevel.DEBUG,
                    "IAM Message deleted from local successfully"
                )
            } catch (e: Exception) {
                MokLogger.log(
                    MokLogger.LogLevel.ERROR,
                    "Failed to delete IAM in local database: ${e.message}"
                )
            } finally {
                //db.close()
            }
        }
    }


    private suspend fun fetchIAMEntry(limit: Int): List<InAppMessageEntity>? {
        val db = Room.databaseBuilder(context, MokDb::class.java, "mok-database").build()

        return try {
            val inAppMessageDao = db.inAppMessageDao()
            inAppMessageDao.getUnseenMessages(limit)
        } catch (e: Exception) {
            MokLogger.log(
                MokLogger.LogLevel.ERROR,
                "Error fetching latest in-app message entry from the database: ${e.message}"
            )
            null
        } finally {
            // db.close()
        }
    }

    private fun activityLaunchEngine(inAppMessageItem: InAppMessageItem) {
        if (inAppMessageItem.jsonData?.popupConfigs?.videoUrl.isNullOrEmpty()) {
            launchIAMBaseActivity(inAppMessageItem)
        } else {
            launchPipVideo(inAppMessageItem)
        }
    }

    private fun launchPipVideo(inAppMessageItem: InAppMessageItem) {
        MokLogger.log(MokLogger.LogLevel.INFO, "PIP video launched")
        val intent = Intent(context, PiPActivity::class.java)
        intent.putExtra("in_app_message_data", inAppMessageItem)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }


    private fun launchIAMBaseActivity(inAppMessageItem: InAppMessageItem) {
        val intent = Intent(context, InAppMessageBaseActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("in_app_message_data", inAppMessageItem)
        context.startActivity(intent)
    }

    fun showInAppMessages(limit: Int? = 1) {
        MokLogger.log(MokLogger.LogLevel.INFO, "showInAppMessages called with limit $limit")
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            try {
                val inAppMessageEntries = fetchIAMEntry(limit ?: 1)
                if (inAppMessageEntries != null) {
                    withContext(Dispatchers.Main) {
                        for (inAppMessageEntry in inAppMessageEntries) {
                            if (!inAppMessageEntry.inAppMessageAsString.isNullOrEmpty()) {
                                val inAppMessageItem = Gson().fromJson(
                                    inAppMessageEntry.inAppMessageAsString,
                                    InAppMessageItem::class.java
                                )
                                activityLaunchEngine(inAppMessageItem)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                MokLogger.log(
                    MokLogger.LogLevel.ERROR,
                    "Error showing in-app message dialog: ${e.message}"
                )
            }
        }
    }


    fun deleteAllInAppMessages(callback: ((success: Boolean?, error: String?) -> Unit)?) {
        val db = Room.databaseBuilder(context, MokDb::class.java, "mok-database").build()
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val inAppMessageDao = db.inAppMessageDao()
                inAppMessageDao.deleteAllInAppMessages()
                withContext(Dispatchers.Main) {
                    MokLogger.log(
                        MokLogger.LogLevel.INFO,
                        "All in app messages deleted successfully"
                    )
                }
            }
            callback?.invoke(true, null)
        } catch (e: Exception) {
            callback?.invoke(false, e.localizedMessage)
            MokLogger.log(
                MokLogger.LogLevel.ERROR, "Error deleting all in-app messages: ${e.message}"
            )
        } finally {
            // db.close()
        }
    }

    fun getIAMCount(callback: ((success: String?, error: String?) -> Unit)?) {
        val db = Room.databaseBuilder(context, MokDb::class.java, "mok-database").build()

        try {
            CoroutineScope(Dispatchers.IO).launch {
                val inAppMessageDao = db.inAppMessageDao()
                val count = inAppMessageDao.getMessageCount()
                withContext(Dispatchers.Main) {
                    MokLogger.log(MokLogger.LogLevel.INFO, "IAM count: $count")
                    callback?.invoke(count.toString(), null)
                }
            }
        } catch (e: Exception) {
            MokLogger.log(MokLogger.LogLevel.ERROR, "Error getting IAM count: ${e.message}")
            callback?.invoke(null, e.localizedMessage)
        } finally {
            // db.close()
        }
    }

}






