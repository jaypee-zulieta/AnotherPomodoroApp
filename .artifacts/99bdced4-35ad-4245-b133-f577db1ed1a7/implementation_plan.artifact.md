# Remove Timer Feature Implementation Plan

Remove the Pomodoro timer functionality, including activities, services, timer navigation, and timer start buttons on tasks.

## User Review Required

> [!IMPORTANT]
> This plan completely removes the timer feature (`TimerActivity`, `PomodoroService`, timer navigation buttons, and start timer buttons on task items). Tasks will remain as task management items without timer execution.

## Proposed Changes

### Component 1: Activities & Services Deletion
#### [DELETE] [TimerActivity.java](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/java/dev/jaypee/anotherpomodoroapp/TimerActivity.java)
#### [DELETE] [PomodoroService.java](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/java/dev/jaypee/anotherpomodoroapp/PomodoroService.java)

### Component 2: Layouts & Resources Deletion
#### [DELETE] [activity_timer.xml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/res/layout/activity_timer.xml)
#### [DELETE] [bg_circle_timer_bg.xml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/res/drawable/bg_circle_timer_bg.xml)
#### [DELETE] [bg_circle_timer_progress.xml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/res/drawable/bg_circle_timer_progress.xml)

### Component 3: UI & Manifest Updates
#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/AndroidManifest.xml)
- Remove `TimerActivity` declaration.
- Remove `VIBRATE` permission if no longer needed (or keep if used elsewhere).

#### [MODIFY] [HomeActivity.java](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/java/dev/jaypee/anotherpomodoroapp/HomeActivity.java)
- Remove `onTaskStartTimer` implementation and timer navigation button click listener (`btn_nav_timer`).

#### [MODIFY] [TaskAdapter.java](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/java/dev/jaypee/anotherpomodoroapp/TaskAdapter.java)
- Remove `onTaskStartTimer` callback from interface and start timer button click listener from view holder.

#### [MODIFY] [activity_home.xml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/res/layout/activity_home.xml)
- Remove the timer navigation button (`btn_nav_timer`).

#### [MODIFY] [item_task.xml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/res/layout/item_task.xml)
- Remove the start timer button (`btn_start_timer`).

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to ensure no compilation errors remain after removing classes and references.

### Manual Verification
- Verify the app builds and runs successfully, with tasks displayed without timer buttons and home screen without the timer navigation button.
