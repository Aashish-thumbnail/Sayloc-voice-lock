package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ActionType
import com.example.data.InstalledAppInfo
import com.example.data.RoutineStep
import com.example.data.UiLanguage
import com.example.data.VoiceCommand
import com.example.system.InstalledAppsLoader
import com.example.ui.components.Glass3DButton
import com.example.ui.components.Glass3DCard
import com.example.ui.components.Glass3DChip
import com.example.ui.components.GlassButtonStyle
import com.example.ui.i18n.LocalSaylocStrings
import com.example.ui.i18n.actionTypeLabel
import com.example.ui.i18n.routineStepLabel
import com.example.ui.theme.LocalSaylocPalette
import com.example.ui.theme.LockCrimson
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandEditorScreen(
    initialCommand: VoiceCommand?,
    uiLanguage: UiLanguage,
    experimentalUnlockEnabled: Boolean,
    onSave: (VoiceCommand) -> Unit,
    onCancel: () -> Unit,
    onTapFeedback: () -> Unit
) {
    BackHandler(onBack = onCancel)
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current
    val context = LocalContext.current

    var name by remember(initialCommand) { mutableStateOf(initialCommand?.name ?: "") }
    var aliasesText by remember(initialCommand) {
        mutableStateOf(initialCommand?.aliases?.joinToString(", ") ?: "")
    }
    var selectedAction by remember(initialCommand) {
        mutableStateOf(initialCommand?.actionType ?: ActionType.LOCK)
    }
    var targetPackage by remember(initialCommand) { mutableStateOf(initialCommand?.targetPackage) }
    var targetAppLabel by remember(initialCommand) { mutableStateOf(initialCommand?.targetAppLabel) }
    var routineSteps by remember(initialCommand) {
        mutableStateOf(
            initialCommand?.routineSteps?.ifEmpty {
                listOf(RoutineStep.ENABLE_DND, RoutineStep.LOCK_DEVICE)
            } ?: listOf(RoutineStep.ENABLE_DND, RoutineStep.LOCK_DEVICE)
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var appSearch by remember { mutableStateOf("") }

    LaunchedEffect(selectedAction) {
        if (selectedAction == ActionType.OPEN_APP && installedApps.isEmpty()) {
            installedApps = InstalledAppsLoader.loadLaunchableApps(context)
        }
    }

    val availableActions = remember(experimentalUnlockEnabled) {
        ActionType.entries.filter {
            it != ActionType.EXPERIMENTAL_UNLOCK || experimentalUnlockEnabled
        }
    }

    val filteredApps = remember(installedApps, appSearch) {
        val q = appSearch.trim().lowercase()
        if (q.isEmpty()) installedApps
        else installedApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("editor_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.cancelBtn,
                        tint = palette.textPrimary
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (initialCommand == null) strings.addCommand else strings.editCommand,
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Trigger word + aliases card
                Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.commandNameLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        placeholder = { Text(strings.commandNamePlaceholder, color = palette.textSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.rimGlow,
                            unfocusedBorderColor = palette.cardBorderTop,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_name_input")
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = strings.aliasesLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aliasesText,
                        onValueChange = { aliasesText = it },
                        placeholder = { Text(strings.aliasesPlaceholder, color = palette.textSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.rimGlow,
                            unfocusedBorderColor = palette.cardBorderTop,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_aliases_input")
                    )
                }

                // Action selector card
                Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.actionLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableActions.forEach { action ->
                            Glass3DChip(
                                label = actionTypeLabel(action, uiLanguage),
                                selected = selectedAction == action,
                                onClick = {
                                    onTapFeedback()
                                    selectedAction = action
                                    validationError = null
                                },
                                testTag = "editor_action_${action.key}"
                            )
                        }
                    }

                    if (selectedAction == ActionType.EXPERIMENTAL_UNLOCK) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.experimentalWarningShort,
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningAmber
                        )
                    }
                }

                // Open App Picker Card
                if (selectedAction == ActionType.OPEN_APP) {
                    Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (targetAppLabel != null) {
                                "${strings.selectedAppPrefix} $targetAppLabel"
                            } else {
                                strings.pickAppBtn
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.rimGlow
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = appSearch,
                            onValueChange = { appSearch = it },
                            placeholder = { Text(strings.searchAppsHint, color = palette.textSecondary) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.rimGlow)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.rimGlow,
                                unfocusedBorderColor = palette.cardBorderTop,
                                focusedTextColor = palette.textPrimary,
                                unfocusedTextColor = palette.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { appInfo ->
                                val isPicked = appInfo.packageName == targetPackage
                                Glass3DChip(
                                    label = appInfo.label,
                                    selected = isPicked,
                                    icon = if (isPicked) Icons.Rounded.Check else Icons.Rounded.Apps,
                                    onClick = {
                                        onTapFeedback()
                                        targetPackage = appInfo.packageName
                                        targetAppLabel = appInfo.label
                                        validationError = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Routine Builder Card
                if (selectedAction == ActionType.ROUTINE) {
                    Glass3DCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.routineStepsLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoutineStep.entries.forEach { step ->
                                val selected = step in routineSteps
                                Glass3DChip(
                                    label = routineStepLabel(step, uiLanguage),
                                    selected = selected,
                                    icon = if (selected) Icons.Rounded.Check else null,
                                    onClick = {
                                        onTapFeedback()
                                        routineSteps = if (selected) {
                                            routineSteps - step
                                        } else {
                                            routineSteps + step
                                        }
                                        validationError = null
                                    },
                                    testTag = "editor_routine_${step.key}"
                                )
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LockCrimson
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            // Save / Cancel Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Glass3DButton(
                    text = strings.cancelBtn,
                    style = GlassButtonStyle.SECONDARY,
                    onClick = onCancel,
                    onTapFeedback = onTapFeedback,
                    modifier = Modifier.weight(1f),
                    testTag = "editor_cancel_btn"
                )
                Glass3DButton(
                    text = strings.saveBtn,
                    icon = Icons.Rounded.Save,
                    style = GlassButtonStyle.PRIMARY,
                    onClick = {
                        val trimmed = name.trim()
                        when {
                            trimmed.isEmpty() -> {
                                validationError = "Please enter a trigger word (e.g. RIYA)."
                            }
                            selectedAction == ActionType.OPEN_APP && targetPackage.isNullOrBlank() -> {
                                validationError = "Please pick an installed app to open."
                            }
                            selectedAction == ActionType.ROUTINE && routineSteps.isEmpty() -> {
                                validationError = "Please choose at least one routine step."
                            }
                            else -> {
                                val aliases = aliasesText.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                onSave(
                                    VoiceCommand(
                                        id = initialCommand?.id ?: 0L,
                                        name = trimmed,
                                        aliases = aliases,
                                        actionType = selectedAction,
                                        targetPackage = if (selectedAction == ActionType.OPEN_APP) targetPackage else null,
                                        targetAppLabel = if (selectedAction == ActionType.OPEN_APP) targetAppLabel else null,
                                        routineSteps = if (selectedAction == ActionType.ROUTINE) routineSteps else emptyList(),
                                        isEnabled = initialCommand?.isEnabled ?: true,
                                        createdAt = initialCommand?.createdAt ?: System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    onTapFeedback = onTapFeedback,
                    modifier = Modifier.weight(1.4f),
                    testTag = "editor_save_btn"
                )
            }
        }
    }
}
