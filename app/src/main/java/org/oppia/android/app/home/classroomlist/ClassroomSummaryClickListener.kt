package org.oppia.android.app.home.classroomlist

import org.oppia.android.app.model.ClassroomSummary

/** Listener for when the active classroom should be switched. */
interface ClassroomSummaryClickListener {
  fun onClassroomSummaryClicked(classroomSummary: ClassroomSummary)
}
