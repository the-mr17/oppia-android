package org.oppia.android.app.classroom

/** Listener for when the active classroom should be switched. */
interface SwitchClassroomListener {
  fun switchClassroom(classroomId: String)
}
