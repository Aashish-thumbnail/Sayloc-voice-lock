package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ActionType
import com.example.data.UiLanguage
import com.example.data.VoiceCommand
import com.example.ui.components.Glass3DButton
import com.example.ui.components.Glass3DCard
import com.example.ui.components.Glass3DChip
import com.example.ui.components.Glass3DToggle
import com.example.ui.components.GlassButtonStyle
import com.example.ui.components.TiltOffset
import com.example.ui.i18n.LocalSaylocStrings
import com.example.ui.i18n.actionTypeLabel
import com.example.ui.i18n.routineStepLabel
import com.example.ui.theme.LocalSaylocPalette
import com.example.ui.theme.LockCrimson

@Composable
fun CommandsScreen(
    commands: List<VoiceCommand>,
    uiLanguage: UiLanguage,
    tilt: TiltOffset,
    onAddNew: () -> Unit,
    onEdit: (VoiceCommand) -> Unit,
    onDelete: (VoiceCommand) -> Unit,
    onToggleEnabled: (VoiceCommand, Boolean) -> Unit,
    onTestCommand: (VoiceCommand) -> Unit,
    onTapFeedback: () -> Unit
) {
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current
    var query by remember { mutableStateOf("") }

    val filtered = remember(commands, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) commands
        else commands.filter { cmd ->
            cmd.name.lowercase().contains(q) ||
                cmd.aliases.any { it.lowercase().contains(q) } ||
                (cmd.targetAppLabel?.lowercase()?.contains(q) == true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.commandsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Glass3DChip(
                    label = strings.addCommand,
                    selected = true,
                    icon = Icons.Rounded.Add,
                    onClick = {
                        onTapFeedback()
                        onAddNew()
                    },
                    testTag = "commands_add_btn"
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(strings.searchCommandsHint, color = palette.textSecondary)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = palette.rimGlow
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.rimGlow,
                    unfocusedBorderColor = palette.cardBorderTop,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedContainerColor = palette.cardBottom,
                    unfocusedContainerColor = palette.cardBottom
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("commands_search_input")
            )

            Spacer(Modifier.height(14.dp))

            if (filtered.isEmpty()) {
                Glass3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    tilt = tilt
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = palette.rimGlow,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.noCommandsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = strings.noCommandsBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Glass3DButton(
                            text = strings.addCommand,
                            icon = Icons.Rounded.Add,
                            style = GlassButtonStyle.PRIMARY,
                            onClick = onAddNew,
                            onTapFeedback = onTapFeedback,
                            testTag = "commands_empty_add_btn"
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { cmd ->
                        CommandItemCard(
                            command = cmd,
                            uiLanguage = uiLanguage,
                            tilt = tilt,
                            onToggleEnabled = { onToggleEnabled(cmd, it) },
                            onTest = { onTestCommand(cmd) },
                            onEdit = { onEdit(cmd) },
                            onDelete = { onDelete(cmd) },
                            onTapFeedback = onTapFeedback
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandItemCard(
    command: VoiceCommand,
    uiLanguage: UiLanguage,
    tilt: TiltOffset,
    onToggleEnabled: (Boolean) -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTapFeedback: () -> Unit
) {
    val strings = LocalSaylocStrings.current
    val palette = LocalSaylocPalette.current

    val actionIcon: ImageVector = when (command.actionType) {
        ActionType.LOCK -> Icons.Rounded.Lock
        ActionType.FLASHLIGHT_TOGGLE,
        ActionType.FLASHLIGHT_ON,
        ActionType.FLASHLIGHT_OFF -> Icons.Rounded.FlashlightOn
        ActionType.OPEN_APP -> Icons.Rounded.Apps
        ActionType.FIND_MY_PHONE -> Icons.Rounded.NotificationsActive
        ActionType.ROUTINE -> Icons.Rounded.AutoAwesome
        ActionType.EXPERIMENTAL_UNLOCK -> Icons.Rounded.LockOpen
    }

    val actionSubtitle = when (command.actionType) {
        ActionType.OPEN_APP -> "${actionTypeLabel(command.actionType, uiLanguage)} • ${command.targetAppLabel ?: command.targetPackage.orEmpty()}"
        ActionType.ROUTINE -> command.routineSteps.joinToString(" → ") { routineStepLabel(it, uiLanguage) }
        else -> actionTypeLabel(command.actionType, uiLanguage)
    }

    Glass3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("command_card_${command.id}"),
        tilt = tilt,
        accentBorder = command.isEnabled
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                if (command.isEnabled) listOf(palette.accentStart, palette.accentEnd)
                                else listOf(palette.cardTop, palette.cardBottom)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = if (command.isEnabled) Color.White else palette.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "\"${command.name}\"",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = actionSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.rimGlow,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (command.aliases.isNotEmpty()) {
                        Text(
                            text = command.aliases.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Glass3DToggle(
                checked = command.isEnabled,
                onCheckedChange = {
                    onTapFeedback()
                    onToggleEnabled(it)
                },
                testTag = "command_toggle_${command.id}"
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Glass3DChip(
                label = strings.testBtn,
                selected = true,
                icon = Icons.Rounded.PlayArrow,
                onClick = {
                    onTapFeedback()
                    onTest()
                },
                testTag = "command_test_${command.id}"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        onTapFeedback()
                        onEdit()
                    },
                    modifier = Modifier.testTag("command_edit_${command.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = strings.editBtn,
                        tint = palette.textPrimary
                    )
                }
                IconButton(
                    onClick = {
                        onTapFeedback()
                        onDelete()
                    },
                    modifier = Modifier.testTag("command_delete_${command.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = strings.deleteBtn,
                        tint = LockCrimson
                    )
                }
            }
        }
    }
}
