package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Entity(tableName = "voice_commands")
data class VoiceCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val aliasesCsv: String = "",
    val actionType: String = ActionType.LOCK.key,
    val targetPackage: String? = null,
    val targetAppLabel: String? = null,
    val routineStepsCsv: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "command_logs")
data class CommandLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val commandName: String,
    val heardText: String,
    val actionSummary: String,
    val source: String,
    val succeeded: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VoiceCommandDao {
    @Query("SELECT * FROM voice_commands ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<VoiceCommandEntity>>

    @Query("SELECT * FROM voice_commands WHERE isEnabled = 1 ORDER BY createdAt ASC")
    suspend fun getEnabledNow(): List<VoiceCommandEntity>

    @Query("SELECT * FROM voice_commands ORDER BY createdAt ASC")
    suspend fun getAllNow(): List<VoiceCommandEntity>

    @Query("SELECT COUNT(*) FROM voice_commands")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VoiceCommandEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VoiceCommandEntity>)

    @Update
    suspend fun update(entity: VoiceCommandEntity)

    @Query("DELETE FROM voice_commands WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE voice_commands SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}

@Dao
interface CommandLogDao {
    @Query("SELECT * FROM command_logs ORDER BY timestamp DESC LIMIT 150")
    fun observeRecent(): Flow<List<CommandLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CommandLogEntity)

    @Query("DELETE FROM command_logs")
    suspend fun clearAll()
}

@Database(
    entities = [VoiceCommandEntity::class, CommandLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SaylocDatabase : RoomDatabase() {
    abstract fun voiceCommandDao(): VoiceCommandDao
    abstract fun commandLogDao(): CommandLogDao

    companion object {
        @Volatile
        private var instance: SaylocDatabase? = null

        fun getInstance(context: Context): SaylocDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(appContext: Context): SaylocDatabase {
            lateinit var db: SaylocDatabase
            db = Room.databaseBuilder(
                appContext,
                SaylocDatabase::class.java,
                "sayloc_local.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(sqliteDb: SupportSQLiteDatabase) {
                        super.onCreate(sqliteDb)
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            seedStarterCommands(db.voiceCommandDao())
                        }
                    }
                })
                .build()
            return db
        }

        suspend fun seedStarterCommands(dao: VoiceCommandDao) {
            if (dao.count() > 0) return
            val now = System.currentTimeMillis()
            dao.insertAll(
                listOf(
                    VoiceCommandEntity(
                        name = "Lock",
                        aliasesCsv = "Lok, Band karo, Phone lock",
                        actionType = ActionType.LOCK.key,
                        isEnabled = true,
                        createdAt = now
                    ),
                    VoiceCommandEntity(
                        name = "Flash",
                        aliasesCsv = "Torch, Light, Roshni",
                        actionType = ActionType.FLASHLIGHT_TOGGLE.key,
                        isEnabled = true,
                        createdAt = now + 1
                    ),
                    VoiceCommandEntity(
                        name = "Good night",
                        aliasesCsv = "Shubh ratri, Sleep mode",
                        actionType = ActionType.ROUTINE.key,
                        routineStepsCsv = listOf(
                            RoutineStep.ENABLE_DND.key,
                            RoutineStep.FLASHLIGHT_OFF.key,
                            RoutineStep.LOCK_DEVICE.key
                        ).joinToString(","),
                        isEnabled = true,
                        createdAt = now + 2
                    )
                )
            )
        }
    }
}

fun VoiceCommandEntity.toDomain(): VoiceCommand = VoiceCommand(
    id = id,
    name = name,
    aliases = aliasesCsv.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() },
    actionType = ActionType.fromKey(actionType),
    targetPackage = targetPackage,
    targetAppLabel = targetAppLabel,
    routineSteps = routineStepsCsv.split(",")
        .mapNotNull { RoutineStep.fromKey(it.trim()) },
    isEnabled = isEnabled,
    createdAt = createdAt
)

fun VoiceCommand.toEntity(): VoiceCommandEntity = VoiceCommandEntity(
    id = id,
    name = name.trim(),
    aliasesCsv = aliases.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", "),
    actionType = actionType.key,
    targetPackage = targetPackage,
    targetAppLabel = targetAppLabel,
    routineStepsCsv = routineSteps.joinToString(",") { it.key },
    isEnabled = isEnabled,
    createdAt = createdAt
)

fun CommandLogEntity.toDomain(): CommandLog = CommandLog(
    id = id,
    commandName = commandName,
    heardText = heardText,
    actionSummary = actionSummary,
    source = LogSource.fromKey(source),
    succeeded = succeeded,
    timestamp = timestamp
)

class SaylocRepository(
    private val commandDao: VoiceCommandDao,
    private val logDao: CommandLogDao
) {
    val commands: Flow<List<VoiceCommand>> = commandDao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val logs: Flow<List<CommandLog>> = logDao.observeRecent().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun ensureStarterCommands() {
        SaylocDatabase.seedStarterCommands(commandDao)
    }

    suspend fun getEnabledCommands(): List<VoiceCommand> =
        commandDao.getEnabledNow().map { it.toDomain() }

    suspend fun getAllCommands(): List<VoiceCommand> =
        commandDao.getAllNow().map { it.toDomain() }

    suspend fun upsertCommand(command: VoiceCommand): Long {
        return if (command.id == 0L) {
            commandDao.insert(command.toEntity())
        } else {
            commandDao.update(command.toEntity())
            command.id
        }
    }

    suspend fun deleteCommand(id: Long) {
        commandDao.deleteById(id)
    }

    suspend fun setCommandEnabled(id: Long, enabled: Boolean) {
        commandDao.setEnabled(id, enabled)
    }

    suspend fun importCommands(imported: List<VoiceCommand>): Int {
        val existingNames = commandDao.getAllNow().map { it.name.trim().lowercase() }.toSet()
        val toInsert = imported.filter { it.name.trim().lowercase() !in existingNames }
            .map { it.copy(id = 0L, createdAt = System.currentTimeMillis()).toEntity() }
        if (toInsert.isNotEmpty()) {
            commandDao.insertAll(toInsert)
        }
        return toInsert.size
    }

    suspend fun logExecution(
        commandName: String,
        heardText: String,
        actionSummary: String,
        source: LogSource,
        succeeded: Boolean
    ) {
        logDao.insert(
            CommandLogEntity(
                commandName = commandName,
                heardText = heardText,
                actionSummary = actionSummary,
                source = source.key,
                succeeded = succeeded,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearLogs() {
        logDao.clearAll()
    }
}
