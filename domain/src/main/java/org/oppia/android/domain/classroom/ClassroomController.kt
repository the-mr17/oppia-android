package org.oppia.android.domain.classroom

import org.oppia.android.app.model.ClassroomIdList
import org.oppia.android.app.model.ClassroomSummary
import org.oppia.android.app.model.ProfileId
import org.oppia.android.app.model.SubtitledHtml
import org.oppia.android.app.model.TopicSummary
import org.oppia.android.domain.topic.TopicListController
import org.oppia.android.domain.translation.TranslationController
import org.oppia.android.domain.util.JsonAssetRetriever
import org.oppia.android.domain.util.getStringFromObject
import org.oppia.android.util.caching.AssetRepository
import org.oppia.android.util.caching.LoadLessonProtosFromAssets
import org.oppia.android.util.data.DataProvider
import org.oppia.android.util.data.DataProviders.Companion.transform
import org.oppia.android.util.locale.OppiaLocale
import javax.inject.Inject
import javax.inject.Singleton

const val TEST_CLASSROOM_ID_0 = "test_classroom_id_0"

private const val GET_CLASSROOM_LIST_PROVIDER_ID = "get_classroom_list_provider_id"

@Singleton
class ClassroomController @Inject constructor(
  private val jsonAssetRetriever: JsonAssetRetriever,
  private val assetRepository: AssetRepository,
  private val translationController: TranslationController,
  private val topicListController: TopicListController,
  @LoadLessonProtosFromAssets private val loadLessonProtosFromAssets: Boolean,
) {
  fun getClassroomList(profileId: ProfileId): DataProvider<List<ClassroomSummary>> {
    val translationLocaleProvider =
      translationController.getWrittenTranslationContentLocale(profileId)
    return translationLocaleProvider.transform(
      GET_CLASSROOM_LIST_PROVIDER_ID,
      ::createClassroomList
    )
  }

  private fun createClassroomList(
    contentLocale: OppiaLocale.ContentLocale
  ): List<ClassroomSummary> {
    return if (loadLessonProtosFromAssets) {
      val classroomIdList = assetRepository.loadProtoFromLocalAssets(
        assetName = "classrooms",
        baseMessage = ClassroomIdList.getDefaultInstance()
      )
      return classroomIdList.classroomIdsList.map {
        createClassroomSummary(it)
      }
    } else loadClassroomListFromJson(contentLocale)
  }

  private fun loadClassroomListFromJson(contentLocale: OppiaLocale.ContentLocale): List<ClassroomSummary> {
    val classroomIdJsonArray = jsonAssetRetriever
      .loadJsonFromAsset("classrooms.json")!!
      .getJSONArray("classroom_id_list")
    val classroomSummaryList: ArrayList<ClassroomSummary> = ArrayList()
    for (i in 0 until classroomIdJsonArray.length()) {
      classroomSummaryList.add(createClassroomSummary(classroomIdJsonArray.optString(i)))
    }
    return classroomSummaryList
  }

  private fun createClassroomSummary(classroomId: String): ClassroomSummary {
    return if (loadLessonProtosFromAssets) {
      assetRepository.loadProtoFromLocalAssets(
        assetName = classroomId,
        baseMessage = ClassroomSummary.getDefaultInstance()
      )
    } else loadClassroomFromJson(classroomId)
  }

  private fun loadClassroomFromJson(classroomId: String): ClassroomSummary {
    val classroomJsonObject = jsonAssetRetriever.loadJsonFromAsset("$classroomId.json")!!
    val classroomSummary = ClassroomSummary.newBuilder()
    classroomSummary.apply {
      setClassroomId(classroomJsonObject.getString("id"))
      classroomTitle = SubtitledHtml.newBuilder().apply {
        contentId = classroomJsonObject.getJSONObject("translatable_title").getStringFromObject("content_id")
        html = classroomJsonObject.getJSONObject("translatable_title").getStringFromObject("html")
      }.build()
      val topicIdArray = classroomJsonObject.getJSONArray("topic_ids")
      val topicSummaryList = mutableListOf<TopicSummary>()
      for (i in 0 until topicIdArray.length()) {
        topicSummaryList.add(topicListController.createTopicSummary(topicIdArray.getString(i)))
      }
      addAllTopicSummary(topicSummaryList)
    }
    return classroomSummary.build()
  }
}
