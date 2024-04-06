package org.oppia.android.app.home.classroomlist

import androidx.databinding.ObservableBoolean
import java.util.Objects
import org.oppia.android.app.classroom.SwitchClassroomListener
import org.oppia.android.app.model.ClassroomSummary
import org.oppia.android.app.viewmodel.ObservableViewModel
import org.oppia.android.domain.classroom.ClassroomController
import org.oppia.android.domain.translation.TranslationController

/** The view model corresponding to individual classroom summaries in the classroom summary RecyclerView. */
class ClassroomSummaryViewModel(
  private val classroomSummary: ClassroomSummary,
  private val classroomController: ClassroomController,
  translationController: TranslationController
): ObservableViewModel(), SwitchClassroomListener {

  val title: String by lazy {
    translationController.extractString(
      classroomSummary.classroomTitle, classroomSummary.writtenTranslationContext
    )
  }

  val isSelected = ObservableBoolean(
    classroomController.getSelectedClassroomId() == classroomSummary.classroomId
  )

  /** Called when user clicks on ClassroomSummaryCard. */
  fun onClassroomClicked() {
    this.switchClassroom(classroomSummary.classroomId)
    // isSelected.set(classroomController.getSelectedClassroomId() == classroomSummary.classroomId)
  }

  override fun switchClassroom(classroomId: String) {
    classroomController.switchClassroom(classroomId)
  }

  // Overriding equals is needed so that DataProvider combine functions used in the HomeViewModel
  // will only rebind when the actual data in the data list changes, rather than when the ViewModel
  // object changes.
  override fun equals(other: Any?): Boolean {
    return other is ClassroomSummaryViewModel && other.classroomSummary == this.classroomSummary
  }

  override fun hashCode() = Objects.hash(classroomSummary)
}
