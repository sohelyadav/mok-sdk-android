package one.mok

import com.unotag.mokone.MokApplication
import com.unotag.mokone.MokSDK
import com.unotag.mokone.utils.MokLogger

class ExampleApplication : MokApplication() {

    override fun onCreate() {
        //registerActivityLifecycleCallbacks(this)
        super.onCreate()
        MokSDK.getInstance(applicationContext)
        MokSDK.initMokSDK(isProdEnv = false, delayMillis = 2000 )
        MokLogger.setLogLevel(MokLogger.LogLevel.DEBUG)
    }
}

