package biz.eventually.atpl

import android.app.Application
import biz.eventually.atpl.di.AppComponent

/**
 * Created by laminr on 18/03/2017.
 */

class AtplApplication : Application() {

    companion object {
        lateinit var component: AppComponent
    }

    override fun onCreate() {
        super.onCreate()
        component = AppComponent.Initializer.init(this)
        println("AtplApplication")

//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return
//        }
//        LeakCanary.install(this)
    }

}