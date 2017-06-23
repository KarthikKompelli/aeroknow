package biz.eventually.atpl

import android.content.Intent
import android.os.Bundle
import biz.eventually.atpl.common.IntentIdentifier
import biz.eventually.atpl.data.model.Source
import biz.eventually.atpl.ui.BaseActivity
import biz.eventually.atpl.ui.source.SourceActivity
import biz.eventually.atpl.ui.source.SourceManager
import biz.eventually.atpl.utils.*
import java.util.*

import kotlinx.android.synthetic.main.activity_splash.*
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import cn.pedant.SweetAlert.SweetAlertDialog

// https://www.bignerdranch.com/blog/splash-screens-the-right-way/
class MainActivity : BaseActivity<SourceManager>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())

        setContentView(R.layout.activity_splash)
        AtplApplication.component.inject(this)

        splash_version.text = "v${BuildConfig.VERSION_NAME}"

        // for App Links
        intent?.let {
            handleIntent(it)
        }

        // save preference waiting to do the screen
        putLong(applicationContext, PREF_TIMER, 1000)
    }

    private fun start() {
        rotateloading.start()
        manager.getSources({ s -> openSourceActivity(s)}, { openSourceActivity(null)})
    }

    private fun handleIntent(intent: Intent) {
        val appLinkAction = intent.action
        val appLinkData = intent.data
        if (Intent.ACTION_VIEW == appLinkAction && appLinkData != null) {
            val token = appLinkData.lastPathSegment

            token?.let {
                PrefsPutString(this@MainActivity,PREF_TOKEN, token)

                SweetAlertDialog(this, SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                        .setTitleText(getString(R.string.dialog_title_ok))
                        .setContentText(getString(R.string.settings_api_saved))
                        .setCustomImage(R.drawable.ic_check)
                        .setConfirmClickListener({s ->
                            start()
                        })
                        .show()
            } ?: kotlin.run {
                SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText(getString(R.string.dialog_title_error))
                        .setContentText(getString(R.string.settings_api_error))
                        .setConfirmClickListener({s ->
                            start()
                        })
                        .show()
            }

        } else {
            start()
        }
    }


    fun openSourceActivity(sources: List<Source>?) {

        rotateloading.stop()
        val intent = Intent(this, SourceActivity::class.java)

        when(sources) {
            null -> intent.putExtra(IntentIdentifier.NETWORK_ERROR, true)
            else -> intent.putParcelableArrayListExtra(IntentIdentifier.SOURCE_LIST, sources as ArrayList<Source>)
        }

        startActivity(intent)
        finish()
    }

}
