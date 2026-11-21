package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    private const val BACKUP_VERSION = 1

    fun serializeCommands(commands: List<VoiceCommand>): String {
        val root = JSONObject()
        root.put("app", "Sayloc")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val array = JSONArray()
        commands.forEach { cmd ->
            val obj = JSONObject().apply {
                put("name", cmd.name)
                put("aliases", JSONArray(cmd.aliases))
                put("actionType", cmd.actionType.key)
                put("targetPackage", cmd.targetPackage ?: JSONObject.NULL)
                put("targetAppLabel", cmd.targetAppLabel ?: JSONObject.NULL)
                put("routineSteps", JSONArray(cmd.routineSteps.map { it.key }))
                put("isEnabled", cmd.isEnabled)
            }
            array.put(obj)
        }
        root.put("commands", array)
        return root.toString(2)
    }

    fun deserializeCommands(jsonText: String): List<VoiceCommand> {
        val root = JSONObject(jsonText)
        val array = root.optJSONArray("commands") ?: return emptyList()
        val result = mutableListOf<VoiceCommand>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue

            val aliasesArr = obj.optJSONArray("aliases")
            val aliases = buildList {
                if (aliasesArr != null) {
                    for (j in 0 until aliasesArr.length()) {
                        val alias = aliasesArr.optString(j, "").trim()
                        if (alias.isNotEmpty()) add(alias)
                    }
                }
            }

            val stepsArr = obj.optJSONArray("routineSteps")
            val steps = buildList {
                if (stepsArr != null) {
                    for (j in 0 until stepsArr.length()) {
                        RoutineStep.fromKey(stepsArr.optString(j, ""))?.let { add(it) }
                    }
                }
            }

            val pkg = if (obj.isNull("targetPackage")) null else obj.optString("targetPackage").takeIf { it.isNotBlank() }
            val label = if (obj.isNull("targetAppLabel")) null else obj.optString("targetAppLabel").takeIf { it.isNotBlank() }

            result.add(
                VoiceCommand(
                    id = 0L,
                    name = name,
                    aliases = aliases,
                    actionType = ActionType.fromKey(obj.optString("actionType", ActionType.LOCK.key)),
                    targetPackage = pkg,
                    targetAppLabel = label,
                    routineSteps = steps,
                    isEnabled = obj.optBoolean("isEnabled", true),
                    createdAt = System.currentTimeMillis() + i
                )
            )
        }
        return result
    }

    suspend fun exportToUri(context: Context, uri: Uri, commands: List<VoiceCommand>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = serializeCommands(commands)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: error("Cannot open output file")
                commands.size
            }
        }

    suspend fun importFromUri(context: Context, uri: Uri): Result<List<VoiceCommand>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("Cannot read selected file")
                deserializeCommands(content)
            }
        }
}
