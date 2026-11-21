package com.example.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.ActionType
import com.example.data.BatterySaverMode
import com.example.data.ListeningState
import com.example.data.RoutineStep
import com.example.data.UiLanguage

data class SaylocStrings(
    val appName: String,
    val tagline: String,
    val navHome: String,
    val navCommands: String,
    val navSettings: String,
    // Home
    val statusIdle: String,
    val statusListening: String,
    val statusDetected: String,
    val statusPausedBattery: String,
    val statusError: String,
    val startListening: String,
    val stopListening: String,
    val lockNow: String,
    val lastHeardLabel: String,
    val lastMatchedLabel: String,
    val waitingForVoice: String,
    val tapOrbHint: String,
    val activeCommandsTitle: String,
    val permissionAttentionTitle: String,
    val permissionAttentionBody: String,
    val checkPermissionsBtn: String,
    val shakeToLockQuick: String,
    val flashlightQuick: String,
    val privacyBadge: String,
    // Commands
    val commandsTitle: String,
    val searchCommandsHint: String,
    val addCommand: String,
    val editCommand: String,
    val noCommandsTitle: String,
    val noCommandsBody: String,
    val testBtn: String,
    val editBtn: String,
    val deleteBtn: String,
    val saveBtn: String,
    val cancelBtn: String,
    val commandNameLabel: String,
    val commandNamePlaceholder: String,
    val aliasesLabel: String,
    val aliasesPlaceholder: String,
    val actionLabel: String,
    val pickAppBtn: String,
    val selectedAppPrefix: String,
    val searchAppsHint: String,
    val routineStepsLabel: String,
    val experimentalWarningShort: String,
    // Settings
    val settingsTitle: String,
    val sectionVoiceLanguage: String,
    val uiLanguageLabel: String,
    val voiceLanguageLabel: String,
    val sensitivityLabel: String,
    val sensitivityStrict: String,
    val sensitivityBalanced: String,
    val sensitivityRelaxed: String,
    val sectionBatteryTriggers: String,
    val batterySaverLabel: String,
    val activeWindowLabel: String,
    val autoLockLabel: String,
    val autoLockOff: String,
    val shakeToLockLabel: String,
    val shakeSensitivityLabel: String,
    val startOnBootLabel: String,
    val startOnBootHint: String,
    val sectionAppearance: String,
    val accentColorLabel: String,
    val themeModeLabel: String,
    val reduceEffectsLabel: String,
    val reduceEffectsHint: String,
    val sectionFeedbackTools: String,
    val soundEffectsLabel: String,
    val hapticsLabel: String,
    val permissionHealthBtn: String,
    val commandHistoryBtn: String,
    val backupJsonBtn: String,
    val restoreJsonBtn: String,
    val rerunOnboardingBtn: String,
    val sectionExperimental: String,
    val experimentalUnlockLabel: String,
    val experimentalUnlockHint: String,
    val experimentalDialogTitle: String,
    val experimentalDialogBody: String,
    val enableAnywayBtn: String,
    // History
    val historyTitle: String,
    val clearHistoryBtn: String,
    val noHistoryText: String,
    // Onboarding
    val skipBtn: String,
    val nextBtn: String,
    val grantBtn: String,
    val grantedBadge: String,
    val finishSetupBtn: String,
    val onboardWelcomeTitle: String,
    val onboardWelcomeBody: String,
    val onboardHonestyTitle: String,
    val onboardHonestyBody: String,
    val onboardMicTitle: String,
    val onboardMicBody: String,
    val onboardNotifTitle: String,
    val onboardNotifBody: String,
    val onboardAdminTitle: String,
    val onboardAdminBody: String,
    val onboardBatteryTitle: String,
    val onboardBatteryBody: String,
    // Permission Health
    val healthTitle: String,
    val healthSubtitle: String,
    val healthMic: String,
    val healthNotif: String,
    val healthAdmin: String,
    val healthBattery: String,
    val healthDnd: String,
    val healthLockType: String,
    val healthSecureLockDetected: String,
    val healthSwipeLockDetected: String,
    val fixBtn: String,
    val readyBadge: String,
    val missingBadge: String,
    val optionalBadge: String,
    // Find My Phone
    val findMyPhoneTitle: String,
    val findMyPhoneBody: String,
    val findMyPhoneDismiss: String
)

