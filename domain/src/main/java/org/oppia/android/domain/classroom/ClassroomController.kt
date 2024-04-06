package org.oppia.android.domain.classroom

import androidx.lifecycle.MutableLiveData
import org.oppia.android.app.model.ClassroomIdList
import org.oppia.android.app.model.ClassroomSummary
import org.oppia.android.app.model.ProfileId
import org.oppia.android.app.model.SubtitledHtml
import org.oppia.android.app.model.TopicSummary
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
import org.json.JSONObject
import org.oppia.android.app.model.EphemeralTopicSummary
import org.oppia.android.app.model.StoryRecord
import org.oppia.android.app.model.TopicIdList
import org.oppia.android.app.model.TopicList
import org.oppia.android.app.model.TopicPlayAvailability
import org.oppia.android.app.model.TopicRecord
import org.oppia.android.domain.topic.createTopicThumbnailFromJson

const val TEST_CLASSROOM_ID_0 = "test_classroom_id_0"
const val TEST_CLASSROOM_ID_1 = "test_classroom_id_1"

private const val GET_CLASSROOM_LIST_PROVIDER_ID = "get_classroom_list_provider_id"

@Singleton
class ClassroomController @Inject constructor(
  private val jsonAssetRetriever: JsonAssetRetriever,
  private val assetRepository: AssetRepository,
  private val translationController: TranslationController,
  @LoadLessonProtosFromAssets private val loadLessonProtosFromAssets: Boolean,
) {
  val selectedClassroomId = MutableLiveData(TEST_CLASSROOM_ID_0)

  fun getClassroomList(profileId: ProfileId): DataProvider<List<ClassroomSummary>> {
    val translationLocaleProvider =
      translationController.getWrittenTranslationContentLocale(profileId)
    return translationLocaleProvider.transform(
      GET_CLASSROOM_LIST_PROVIDER_ID,
      ::createClassroomList
    )
  }

  fun getTopicList(profileId: ProfileId, classroomId: String): DataProvider<TopicList> {
    // selectedClassroomId = classroomId
    val translationLocaleProvider =
      translationController.getWrittenTranslationContentLocale(profileId)
    return translationLocaleProvider.transform(
      "GET_TOPIC_LIST_PROVIDER_ID",
      ::createTopicList
    )
  }

  fun switchClassroom(classroomId: String) {
    selectedClassroomId.value = classroomId
  }

  fun getSelectedClassroomId() = selectedClassroomId.value

  private fun createTopicList(contentLocale: OppiaLocale.ContentLocale): TopicList {
    return if (loadLessonProtosFromAssets) {
      val topicIdList =
        assetRepository.loadProtoFromLocalAssets(
          assetName = "topics",
          baseMessage = TopicIdList.getDefaultInstance()
        )
      return TopicList.newBuilder().apply {
        // Only include topics currently playable in the topic list.
        addAllTopicSummary(
          topicIdList.topicIdsList.map {
            createEphemeralTopicSummary(it, contentLocale)
          }.filter {
            it.topicSummary.topicPlayAvailability.availabilityCase == TopicPlayAvailability.AvailabilityCase.AVAILABLE_TO_PLAY_NOW
          }
        )
      }.build()
    } else loadTopicListFromJson(contentLocale)
  }

  private fun loadTopicListFromJson(contentLocale: OppiaLocale.ContentLocale): TopicList {
    val topicIdJsonArray = jsonAssetRetriever
      .loadJsonFromAsset("${selectedClassroomId.value}.json")!!
      .getJSONArray("topic_ids")
    val topicListBuilder = TopicList.newBuilder()
    for (i in 0 until topicIdJsonArray.length()) {
      val ephemeralSummary =
        createEphemeralTopicSummary(topicIdJsonArray.optString(i)!!, contentLocale)
      val topicPlayAvailability = ephemeralSummary.topicSummary.topicPlayAvailability
      // Only include topics currently playable in the topic list.
      if (topicPlayAvailability.availabilityCase == TopicPlayAvailability.AvailabilityCase.AVAILABLE_TO_PLAY_NOW) {
        topicListBuilder.addTopicSummary(ephemeralSummary)
      }
    }
    return topicListBuilder.build()
  }

  private fun createEphemeralTopicSummary(
    topicId: String,
    contentLocale: OppiaLocale.ContentLocale
  ): EphemeralTopicSummary {
    val topicSummary = createTopicSummary(topicId)
    return EphemeralTopicSummary.newBuilder().apply {
      this.topicSummary = topicSummary
      writtenTranslationContext =
        translationController.computeWrittenTranslationContext(
          topicSummary.writtenTranslationsMap, contentLocale
        )
    }.build()
  }

  private fun createTopicSummary(topicId: String): TopicSummary {
    return if (loadLessonProtosFromAssets) {
      val topicRecord =
        assetRepository.loadProtoFromLocalAssets(
          assetName = topicId,
          baseMessage = TopicRecord.getDefaultInstance()
        )
      val storyRecords = topicRecord.canonicalStoryIdsList.map {
        assetRepository.loadProtoFromLocalAssets(
          assetName = it,
          baseMessage = StoryRecord.getDefaultInstance()
        )
      }
      TopicSummary.newBuilder().apply {
        this.topicId = topicId
        putAllWrittenTranslations(topicRecord.writtenTranslationsMap)
        title = topicRecord.translatableTitle
        totalChapterCount = storyRecords.map { it.chaptersList.size }.sum()
        topicThumbnail = topicRecord.topicThumbnail
        topicPlayAvailability = if (topicRecord.isPublished) {
          TopicPlayAvailability.newBuilder().setAvailableToPlayNow(true).build()
        } else {
          TopicPlayAvailability.newBuilder().setAvailableToPlayInFuture(true).build()
        }
        storyRecords.firstOrNull()?.storyId?.let { this.firstStoryId = it }
      }.build()
    } else {
      createTopicSummaryFromJson(topicId, jsonAssetRetriever.loadJsonFromAsset("$topicId.json")!!)
    }
  }

  private fun createTopicSummaryFromJson(topicId: String, jsonObject: JSONObject): TopicSummary {
    var totalChapterCount = 0
    val storyData = jsonObject.getJSONArray("canonical_story_dicts")
    for (i in 0 until storyData.length()) {
      totalChapterCount += storyData
        .getJSONObject(i)
        .getJSONArray("node_titles")
        .length()
    }
    val firstStoryId =
      if (storyData.length() == 0) "" else storyData.getJSONObject(0).getStringFromObject("id")

    val topicPlayAvailability = if (jsonObject.getBoolean("published")) {
      TopicPlayAvailability.newBuilder().setAvailableToPlayNow(true).build()
    } else {
      TopicPlayAvailability.newBuilder().setAvailableToPlayInFuture(true).build()
    }
    val topicTitle = SubtitledHtml.newBuilder().apply {
      contentId = "title"
      html = jsonObject.getStringFromObject("topic_name")
    }.build()
    // No written translations are included since none are retrieved from JSON.
    return TopicSummary.newBuilder()
      .setTopicId(topicId)
      .setTitle(topicTitle)
      .setVersion(jsonObject.optInt("version"))
      .setTotalChapterCount(totalChapterCount)
      .setTopicThumbnail(createTopicThumbnailFromJson(jsonObject))
      .setTopicPlayAvailability(topicPlayAvailability)
      .setFirstStoryId(firstStoryId)
      .build()
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
        topicSummaryList.add(createTopicSummary(topicIdArray.getString(i)))
      }
      addAllTopicSummary(topicSummaryList)
    }
    return classroomSummary.build()
  }
}
