# Implement Pomodoro Task Flow with AI Recommendations

This plan outlines the steps to add a Pomodoro task creation flow where Gemini AI recommends work and break times based on the task description.

## User Review Required

> [!IMPORTANT]
> A Gemini API Key is required for the AI recommendation feature to work. You will need to provide this key in a secure way (e.g., `local.properties` or a secrets file). I will use a placeholder for now.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/gradle/libs.versions.toml)
- Add the Google AI SDK dependency.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/build.gradle.kts)
- Add the Generative AI library.

### UI & UX

#### [MODIFY] [activity_home.xml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/res/layout/activity_home.xml)
- Add a Floating Action Button (FAB) to initiate task creation.

#### [NEW] [dialog_add_task.xml](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/res/layout/dialog_add_task.xml)
- Create a layout for the "Add Task" dialog, including:
    - Input for task description.
    - "Ask AI" button.
    - Work time and Break time inputs (editable).
    - "Add Task" button.

### Logic & Data

#### [NEW] [PomodoroTask.java](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/java/dev/jaypee/anotherpomodoroapp/PomodoroTask.java)
- A data model for Pomodoro tasks.

#### [NEW] [GeminiRecommendationService.java](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/java/dev/jaypee/anotherpomodoroapp/GeminiRecommendationService.java)
- A service to handle interactions with the Gemini API.

#### [MODIFY] [HomeActivity.java](file:///C:/Users/Angela/StudioProjects/AnotherPomodoroApp/app/src/main/java/dev/jaypee/anotherpomodoroapp/HomeActivity.java)
- Implement the logic to show the "Add Task" dialog and handle the AI recommendation flow.

## Verification Plan

### Manual Verification
1. Launch the app and go to the Home screen.
2. Tap the "Add Task" FAB.
3. Enter a task description (e.g., "Write a blog post about Android").
4. Tap "Ask AI".
5. Verify that AI-recommended times appear in the inputs.
6. Modify the times manually if needed.
7. Save the task and verify it appears (even if just a Toast for now).