fun stringsFor(language: UiLanguage): SaylocStrings = when (language) {
    UiLanguage.EN -> EnglishStrings
    UiLanguage.HI -> HindiStrings
}

val LocalSaylocStrings = staticCompositionLocalOf { EnglishStrings }

fun SaylocStrings.listeningStateLabel(state: ListeningState): String = when (state) {
    ListeningState.IDLE -> statusIdle
    ListeningState.LISTENING -> statusListening
    ListeningState.DETECTED -> statusDetected
    ListeningState.PAUSED_BATTERY -> statusPausedBattery
    ListeningState.ERROR -> statusError
}

fun actionTypeLabel(type: ActionType, lang: UiLanguage): String = when (lang) {
    UiLanguage.EN -> when (type) {
        ActionType.LOCK -> "Lock Phone"
        ActionType.FLASHLIGHT_TOGGLE -> "Flashlight Toggle"
        ActionType.FLASHLIGHT_ON -> "Flashlight On"
        ActionType.FLASHLIGHT_OFF -> "Flashlight Off"
        ActionType.OPEN_APP -> "Open App"
        ActionType.FIND_MY_PHONE -> "Find My Phone"
        ActionType.ROUTINE -> "Routine"
        ActionType.EXPERIMENTAL_UNLOCK -> "Wake / Unlock (Exp.)"
    }
    UiLanguage.HI -> when (type) {
        ActionType.LOCK -> "फ़ोन लॉक करें"
        ActionType.FLASHLIGHT_TOGGLE -> "टॉर्च चालू/बंद"
        ActionType.FLASHLIGHT_ON -> "टॉर्च चालू करें"
        ActionType.FLASHLIGHT_OFF -> "टॉर्च बंद करें"
        ActionType.OPEN_APP -> "ऐप खोलें"
        ActionType.FIND_MY_PHONE -> "मेरा फ़ोन ढूंढें"
        ActionType.ROUTINE -> "रूटीन (कई काम)"
        ActionType.EXPERIMENTAL_UNLOCK -> "वॉइस अनलॉक (प्रयोग)"
    }
}

fun routineStepLabel(step: RoutineStep, lang: UiLanguage): String = when (lang) {
    UiLanguage.EN -> when (step) {
        RoutineStep.ENABLE_DND -> "Do Not Disturb ON"
        RoutineStep.DISABLE_DND -> "Do Not Disturb OFF"
        RoutineStep.FLASHLIGHT_ON -> "Flashlight ON"
        RoutineStep.FLASHLIGHT_OFF -> "Flashlight OFF"
        RoutineStep.LOCK_DEVICE -> "Lock Screen"
        RoutineStep.FIND_MY_PHONE -> "Find My Phone Ring"
    }
    UiLanguage.HI -> when (step) {
        RoutineStep.ENABLE_DND -> "डू नॉट डिस्टर्ब चालू"
        RoutineStep.DISABLE_DND -> "डू नॉट डिस्टर्ब बंद"
        RoutineStep.FLASHLIGHT_ON -> "टॉर्च चालू"
        RoutineStep.FLASHLIGHT_OFF -> "टॉर्च बंद"
        RoutineStep.LOCK_DEVICE -> "स्क्रीन लॉक"
        RoutineStep.FIND_MY_PHONE -> "फ़ोन रिंग अलर्ट"
    }
}

fun batteryModeLabel(mode: BatterySaverMode, lang: UiLanguage): String = when (lang) {
    UiLanguage.EN -> when (mode) {
        BatterySaverMode.ALWAYS -> "Always"
        BatterySaverMode.CHARGING_ONLY -> "Charging Only"
        BatterySaverMode.TIME_WINDOW -> "Time Window"
    }
    UiLanguage.HI -> when (mode) {
        BatterySaverMode.ALWAYS -> "हमेशा"
        BatterySaverMode.CHARGING_ONLY -> "चार्जिंग पर"
        BatterySaverMode.TIME_WINDOW -> "समय अनुसार"
    }
}

