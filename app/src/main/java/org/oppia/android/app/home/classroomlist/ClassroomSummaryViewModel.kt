package org.oppia.android.app.home.classroomlist

import java.util.Objects
import org.oppia.android.app.model.ClassroomSummary
import org.oppia.android.app.viewmodel.ObservableViewModel
import org.oppia.android.domain.translation.TranslationController

/** The view model corresponding to individual classroom summaries in the classroom summary RecyclerView. */
class ClassroomSummaryViewModel(
  private val classroomSummary: ClassroomSummary,
  translationController: TranslationController
): ObservableViewModel() {
  val title: String by lazy {
    translationController.extractString(
      classroomSummary.classroomTitle, classroomSummary.writtenTranslationContext
    )
  }

  // Overriding equals is needed so that DataProvider combine functions used in the HomeViewModel
  // will only rebind when the actual data in the data list changes, rather than when the ViewModel
  // object changes.
  override fun equals(other: Any?): Boolean {
    return other is ClassroomSummaryViewModel && other.classroomSummary == this.classroomSummary
  }

  override fun hashCode() = Objects.hash(classroomSummary)
}
