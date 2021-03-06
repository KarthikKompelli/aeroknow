package biz.eventually.atpl.data

import biz.eventually.atpl.data.db.*
import biz.eventually.atpl.data.dto.SubjectView
import biz.eventually.atpl.data.network.*

/**
 * Created by Thibault de Lambilly on 21/03/17.
 *
 */

fun toAppSources(from: List<SourceNetwork>?) = from?.map({ Source(it.id, it.name) }) ?: listOf()

fun toAppSubjects(sourceId: Long, from: List<SubjectNetwork>?) = from?.map({ Subject(it.id, sourceId, it.name).apply { topics = toAppTopics(it.id, it.topics) } }) ?: listOf()

fun toAppSubjectViews(sourceId: Long, from: List<SubjectNetwork>?)
        = from?.map({ SubjectView(Subject(it.id, sourceId, it.name), toAppTopics(it.id, it.topics)) }) ?: listOf()

fun toAppTopic(from: TopicNetwork, subjectId: Long) = Topic(from.id, subjectId, from.name, from.questions, from.follow ?: 0, from.focus ?: 0)

fun toAppTopics(sourceId: Long = -1, from: List<TopicNetwork>?): List<Topic> {

    val list = mutableListOf<Topic>()
    from?.forEach {
        list.add(toAppTopic(it, sourceId))
    }

    return list
}

fun toAppAnswers(questionId: Long, from: List<AnswerNetwork>?): List<Answer> {
    return from?.map { Answer(it.id, questionId, it.value, it.good) } ?: listOf()
}

fun toAppQuestion(topicId: Long, from: QuestionNetwork) = Question(
        from.id,
        topicId,
        from.label,
        from.img ?: "",
        from.focus,
        from.follow?.good ?: 0,
        from.follow?.wrong ?: 0
).apply { answers = toAppAnswers(from.id, from.answers) }

fun toAppQuestions(topicId: Long, from: List<QuestionNetwork>?) = from?.map { toAppQuestion(topicId, it) } ?: listOf()