private val EnglishStrings = SaylocStrings(
    appName = "Sayloc",
    tagline = "Say it. Lock it.",
    navHome = "Home",
    navCommands = "Commands",
    navSettings = "Settings",
    statusIdle = "Standby",
    statusListening = "Listening On-Device",
    statusDetected = "Command Matched!",
    statusPausedBattery = "Battery Saver Paused",
    statusError = "Mic Engine Notice",
    startListening = "Start Listening",
    stopListening = "Stop Listening",
    lockNow = "Lock Now",
    lastHeardLabel = "LAST HEARD",
    lastMatchedLabel = "MATCHED",
    waitingForVoice = "Speak a saved command word…",
    tapOrbHint = "Tap orb to toggle listening",
    activeCommandsTitle = "Active Voice Triggers",
    permissionAttentionTitle = "Setup Incomplete",
    permissionAttentionBody = "Grant remaining permissions so lock & background voice work smoothly.",
    checkPermissionsBtn = "Health Check",
    shakeToLockQuick = "Shake Lock",
    flashlightQuick = "Flashlight",
    privacyBadge = "100% On-Device • Zero Cloud",
    commandsTitle = "Voice Commands",
    searchCommandsHint = "Search commands or aliases…",
    addCommand = "New Command",
    editCommand = "Edit Command",
    noCommandsTitle = "No commands yet",
    noCommandsBody = "Tap New Command to add a secret voice trigger like RIYA, AASHU, or BADSHAH.",
    testBtn = "Test",
    editBtn = "Edit",
    deleteBtn = "Delete",
    saveBtn = "Save Command",
    cancelBtn = "Cancel",
    commandNameLabel = "Voice Trigger Word",
    commandNamePlaceholder = "e.g. RIYA, AASHU, BADSHAH",
    aliasesLabel = "Extra Spellings / Hinglish Aliases (optional)",
    aliasesPlaceholder = "e.g. Ashu, Aashoo, आशु",
    actionLabel = "Mapped Action",
    pickAppBtn = "Choose Installed App",
    selectedAppPrefix = "Selected App:",
    searchAppsHint = "Filter installed apps…",
    routineStepsLabel = "Routine Steps (runs in order)",
    experimentalWarningShort = "Works ONLY if phone has no PIN, pattern, or fingerprint lock.",
    settingsTitle = "Settings",
    sectionVoiceLanguage = "Voice & Language",
    uiLanguageLabel = "App Interface Language",
    voiceLanguageLabel = "Voice Recognition Language",
    sensitivityLabel = "Fuzzy Match Sensitivity",
    sensitivityStrict = "Strict",
    sensitivityBalanced = "Balanced",
    sensitivityRelaxed = "Relaxed",
    sectionBatteryTriggers = "Battery & Smart Triggers",
    batterySaverLabel = "Listening Schedule",
    activeWindowLabel = "Active Time Window",
    autoLockLabel = "Auto-Lock Inactivity Timer",
    autoLockOff = "Off",
    shakeToLockLabel = "Shake Phone to Lock",
    shakeSensitivityLabel = "Shake Sensitivity",
    startOnBootLabel = "Resume After Restart",
    startOnBootHint = "Auto-starts on older Android; posts one-tap resume alert on Android 14+.",
    sectionAppearance = "3D Glass Appearance",
    accentColorLabel = "Glow Accent Palette",
    themeModeLabel = "Theme Mode",
    reduceEffectsLabel = "Reduce Effects (Low-End Smooth Mode)",
    reduceEffectsHint = "Turns off parallax tilt, heavy glows, and pulsing animations.",
    sectionFeedbackTools = "Sound, Haptics & Data",
    soundEffectsLabel = "Synthesized Sound Effects",
    hapticsLabel = "Haptic Feedback",
    permissionHealthBtn = "Permission Health-Check",
    commandHistoryBtn = "Command History Log",
    backupJsonBtn = "Backup JSON",
    restoreJsonBtn = "Restore JSON",
    rerunOnboardingBtn = "Re-open Setup Guide",
    sectionExperimental = "Experimental & Honesty",
    experimentalUnlockLabel = "Experimental Voice Unlock",
    experimentalUnlockHint = "Android blocks bypassing PIN/Pattern/Biometrics. Only wakes swipe-only lock screens.",
    experimentalDialogTitle = "Important Android Security Limit",
    experimentalDialogBody = "No third-party app can bypass a PIN, password, pattern, or fingerprint lock on Android. Experimental Voice Unlock can ONLY wake the screen and dismiss a swipe-only (no security) lock screen. Enable only if you understand this limit.",
    enableAnywayBtn = "I Understand — Enable",
    historyTitle = "Command History",
    clearHistoryBtn = "Clear Log",
    noHistoryText = "No voice commands or locks recorded yet.",
    skipBtn = "Skip",
    nextBtn = "Next",
    grantBtn = "Enable Now",
    grantedBadge = "Enabled",
    finishSetupBtn = "Launch Sayloc",
    onboardWelcomeTitle = "Welcome to Sayloc",
    onboardWelcomeBody = "Control your phone with your own secret voice words — completely offline and private. Audio never leaves your device.",
    onboardHonestyTitle = "Honest Lock & Unlock Notice",
    onboardHonestyBody = "Voice Lock works on every phone once Device Admin is enabled. However, Android forbids any third-party app from bypassing PIN, pattern, or fingerprint locks — so Voice Unlock is only an optional experimental feature for phones with no secure lock.",
    onboardMicTitle = "1. Microphone Access",
    onboardMicBody = "Needed so Sayloc can listen locally for your saved voice command words.",
    onboardNotifTitle = "2. Notifications",
    onboardNotifBody = "Shows when Sayloc is listening in the background and provides quick Lock / Stop controls.",
    onboardAdminTitle = "3. Screen Lock (Device Admin)",
    onboardAdminBody = "Requests force-lock permission ONLY so Sayloc can lock your screen instantly by voice, shake, or tile.",
    onboardBatteryTitle = "4. Battery Optimization",
    onboardBatteryBody = "Prevents Android from silently killing background voice listening while the screen is off.",
    healthTitle = "Permission Health-Check",
    healthSubtitle = "Keep every item green for reliable background voice control.",
    healthMic = "Microphone (Voice Listening)",
    healthNotif = "Notifications (Foreground Status)",
    healthAdmin = "Device Admin (Force-Lock Only)",
    healthBattery = "Battery Exemption (Keep Alive)",
    healthDnd = "Do Not Disturb Access (Routines)",
    healthLockType = "Phone Lock Security Status",
    healthSecureLockDetected = "PIN / Pattern / Biometric active — Voice Lock works; Voice Unlock blocked by Android.",
    healthSwipeLockDetected = "No secure lock screen — Experimental Voice Unlock is possible.",
    fixBtn = "Fix",
    readyBadge = "Ready",
    missingBadge = "Needed",
    optionalBadge = "Optional",
    findMyPhoneTitle = "Find My Phone Active!",
    findMyPhoneBody = "Ringing at max alarm volume & blinking flashlight.",
    findMyPhoneDismiss = "I Found It — Stop Alert"
)

