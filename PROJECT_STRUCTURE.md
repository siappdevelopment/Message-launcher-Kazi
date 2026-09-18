# Project Structure

## 1. Folder Structure
The project follows a standard Android Gradle structure with a single `:app` module.

```text
Messages/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/messages/smartsms/
│   │   │   │   ├── activities/      # UI entry points (Screens)
│   │   │   │   ├── adapters/        # RecyclerView and ViewPager adapters
│   │   │   │   ├── common/          # Shared utilities, ad management, and navigation
│   │   │   │   ├── dialogs/         # Custom dialog implementations
│   │   │   │   ├── fragments/       # Modular UI components
│   │   │   │   ├── helpers/         # Business logic and data manipulation helpers
│   │   │   │   ├── interfaces/      # Callback and listener definitions
│   │   │   │   ├── models/          # Data classes and POJOs
│   │   │   │   └── services/        # Background services and BroadcastReceivers
│   │   │   │   └── MyApplication.java # Application class for global initialization
│   │   │   ├── res/                 # Android resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml  # App configuration and component declaration
│   │   ├── test/                    # Unit tests (Not currently implemented)
│   │   └── androidTest/             # Instrumentation tests (Not currently implemented)
│   ├── build.gradle                 # Module-level Gradle configuration
│   └── google-services.json         # Firebase configuration
├── gradle/
│   └── libs.versions.toml           # Version Catalog for dependencies
├── build.gradle                     # Project-level Gradle configuration
└── settings.gradle                  # Sub-project inclusion
```

