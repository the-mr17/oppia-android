package org.oppia.android.app.home.classroomlist

import androidx.appcompat.app.AppCompatActivity
import java.util.*
import org.oppia.android.R
import org.oppia.android.app.home.HomeItemViewModel

/** [ViewModel] for the classroom list displayed in [ClassroomListFragment]. */
class ClassroomSummaryListViewModel(
  activity: AppCompatActivity,
  val classroomSummaryList: List<ClassroomSummaryViewModel>
): HomeItemViewModel() {
  val endPadding =
    if (classroomSummaryList.size > 1)
      activity.resources.getDimensionPixelSize(R.dimen.home_padding_end)
    else activity.resources.getDimensionPixelSize(R.dimen.home_padding_start)

  // Overriding equals is needed so that DataProvider combine functions used in the HomeViewModel
  // will only rebind when the actual data in the data list changes, rather than when the ViewModel
  // object changes.
  override fun equals(other: Any?): Boolean {
    return other is ClassroomSummaryListViewModel && other.classroomSummaryList == this.classroomSummaryList
  }

  override fun hashCode() = Objects.hash(classroomSummaryList)
}