private val HindiStrings = SaylocStrings(
    appName = "Sayloc",
    tagline = "बोलो और लॉक करो",
    navHome = "होम",
    navCommands = "कमांड्स",
    navSettings = "सेटिंग्स",
    statusIdle = "बंद है",
    statusListening = "फ़ोन पर सुन रहा है",
    statusDetected = "कमांड मिल गया!",
    statusPausedBattery = "बैटरी सेवर से रुका है",
    statusError = "माइक सूचना",
    startListening = "सुनना शुरू करें",
    stopListening = "सुनना बंद करें",
    lockNow = "अभी लॉक करें",
    lastHeardLabel = "आखिरी सुना शब्द",
    lastMatchedLabel = "मैच हुआ",
    waitingForVoice = "अपना सेव किया शब्द बोलें…",
    tapOrbHint = "सुनना शुरू/बंद करने के लिए ऑर्ब छुएं",
    activeCommandsTitle = "सक्रिय वॉइस कमांड्स",
    permissionAttentionTitle = "सेटअप अधूरा है",
    permissionAttentionBody = "बैकग्राउंड लॉक और आवाज़ के लिए बाकी अनुमतियाँ चालू करें।",
    checkPermissionsBtn = "जांच करें",
    shakeToLockQuick = "शेक लॉक",
    flashlightQuick = "टॉर्च",
    privacyBadge = "100% ऑफ़लाइन • पूरा डेटा फ़ोन में",
    commandsTitle = "वॉइस कमांड्स",
    searchCommandsHint = "कमांड या नाम खोजें…",
    addCommand = "नया कमांड",
    editCommand = "कमांड बदलें",
    noCommandsTitle = "कोई कमांड नहीं है",
    noCommandsBody = "RIYA, AASHU या BADSHAH जैसा अपना वॉइस कमांड जोड़ने के लिए नया कमांड दबाएँ।",
    testBtn = "टेस्ट",
    editBtn = "एडिट",
    deleteBtn = "हटाएं",
    saveBtn = "सेव करें",
    cancelBtn = "रद्द करें",
    commandNameLabel = "वॉइस कमांड शब्द",
    commandNamePlaceholder = "जैसे RIYA, AASHU, BADSHAH",
    aliasesLabel = "अन्य स्पेलिंग / हिंग्लिश नाम (वैकल्पिक)",
    aliasesPlaceholder = "जैसे Ashu, Aashoo, आशु",
    actionLabel = "क्या काम करना है",
    pickAppBtn = "इंस्टॉल ऐप चुनें",
    selectedAppPrefix = "चुना गया ऐप:",
    searchAppsHint = "ऐप खोजें…",
    routineStepsLabel = "रूटीन के काम (क्रम से चलेंगे)",
    experimentalWarningShort = "केवल बिना PIN/पैटर्न/फिंगरप्रिंट वाले फ़ोन पर ही काम करता है।",
    settingsTitle = "सेटिंग्स",
    sectionVoiceLanguage = "आवाज़ और भाषा",
    uiLanguageLabel = "ऐप की भाषा",
    voiceLanguageLabel = "आवाज़ पहचान की भाषा",
    sensitivityLabel = "मैच सेंसिटिविटी",
    sensitivityStrict = "सख्त",
    sensitivityBalanced = "संतुलित",
    sensitivityRelaxed = "आसान",
    sectionBatteryTriggers = "बैटरी और स्मार्ट ट्रिगर",
    batterySaverLabel = "सुनने का समय",
    activeWindowLabel = "एक्टिव टाइम विंडो",
    autoLockLabel = "ऑटो-लॉक टाइमर",
    autoLockOff = "बंद",
    shakeToLockLabel = "फ़ोन हिलाकर लॉक करें",
    shakeSensitivityLabel = "शेक सेंसिटिविटी",
    startOnBootLabel = "रीस्टार्ट के बाद चालू करें",
    startOnBootHint = "पुराने Android पर ऑटो शुरू; Android 14+ पर एक टैप नोटिफिकेशन।",
    sectionAppearance = "3D ग्लास लुक",
    accentColorLabel = "ग्लो एक्सेंट रंग",
    themeModeLabel = "थीम मोड",
    reduceEffectsLabel = "इफेक्ट्स कम करें (स्मूद मोड)",
    reduceEffectsHint = "कमज़ोर फ़ोन के लिए टिल्ट, ग्लो और भारी एनीमेशन बंद करता है।",
    sectionFeedbackTools = "साउंड, वाइब्रेशन और डेटा",
    soundEffectsLabel = "साउंड इफेक्ट्स",
    hapticsLabel = "वाइब्रेशन (हैप्टिक्स)",
    permissionHealthBtn = "परमिशन हेल्थ-चेक",
    commandHistoryBtn = "कमांड हिस्ट्री लॉग",
    backupJsonBtn = "बैकअप JSON",
    restoreJsonBtn = "रीस्टोर JSON",
    rerunOnboardingBtn = "सेटअप गाइड फिर खोलें",
    sectionExperimental = "प्रयोगात्मक और सच्चाई",
    experimentalUnlockLabel = "वॉइस अनलॉक (प्रयोग)",
    experimentalUnlockHint = "Android PIN/पैटर्न/फिंगरप्रिंट हटाने नहीं देता। केवल स्वाइप लॉक पर चलता है।",
    experimentalDialogTitle = "ज़रूरी Android सुरक्षा जानकारी",
    experimentalDialogBody = "कोई भी थर्ड-पार्टी ऐप Android का PIN, पैटर्न या फिंगरप्रिंट लॉक नहीं खोल सकता। यह ऑप्शन केवल बिना सिक्योर लॉक (स्वाइप-ओनली) वाले फ़ोन की स्क्रीन जगाने के लिए है।",
    enableAnywayBtn = "समझ गया — चालू करें",
    historyTitle = "कमांड हिस्ट्री",
    clearHistoryBtn = "लॉग साफ़ करें",
    noHistoryText = "अभी तक कोई कमांड या लॉक रिकॉर्ड नहीं हुआ है।",
    skipBtn = "छोड़ें",
    nextBtn = "आगे बढ़ें",
    grantBtn = "अभी चालू करें",
    grantedBadge = "चालू है",
    finishSetupBtn = "Sayloc शुरू करें",
    onboardWelcomeTitle = "Sayloc में स्वागत है",
    onboardWelcomeBody = "अपनी आवाज़ के शब्दों से फ़ोन लॉक और कंट्रोल करें — पूरी तरह ऑफ़लाइन और प्राइवेट। आपकी आवाज़ कभी फ़ोन से बाहर नहीं जाती।",
    onboardHonestyTitle = "लॉक और अनलॉक की सच्चाई",
    onboardHonestyBody = "Device Admin चालू करने पर वॉइस लॉक हर फ़ोन में चलता है। लेकिन Android सुरक्षा के कारण कोई भी ऐप PIN, पैटर्न या फिंगरप्रिंट नहीं खोल सकता — वॉइस अनलॉक सिर्फ बिना लॉक वाले फ़ोन में प्रयोग के तौर पर उपलब्ध है।",
    onboardMicTitle = "1. माइक्रोफ़ोन अनुमति",
    onboardMicBody = "ताकि Sayloc आपके सेव किए हुए वॉइस कमांड्स सुन सके।",
    onboardNotifTitle = "2. नोटिफिकेशन अनुमति",
    onboardNotifBody = "बैकग्राउंड में सुनने की स्थिति और तुरंत लॉक/स्टॉप बटन दिखाने के लिए।",
    onboardAdminTitle = "3. स्क्रीन लॉक (Device Admin)",
    onboardAdminBody = "सिर्फ फ़ोन लॉक करने की अनुमति लेता है ताकि आवाज़ या शेक से स्क्रीन तुरंत लॉक हो सके।",
    onboardBatteryTitle = "4. बैटरी ऑप्टिमाइज़ेशन छूट",
    onboardBatteryBody = "ताकि Android बैकग्राउंड में वॉइस सर्विस को बंद न करे।",
    healthTitle = "परमिशन हेल्थ-चेक",
    healthSubtitle = "बेहतर बैकग्राउंड वॉइस कंट्रोल के लिए सभी जरूरी आइटम हरे रखें।",
    healthMic = "माइक्रोफ़ोन (वॉइस लिसनिंग)",
    healthNotif = "नोटिफिकेशन (बैकग्राउंड स्टेटस)",
    healthAdmin = "Device Admin (सिर्फ लॉक)",
    healthBattery = "बैटरी छूट (लगातार चलने के लिए)",
    healthDnd = "डू नॉट डिस्टर्ब अनुमति (रूटीन)",
    healthLockType = "फ़ोन लॉक सुरक्षा स्थिति",
    healthSecureLockDetected = "PIN / पैटर्न / फिंगरप्रिंट चालू है — वॉइस लॉक चलेगा, अनलॉक नहीं।",
    healthSwipeLockDetected = "कोई सिक्योर लॉक नहीं — प्रयोगात्मक वॉइस अनलॉक संभव है।",
    fixBtn = "ठीक करें",
    readyBadge = "तैयार",
    missingBadge = "ज़रूरी",
    optionalBadge = "वैकल्पिक",
    findMyPhoneTitle = "मेरा फ़ोन ढूंढें चालू है!",
    findMyPhoneBody = "फ़ुल वॉल्यूम अलार्म और टॉर्च ब्लिंक चल रहा है।",
    findMyPhoneDismiss = "फ़ोन मिल गया — बंद करें"
)