### Folder Purposes:
- **activities/**: Contains all Activity classes, each representing a distinct screen in the app.
- **fragments/**: Contains Fragment classes used for modular UI parts, especially within `MainActivity` and `LauncherHomeActivity`.
- **helpers/**: Contains utility-like classes that handle specific logic like SMS syncing, archiving, blocking, and remote config.
- **common/**: Houses shared logic such as `AdPlacement` for ad management and `ScreenFlowNavigator` for onboarding flow.
- **services/**: Handles background tasks like SMS/MMS reception, call state monitoring, and Firebase messaging.
- **models/**: Defines the data structure for messages, contacts, news, etc.

## 2. Coding Structure
The code is organized into a functional package structure. It does not strictly follow a specific architecture like MVVM or Clean Architecture but uses a "Classic Android" approach where Activities and Fragments interact with Helper classes.

### Core Components:
- **Activities**:
  - `SplashActivity`: Initial entry point, handles Remote Config fetching.
  - `MainActivity`: Main container for messaging and contact fragments.
  - `MessagesContentActivity`: Conversation view for a specific thread.
  - `LauncherHomeActivity`: Provides custom launcher functionality.
  - `ClEndActivity`: Displayed after a phone call ends.
- **Fragments**:
  - `MessagesFragment`: Displays the list of message threads with category filtering.
  - `ContactsFragment`: Displays the contact list.
  - `LauncherHomeFragment`: Main screen for the launcher functionality.
- **Adapters**:
  - `MessagesAdapter`: Binds message threads to the RecyclerView.
  - `MessagesContentAdapter`: Binds individual messages in a conversation.
  - `MessagesCategoryAdapter`: Handles the horizontal category tabs (All, Unread, Personal, etc.).
- **Helpers/Managers**:
  - `RemoteConfigHelper`: Manages dynamic configuration from Firebase, primarily for ads.
  - `SmsUnreadHelper`: Handles unread message counts and marking as read.
  - `ArchiveHelper`, `BlockHelper`, `RecycleBinHelper`: Manage message filtering and storage states.
  - `AdPlacement`: Centralized manager for AdMob and Facebook ad logic.
- **Services/Receivers**:
  - `SMSReceiver` & `MMSReceiver`: Intercept incoming messages.
  - `PhoneCallStateService`: Monitors call states to trigger the call-end screen.
  - `MyFirebaseMessagingService`: Handles push notifications.

## 3. Project Architecture
The project uses a **Classic Android Architecture** with logic delegated to Helper and Manager classes.

- **UI Layer**: Uses Activities and Fragments. Navigation is managed manually via `Intent` and `FragmentManager`, often guided by `ScreenFlowNavigator` during onboarding.
- **Business/Data Layer**: Logic is encapsulated in `Helper` classes (e.g., `ArchiveHelper`, `MessageListSyncHelper`). There is no explicit Repository pattern.
- **Data Flow**: Data is typically queried directly from Content Providers (like `content://sms` and `content://contacts`) using `Cursor` within Fragments or Helpers.
- **Dependency Injection**: Not currently implemented.
- **Firebase/Remote Config**: Heavily used for dynamic ad configuration and controlling the onboarding flow. `RemoteConfigHelper` fetches a JSON configuration that `AdPlacement` then applies.
- **Ad Configuration**: Managed through `AdPlacement`, supporting AdMob and Facebook Audience Network. It handles Interstitials, Native ads, and App Open ads.
- **Navigation**: Controlled by `ScreenFlowNavigator` during the initial setup (Language -> Collection -> Default Apps -> Intro). Subsequent navigation is Intent-based.
- **Local Storage**: Uses `SharedPreferences` via `Utils` for app settings and state. Message states (archived/blocked) are tracked via thread IDs stored in preferences.

## 4. Testing Structure
The project includes the default Android testing directories, but no custom tests are implemented.

- **Unit tests** (`app/src/test`): Not currently implemented.
- **Instrumentation tests** (`app/src/androidTest`): Not currently implemented.
- **UI tests**: Not currently implemented.
- **Testing frameworks**: JUnit and Espresso dependencies are present in `build.gradle`, but no test cases are defined.

## 5. Important Project Flow

### App Startup / Onboarding Flow
1. `SplashActivity` starts and fetches Firebase Remote Config via `RemoteConfigHelper`.
2. `ScreenFlowNavigator` determines the next step based on completed states:
   - `LanguageActivity` (Language selection)
   - `CollectionActivity` (Data collection consent)
   - `DefaultSMSActivity` (Request to become default SMS app)
   - `DefaultAppActivity` (Request to become default Home app/Launcher)
   - `IntroSwipeActivity` or `Intro1-4Activity` (Feature introduction)
3. Finally, navigates to `LauncherHomeActivity`.

### Messaging Flow
- **Message List**: `MessagesFragment` queries the SMS content provider, filters messages into categories (Personal, Transaction, etc.), and displays them.
- **Message Sync**: `MessageListSyncHelper` and `SMSReceiver` handle real-time updates when new messages arrive.
- **Conversation**: `MessagesContentActivity` displays the full chat history for a thread and allows sending new messages via `SmsSendHelper`.

### Call-End Flow
1. `PhoneCallStateService` detects when a call ends.
2. `CallEndReceiver` or `CallEndLaunchHelper` triggers the `ClEndActivity`.
3. `ClEndActivity` shows call details along with configured advertisements.

### Default SMS App Flow
- `DefaultSMSActivity` prompts the user to set the app as the system's default SMS handler.
- Checks are performed using `RoleManager` (Android 10+) or `Telephony.Sms.getDefaultSmsPackage()`.

## 6. Dependency & Technology Overview
- **Android SDK**: Target SDK 37, Min SDK 24.
- **Language**: Java 11.
- **AndroidX**: AppCompat, Activity, Lifecycle (Process).
- **Architecture Components**: Lifecycle observers for app-wide state tracking.
- **Firebase**: Remote Config, Analytics, Messaging (FCM), Crashlytics.
- **Ads**: Google Mobile Ads (AdMob), Facebook Audience Network.
- **Networking**: Retrofit 3.0.0, GSON.
- **Image Loading**: Glide 5.0.7.
- **Animations**: Lottie.
- **UI Scaling**: SDP (Scalable DP) for responsive layouts.
- **Installation Tracking**: Install Referrer API.

## 7. Development Guidelines

### Adding New Components
- **Activities**: Place in `com.messages.smart.sms.activities`. Register in `AndroidManifest.xml` with appropriate orientation (usually portrait).
- **Fragments**: Place in `com.messages.smart.sms.fragments`.
- **Adapters**: Place in `com.messages.smart.sms.adapters`.
- **Models**: Add POJO/Data classes to `com.messages.smart.sms.models`.
- **Utilities/Helpers**: Reusable logic should go into `com.messages.smart.sms.helpers` or `com.messages.smart.sms.common.Utils`.

### Naming Conventions
- **Classes**: PascalCase (e.g., `NewFeatureActivity`, `SmsDataHelper`).
- **Layouts**: prefix with component type (e.g., `activity_new_feature.xml`, `fragment_message_list.xml`, `adapter_message_item.xml`).
- **Resources**: Use descriptive names (e.g., `ic_send_message`, `custom_button_bg`).
- **Strings**: Define in `res/values/strings.xml` and ensure translations are updated in corresponding `values-xx` folders.

### Resource Organization
- Keep layouts clean and use the `sdp` library for dimensions to ensure compatibility across screen sizes.
- Ad-related layouts should follow the `qz_` or `native_` naming prefix where applicable to match existing patterns.

### Testing
- When implementing tests, follow the `androidx.test` conventions. Place unit tests in `src/test` and UI tests in `src/androidTest`.

## 8. Project Structure Summary
The **Messages** project is a feature-rich SMS and Launcher application. It is structured around a centralized `app` module with a clear separation of UI (`activities`, `fragments`) and business logic (`helpers`). The application heavily relies on **Firebase Remote Config** to dynamically control its UI flow and monetization (Ads). While it doesn't use a modern reactive architecture like MVVM, it maintains order through specific helper classes for messaging operations (Syncing, Filtering, Sending) and a dedicated navigation helper (`ScreenFlowNavigator`) for onboarding.
