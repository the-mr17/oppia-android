package org.oppia.android.domain.classroom

import org.oppia.android.app.model.ClassroomIdList
import org.oppia.android.app.model.ClassroomSummary
import org.oppia.android.app.model.ProfileId
import org.oppia.android.domain.translation.TranslationController
import org.oppia.android.util.caching.AssetRepository
import org.oppia.android.util.data.DataProvider
import org.oppia.android.util.data.DataProviders.Companion.transform
import org.oppia.android.util.locale.OppiaLocale
import javax.inject.Inject
import javax.inject.Singleton

const val TEST_CLASSROOM_ID_0 = "test_classroom_id_0"

private const val GET_CLASSROOM_LIST_PROVIDER_ID = "get_classroom_list_provider_id"

@Singleton
class ClassroomController @Inject constructor(
  private val assetRepository: AssetRepository,
  private val translationController: TranslationController,
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
    val classroomIdList = assetRepository.loadProtoFromLocalAssets(
      assetName = "classrooms",
      baseMessage = ClassroomIdList.getDefaultInstance()
    )
    return classroomIdList.classroomIdsList.map {
      createClassroomSummary(it)
    }
  }

  private fun createClassroomSummary(classroomId: String): ClassroomSummary {
    return assetRepository.loadProtoFromLocalAssets(
      assetName = classroomId,
      baseMessage = ClassroomSummary.getDefaultInstance()
    )
  }
}
