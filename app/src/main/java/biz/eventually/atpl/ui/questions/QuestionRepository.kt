package biz.eventually.atpl.ui.questions

import android.annotation.SuppressLint
import biz.eventually.atpl.data.DataProvider
import biz.eventually.atpl.data.NetworkStatus
import biz.eventually.atpl.data.dao.LastCallDao
import biz.eventually.atpl.data.dao.QuestionDao
import biz.eventually.atpl.data.db.LastCall
import biz.eventually.atpl.data.db.Question
import biz.eventually.atpl.ui.BaseRepository
import biz.eventually.atpl.utils.hasInternetConnection
import com.google.firebase.perf.metrics.AddTrace
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Thibault de Lambilly on 20/03/17.
 *
 */
@Singleton
class QuestionRepository @Inject constructor(private val dataProvider: DataProvider, private val dao: QuestionDao, private val lastCallDao: LastCallDao) : BaseRepository() {

    @AddTrace(name = "launchTest", enabled = true)
    fun getQuestions(topicId: Long, starFist: Boolean, then: (data: List<Question>) -> Unit) {

        // has Network: request data
        if (hasInternetConnection()) {
            getWebData(topicId, starFist) { data ->
                then(data)
            }
        }
        // No network: taking in database
        else {
            doAsync {
                val data = getDataFromDb(topicId)
                uiThread {
                    then(data)
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    fun getWebData(topicId: Long, starFist: Boolean, fromScratch: Boolean = false, silent: Boolean = false, then: (data: List<Question>) -> Unit) {

        doAsync {
            val lastCall = if (fromScratch) 0L else lastCallDao.findByType("${LastCall.TYPE_TOPIC}_$topicId")?.updatedAt ?: 0L
            uiThread {
                if (!silent) status.postValue(NetworkStatus.LOADING)
                dataProvider
                        .dataGetTopicQuestions(topicId, starFist, lastCall)
                        .subscribeOn(scheduler.network)
                        .map { question -> analyseData(topicId, question) }
                        .map { getDataFromDb(topicId) }
                        .observeOn(scheduler.main)
                        .subscribe({ data ->
                            if (!silent) status.postValue(NetworkStatus.SUCCESS)
                            then(data)
                        }, { e ->
                            if (!silent) status.postValue(NetworkStatus.ERROR)
                            Timber.d("launchTest -> WebData: $e")
                        })
            }
        }
    }

    fun getTopicQuestionWithImage(topicId: Long): List<Question> {
        return dao.getQuestionWithImage(topicId)
    }

    private fun getDataFromDb(topicId: Long): List<Question> {
        return dao.findByTopicId(topicId).map {
            Question(
                    it.question.idWeb,
                    topicId,
                    it.question.label,
                    it.question.img
            ).apply {
                answers = it.answers ?: listOf()
            }
        }
    }


    private fun analyseData(topicId: Long, questionsWeb: List<Question>) {

        val questionsId = dao.getIds()

        questionsWeb.forEach { qWeb ->
            // Update
            if (qWeb.idWeb in questionsId) {
                dao.findById(qWeb.idWeb)?.let {
                    it.label = qWeb.label
                    it.img = qWeb.img

                    dao.updateQuestionAndAnswers(it)
                }
            }
            // New
            else {
                qWeb.topicId = topicId
                dao.insertQuestionAndAnswers(qWeb)
            }
        }

        // update time reference
        if (questionsWeb.isNotEmpty()) {
            lastCallDao.updateOrInsert(LastCall("${LastCall.TYPE_TOPIC}_$topicId", Date().time))
        }
    }
}