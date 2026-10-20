package com.codexremote

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat
import com.codexremote.domain.ChatItem
import com.codexremote.domain.ChatRole
import com.codexremote.domain.CodexModelCatalog
import com.codexremote.domain.CodexModelDiscovery
import com.codexremote.domain.CodexModelOption
import com.codexremote.domain.CodexRemoteSession
import com.codexremote.domain.CodexUiEvent
import com.codexremote.domain.AgentMessagePhase
import com.codexremote.domain.ConversationTitleGenerator
import com.codexremote.domain.MessageAttachment
import com.codexremote.domain.PhoneLocation
import com.codexremote.domain.ProcessStep
import com.codexremote.domain.ProcessStepKind
import com.codexremote.domain.ThreadRecord
import com.codexremote.domain.TitleApiConfig
import com.codexremote.domain.VpsConfig
import com.codexremote.location.PhoneLocationProvider
import com.codexremote.ssh.JschSshTransport
import com.codexremote.ssh.JschCodexAppServerClient
import com.codexremote.ssh.JschPtyTerminal
import com.codexremote.store.LocalAttachmentStore
import com.codexremote.store.PreferencesConfigStore
import com.codexremote.store.PreferencesThreadStore
import com.codexremote.store.PreferencesTitleConfigStore
import com.codexremote.store.TofuHostKeyRepository
import com.codexremote.terminal.TerminalWebController
import com.codexremote.terminal.TerminalWebView
import java.text.DateFormat
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            CodexRemoteApp()
        }
    }
}

private enum class MainMode {
    Chat,
    Terminal,
}

private enum class ConnectionIndicator {
    Disconnected,
    Connecting,
    Connected,
    Error,
}

private val AppBackground = Color(0xFF0E0F11)
private val AppSurface = Color(0xFF1E1F20)
private val AppSurfaceHigh = Color(0xFF282A2D)
private val AppText = Color(0xFFE3E3E3)
private val AppMuted = Color(0xFF9AA0A6)
private val AppBlue = Color(0xFF5578B4)
private val AppBlueContainer = Color(0xFF17294B)
private val AppError = Color(0xFFF2B8B5)


@Composable
private fun CodexRemoteApp() {
    val context = LocalContext.current.applicationContext
    val configStore = remember { PreferencesConfigStore(context) }
    val threadStore = remember { PreferencesThreadStore(context) }
    val titleConfigStore = remember { PreferencesTitleConfigStore(context) }
    val titleGenerator = remember { ConversationTitleGenerator() }
    val attachmentStore = remember { LocalAttachmentStore(context) }
    val locationProvider = remember { PhoneLocationProvider(context) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hostKeyRepository = remember { TofuHostKeyRepository(context) }
    val sshTransport = remember { JschSshTransport(hostKeyRepository) }
    val session = remember { CodexRemoteSession(configStore, threadStore, sshTransport) }
    val appServerClient = remember { JschCodexAppServerClient(hostKeyRepository) }
    val modelDiscovery = remember { CodexModelDiscovery(configStore, appServerClient) }
    val ptyTerminal = remember { JschPtyTerminal(hostKeyRepository) }
    val scope = rememberCoroutineScope()
    val initialConfig = remember { configStore.load() }
    val initialEffort = remember(initialConfig.model, initialConfig.reasoningEffort) {
        CodexModelCatalog.normalizeEffort(initialConfig.model, initialConfig.reasoningEffort)
    }

    var threads by remember { mutableStateOf(threadStore.list()) }
    val messages = remember { mutableStateListOf<ChatItem>() }
    val pendingAttachments = remember { mutableStateListOf<MessageAttachment>() }
    var selectedThreadId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWorkDir by rememberSaveable { mutableStateOf<String?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("未连接") }
    var running by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(MainMode.Chat) }
    var selectedModel by rememberSaveable { mutableStateOf(initialConfig.model) }
    var selectedEffort by rememberSaveable { mutableStateOf(initialEffort) }
    var availableModels by remember { mutableStateOf(CodexModelCatalog.models) }
    var modelsRefreshing by remember { mutableStateOf(false) }
    var modelRefreshError by remember { mutableStateOf<String?>(null) }
    var welcomeConnection by rememberSaveable { mutableStateOf<String?>(null) }
    var welcomeConnecting by rememberSaveable { mutableStateOf(false) }
    var connectionIndicator by rememberSaveable { mutableStateOf(ConnectionIndicator.Disconnected) }
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var pendingLocation by remember { mutableStateOf<PhoneLocation?>(null) }
    var renameTarget by remember { mutableStateOf<ThreadRecord?>(null) }
    var infoTarget by remember { mutableStateOf<ThreadRecord?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    val greeting = remember {
        listOf(
            "你好，有何需要？",
            "你好，今天想做什么？",
            "有什么需要我处理？",
            "准备好了，从哪里开始？",
        ).random()
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    fun captureLocation() {
        scope.launch {
            status = "正在获取位置"
            runCatching { locationProvider.current() }
                .onSuccess {
                    pendingLocation = it
                    status = if (it.isCached) "已附加最近位置" else "位置已附加"
                }
                .onFailure { status = it.message ?: "无法获取位置" }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(4),
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                runCatching { attachmentStore.importImages(uris) }
                    .onSuccess {
                        pendingAttachments.clear()
                        pendingAttachments.addAll(it)
                        status = "已选择 ${it.size} 张图片"
                    }
                    .onFailure { status = it.message ?: "无法读取图片" }
            }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) captureLocation() else status = "未授予定位权限"
    }

    fun refreshModels(showWelcomeStatus: Boolean = false) {
        if (modelsRefreshing || running) return
        val config = configStore.load()
        if (config.host.isBlank() || !configStore.hasPrivateKey()) {
            modelRefreshError = "请先完成 SSH 配置"
            connectionIndicator = ConnectionIndicator.Disconnected
            if (showWelcomeStatus) welcomeConnection = "请先配置 SSH"
            return
        }
        modelsRefreshing = true
        modelRefreshError = null
        connectionIndicator = ConnectionIndicator.Connecting
        if (showWelcomeStatus) {
            welcomeConnecting = true
            welcomeConnection = "正在连接并读取模型"
        }
        scope.launch {
            runCatching { modelDiscovery.discover() }
                .onSuccess { result ->
                    availableModels = result.models
                    val nextModel = selectedModel.takeIf { selected ->
                        selected.isNotBlank() && result.models.any { it.id == selected }
                    } ?: result.defaultModelId
                    val preferredEffort = selectedEffort.ifBlank {
                        result.models.firstOrNull { it.id == nextModel }?.defaultEffort.orEmpty()
                    }
                    val nextEffort = CodexModelCatalog.normalizeEffort(
                        nextModel,
                        preferredEffort,
                        result.models,
                    )
                    selectedModel = nextModel
                    selectedEffort = nextEffort
                    configStore.saveModelSelection(nextModel, nextEffort)
                    connectionIndicator = ConnectionIndicator.Connected
                    if (showWelcomeStatus) welcomeConnection = "VPS 与模型目录已连接"
                }
                .onFailure { error ->
                    modelRefreshError = error.message ?: "无法读取模型目录"
                    connectionIndicator = ConnectionIndicator.Error
                    if (showWelcomeStatus) {
                        welcomeConnection = "连接失败 · ${error.message ?: "请检查 SSH 设置"}"
                    }
                }
            modelsRefreshing = false
            if (showWelcomeStatus) welcomeConnecting = false
        }
    }

    fun toggleWelcomeConnection() {
        if (welcomeConnecting) return
        if (welcomeConnection != null) {
            welcomeConnection = null
            return
        }
        refreshModels(showWelcomeStatus = true)
    }

    LaunchedEffect(Unit) {
        if (initialEffort != initialConfig.reasoningEffort) {
            configStore.saveModelSelection(initialConfig.model, initialEffort)
        }
        refreshModels()
    }

    fun currentWorkDir(): String = selectedWorkDir?.takeIf { it.isNotBlank() } ?: configStore.load().workDir

    fun historyTitle(): String {
        return messages.firstOrNull { it.role == ChatRole.User }?.text
            ?.lineSequence()
            ?.firstOrNull()
            ?.take(48)
            ?.ifBlank { null }
            ?: selectedThreadId?.take(12)
            ?: "新会话"
    }

    fun persistHistory(threadId: String? = selectedThreadId, workDir: String = currentWorkDir()) {
        val id = threadId?.takeIf { it.isNotBlank() } ?: return
        val existing = threadStore.list().firstOrNull { it.id == id }
        threadStore.upsert(
            existing?.copy(
                title = if (existing.titleGenerated || existing.titleLocked) existing.title else historyTitle(),
                updatedAt = System.currentTimeMillis(),
                workDir = workDir,
            ) ?: ThreadRecord(
                id = id,
                title = historyTitle(),
                updatedAt = System.currentTimeMillis(),
                workDir = workDir,
            ),
        )
        threadStore.saveMessages(id, messages.map { it.copy(status = null) })
        threads = threadStore.list()
    }

    fun appendAgentText(text: String) {
        val index = messages.indexOfLast { it.role == ChatRole.Agent && it.status == "streaming" }
        if (index >= 0) {
            val old = messages[index]
            messages[index] = old.copy(text = old.text + text)
        } else {
            messages += ChatItem(UUID.randomUUID().toString(), ChatRole.Agent, text, status = "streaming")
        }
    }

    fun updateProcess(step: ProcessStep) {
        var processIndex = messages.indexOfLast { it.role == ChatRole.Process && it.status == "running" }
        if (processIndex < 0) {
            messages += ChatItem(
                id = UUID.randomUUID().toString(),
                role = ChatRole.Process,
                text = "工作过程",
                status = "running",
            )
            processIndex = messages.lastIndex
        }
        val process = messages[processIndex]
        val replaceIndex = process.processSteps.indexOfLast {
            it.kind == step.kind && it.title == step.title && it.status == "running"
        }
        val nextSteps = process.processSteps.toMutableList().apply {
            if (replaceIndex >= 0) set(replaceIndex, step) else add(step)
        }
        messages[processIndex] = process.copy(processSteps = nextSteps)
    }

    fun finishProcess(promoteCandidate: Boolean = true) {
        val processIndex = messages.indexOfLast { it.role == ChatRole.Process && it.status == "running" }
        if (processIndex < 0) return
        val process = messages[processIndex]
        val candidateIndex = process.processSteps.indexOfLast {
            it.kind == ProcessStepKind.Commentary && it.status == "candidate_final"
        }
        if (promoteCandidate && messages.none { it.role == ChatRole.Agent && it.status == "streaming" } && candidateIndex >= 0) {
            val finalText = process.processSteps[candidateIndex].content
            messages += ChatItem(UUID.randomUUID().toString(), ChatRole.Agent, finalText, status = "streaming")
        }
        val retained = process.processSteps.filterIndexed { index, _ -> !promoteCandidate || index != candidateIndex }
        if (retained.isEmpty()) messages.removeAt(processIndex)
        else messages[processIndex] = process.copy(status = "completed", processSteps = retained)
    }

    suspend fun generateTitleIfNeeded(threadId: String) {
        val record = threadStore.list().firstOrNull { it.id == threadId } ?: return
        val config = titleConfigStore.load()
        if (record.titleGenerated || record.titleLocked) return
        if (!config.enabled) {
            status = "标题未生成：请在设置中启用自动标题"
            return
        }
        if (config.apiKey.isBlank()) {
            status = "标题未生成：请在设置中填写 API Key"
            return
        }
        val firstUser = messages.firstOrNull { it.role == ChatRole.User }?.text ?: return
        val finalAnswer = messages.lastOrNull { it.role == ChatRole.Agent }?.text ?: return
        runCatching { titleGenerator.generate(config, firstUser, finalAnswer) }
            .onSuccess { title ->
                threadStore.upsert(
                    record.copy(
                        title = title,
                        titleGenerated = true,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                threads = threadStore.list()
                status = "AI 标题已生成"
            }
            .onFailure { error ->
                status = "标题生成失败：${error.message ?: "未知错误"}"
            }
    }

    fun markAgentComplete() {
        val index = messages.indexOfLast { it.role == ChatRole.Agent && it.status == "streaming" }
        if (index >= 0) messages[index] = messages[index].copy(status = null)
    }

    fun cancelActive() {
        activeJob?.cancel()
        session.cancel()
        running = false
        finishProcess(promoteCandidate = false)
        markAgentComplete()
        status = "已取消"
    }

    fun startSend() {
        val prompt = input.trim()
        if ((prompt.isBlank() && pendingAttachments.isEmpty() && pendingLocation == null) || running) return
        val workDir = currentWorkDir()
        val sendingAttachments = pendingAttachments.toList()
        val sendingLocation = pendingLocation
        input = ""
        pendingAttachments.clear()
        pendingLocation = null
        messages += ChatItem(
            id = UUID.randomUUID().toString(),
            role = ChatRole.User,
            text = prompt,
            attachments = sendingAttachments,
            location = sendingLocation,
        )
        selectedWorkDir = workDir
        running = true
        status = "运行中"
        activeJob = scope.launch {
            var completedNormally = false
            try {
                session.send(
                    currentThreadId = selectedThreadId,
                    workDirOverride = selectedWorkDir,
                    message = prompt.ifBlank { "请查看我附加的内容。" },
                    attachments = sendingAttachments,
                    location = sendingLocation,
                ) { event ->
                    withContext(Dispatchers.Main) {
                        when (event) {
                            is CodexUiEvent.AgentText -> {
                                when (event.phase) {
                                    AgentMessagePhase.FinalAnswer -> appendAgentText(event.text)
                                    AgentMessagePhase.Commentary -> updateProcess(
                                        ProcessStep(
                                            id = UUID.randomUUID().toString(),
                                            kind = ProcessStepKind.Commentary,
                                            title = event.text.lineSequence().firstOrNull().orEmpty().take(56).ifBlank { "进度更新" },
                                            content = event.text,
                                            status = "completed",
                                        ),
                                    )
                                    AgentMessagePhase.Unknown -> updateProcess(
                                        ProcessStep(
                                            id = UUID.randomUUID().toString(),
                                            kind = ProcessStepKind.Commentary,
                                            title = event.text.lineSequence().firstOrNull().orEmpty().take(56).ifBlank { "Comso1 回复" },
                                            content = event.text,
                                            status = "candidate_final",
                                        ),
                                    )
                                }
                                persistHistory()
                            }
                            is CodexUiEvent.ToolCall -> {
                                updateProcess(
                                    ProcessStep(
                                        id = UUID.randomUUID().toString(),
                                        kind = ProcessStepKind.Tool,
                                        title = event.title,
                                        content = event.details,
                                        status = event.status,
                                    ),
                                )
                                persistHistory()
                            }
                            is CodexUiEvent.Command -> {
                                updateProcess(
                                    ProcessStep(
                                        id = UUID.randomUUID().toString(),
                                        kind = ProcessStepKind.Command,
                                        title = event.command,
                                        content = event.output,
                                        status = buildString {
                                            append(event.status)
                                            event.exitCode?.let { append(" · 退出码 $it") }
                                        },
                                    ),
                                )
                                persistHistory()
                            }
                            is CodexUiEvent.Error -> {
                                updateProcess(
                                    ProcessStep(
                                        id = UUID.randomUUID().toString(),
                                        kind = ProcessStepKind.Error,
                                        title = "运行期错误",
                                        content = event.message,
                                        status = "error",
                                    ),
                                )
                                status = "运行中 · 检测到错误"
                                persistHistory()
                            }
                            is CodexUiEvent.ThreadFound -> {
                                selectedThreadId = event.threadId
                                selectedWorkDir = workDir
                                status = "会话 ${event.threadId.take(8)}"
                                persistHistory(event.threadId, workDir)
                            }
                            is CodexUiEvent.Completed -> {
                                completedNormally = true
                                event.threadId?.let {
                                    selectedThreadId = it
                                    selectedWorkDir = workDir
                                }
                                finishProcess()
                                markAgentComplete()
                                persistHistory(event.threadId ?: selectedThreadId, workDir)
                                status = "空闲"
                            }
                            is CodexUiEvent.Raw -> Unit
                        }
                    }
                }
            } catch (_: CancellationException) {
                withContext(Dispatchers.Main) {
                    messages += ChatItem(UUID.randomUUID().toString(), ChatRole.System, "已取消")
                    markAgentComplete()
                    status = "已取消"
                    persistHistory()
                }
            } catch (error: Throwable) {
                withContext(Dispatchers.Main) {
                    messages += ChatItem(UUID.randomUUID().toString(), ChatRole.Error, error.message ?: error::class.java.simpleName)
                    markAgentComplete()
                    status = "错误"
                    persistHistory()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    running = false
                    threads = threadStore.list()
                }
                if (completedNormally) selectedThreadId?.let { generateTitleIfNeeded(it) }
            }
        }
    }

    fun newThread(closeDrawer: Boolean = true) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        selectedThreadId = null
        selectedWorkDir = configStore.load().workDir
        messages.clear()
        input = ""
        pendingAttachments.clear()
        pendingLocation = null
        welcomeConnection = null
        status = "新会话"
        mode = MainMode.Chat
        if (closeDrawer) scope.launch { drawerState.close() }
    }

    fun renameThread(record: ThreadRecord) {
        renameTarget = record
        renameValue = record.title
    }

    fun togglePinned(record: ThreadRecord) {
        threadStore.setPinned(record.id, !record.pinned)
        threads = threadStore.list()
        status = if (record.pinned) "已取消置顶" else "已置顶"
    }

    fun toggleArchived(record: ThreadRecord) {
        val archive = !record.archived
        threadStore.setArchived(record.id, archive)
        threads = threadStore.list()
        if (archive && selectedThreadId == record.id) {
            selectedThreadId = null
            selectedWorkDir = configStore.load().workDir
            messages.clear()
            pendingAttachments.clear()
            pendingLocation = null
        }
        status = if (archive) "会话已归档" else "会话已恢复"
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppBlue,
            onPrimary = Color(0xFFF4F7FF),
            primaryContainer = AppBlueContainer,
            onPrimaryContainer = Color(0xFFD7E3FF),
            secondary = AppBlue,
            background = AppBackground,
            surface = AppSurface,
            surfaceVariant = AppSurfaceHigh,
            onBackground = AppText,
            onSurface = AppText,
            onSurfaceVariant = AppMuted,
            error = AppError,
        ),
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !running,
            drawerContent = {
                AppDrawer(
                    busy = running,
                    threads = threads,
                    selectedThreadId = selectedThreadId,
                    mode = mode,
                    onClose = { scope.launch { drawerState.close() } },
                    onNewThread = { newThread() },
                    onSelect = { record ->
                        selectedThreadId = record.id
                        selectedWorkDir = record.workDir.ifBlank { configStore.load().workDir }
                        messages.clear()
                        messages += threadStore.messages(record.id)
                        pendingAttachments.clear()
                        pendingLocation = null
                        status = "已恢复 ${record.id.take(8)}"
                        mode = MainMode.Chat
                        scope.launch { drawerState.close() }
                    },
                    onSettings = {
                        showSettings = true
                        scope.launch { drawerState.close() }
                    },
                    onRename = ::renameThread,
                    onShowInfo = { record -> infoTarget = record },
                    onTogglePinned = ::togglePinned,
                    onToggleArchived = ::toggleArchived,
                    onToggleMode = {
                        if (mode == MainMode.Terminal) ptyTerminal.disconnect()
                        mode = if (mode == MainMode.Chat) MainMode.Terminal else MainMode.Chat
                        scope.launch { drawerState.close() }
                    },
                )
            },
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppBackground,
            ) {
                val openDrawer: () -> Unit = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    scope.launch { drawerState.open() }
                    Unit
                }
                when (mode) {
                    MainMode.Chat -> ChatPane(
                        messages = messages,
                        input = input,
                        onInputChange = { input = it },
                        onSend = ::startSend,
                        onStop = ::cancelActive,
                        onOpenDrawer = openDrawer,
                        onNewThread = { newThread(closeDrawer = false) },
                        currentThread = threads.firstOrNull { it.id == selectedThreadId },
                        onRenameCurrent = ::renameThread,
                        onShowCurrentInfo = { infoTarget = it },
                        onToggleCurrentPinned = ::togglePinned,
                        onToggleCurrentArchived = ::toggleArchived,
                        running = running,
                        configured = configStore.load().host.isNotBlank() && configStore.hasPrivateKey(),
                        model = selectedModel,
                        reasoningEffort = selectedEffort,
                        availableModels = availableModels,
                        modelsRefreshing = modelsRefreshing,
                        modelRefreshError = modelRefreshError,
                        onRefreshModels = ::refreshModels,
                        onModelSelected = { model ->
                            val normalizedEffort = CodexModelCatalog.normalizeEffort(model, selectedEffort, availableModels)
                            selectedModel = model
                            selectedEffort = normalizedEffort
                            configStore.saveModelSelection(model, normalizedEffort)
                        },
                        onEffortSelected = { effort ->
                            selectedEffort = effort
                            configStore.saveModelSelection(selectedModel, effort)
                        },
                        pendingAttachments = pendingAttachments,
                        pendingLocation = pendingLocation,
                        onPickImages = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onRemoveAttachment = { attachment -> pendingAttachments.remove(attachment) },
                        onAttachLocation = {
                            val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                            val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                                captureLocation()
                            } else {
                                locationPermission.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                        },
                        onRemoveLocation = { pendingLocation = null },
                        greeting = greeting,
                        welcomeConnection = welcomeConnection,
                        welcomeConnecting = welcomeConnecting,
                        connectionIndicator = connectionIndicator,
                        onWelcomeClick = ::toggleWelcomeConnection,
                    )
                    MainMode.Terminal -> TerminalPane(
                        terminal = ptyTerminal,
                        config = configStore.load(),
                        onOpenDrawer = openDrawer,
                        onClose = { mode = MainMode.Chat },
                        workDir = currentWorkDir(),
                        configured = configStore.load().host.isNotBlank() && configStore.hasPrivateKey(),
                    )
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            initial = configStore.load(),
            hasPrivateKey = configStore.hasPrivateKey(),
            initialTitleApi = titleConfigStore.load(),
            hasTitleApiKey = titleConfigStore.hasApiKey(),
            onDismiss = { showSettings = false },
            onSave = { config, privateKey, titleConfig, titleApiKey ->
                configStore.save(config, privateKey)
                titleConfigStore.save(titleConfig, titleApiKey)
                selectedWorkDir = selectedWorkDir ?: config.workDir
                status = "设置已保存"
                showSettings = false
                refreshModels()
            },
        )
    }

    renameTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = AppSurfaceHigh,
            titleContentColor = AppText,
            textContentColor = AppText,
            shape = RoundedCornerShape(28.dp),
            title = { Text("修改会话标题") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it.take(80) },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = settingsTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        threadStore.rename(record.id, renameValue, lock = true)
                        threads = threadStore.list()
                        renameTarget = null
                    },
                    enabled = renameValue.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            },
        )
    }

    infoTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { infoTarget = null },
            containerColor = AppSurfaceHigh,
            titleContentColor = AppText,
            textContentColor = AppText,
            shape = RoundedCornerShape(28.dp),
            title = { Text("会话信息") },
            text = {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SessionInfoField("标题", record.title.ifBlank { "未命名会话" })
                        SessionInfoField("会话 ID", record.id)
                        SessionInfoField("工作目录", record.workDir.ifBlank { configStore.load().workDir })
                        SessionInfoField(
                            "最近更新",
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(record.updatedAt),
                        )
                        SessionInfoField("状态", if (record.archived) "已归档" else if (record.pinned) "已置顶" else "最近会话")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { infoTarget = null }) { Text("完成") }
            },
        )
    }
}

@Composable
private fun SessionInfoField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = AppMuted, fontSize = 12.sp)
        Text(value, color = AppText, fontSize = 15.sp, lineHeight = 21.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDrawer(
    busy: Boolean,
    threads: List<ThreadRecord>,
    selectedThreadId: String?,
    mode: MainMode,
    onClose: () -> Unit,
    onNewThread: () -> Unit,
    onSelect: (ThreadRecord) -> Unit,
    onSettings: () -> Unit,
    onToggleMode: () -> Unit,
    onRename: (ThreadRecord) -> Unit,
    onShowInfo: (ThreadRecord) -> Unit,
    onTogglePinned: (ThreadRecord) -> Unit,
    onToggleArchived: (ThreadRecord) -> Unit,
) {
    var actionTarget by remember { mutableStateOf<ThreadRecord?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val activeThreads = threads.filterNot { it.archived }
    val pinnedThreads = activeThreads.filter { it.pinned }
    val recentThreads = activeThreads.filterNot { it.pinned }
    val archivedThreads = threads.filter { it.archived }

    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        drawerContainerColor = Color(0xFF141516),
        drawerContentColor = AppText,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Comso1", modifier = Modifier.weight(1f), fontSize = 21.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = { if (!busy) onNewThread() }, enabled = !busy) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "发起新对话",
                    tint = AppText,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭侧边栏", tint = AppText)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (pinnedThreads.isNotEmpty()) {
                item(key = "pinned-label") {
                    DrawerSectionLabel("置顶")
                }
                items(pinnedThreads, key = { "pinned-${it.id}" }) { record ->
                    ConversationDrawerRow(
                        record = record,
                        selected = selectedThreadId == record.id,
                        enabled = !busy,
                        onClick = { onSelect(record) },
                        onLongClick = { actionTarget = record },
                    )
                }
            }
            item(key = "recent-label") {
                DrawerSectionLabel("最近会话")
            }
            items(recentThreads, key = { it.id }) { record ->
                ConversationDrawerRow(
                    record = record,
                    selected = selectedThreadId == record.id,
                    enabled = !busy,
                    onClick = { onSelect(record) },
                    onLongClick = { actionTarget = record },
                )
            }
            if (archivedThreads.isNotEmpty()) {
                item(key = "archived-toggle") {
                    TextButton(
                        onClick = { showArchived = !showArchived },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            if (showArchived) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = AppText,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "已归档 · ${archivedThreads.size}",
                            color = AppText,
                            fontSize = 17.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            if (showArchived) {
                items(archivedThreads, key = { "archived-${it.id}" }) { record ->
                    ConversationDrawerRow(
                        record = record,
                        selected = selectedThreadId == record.id,
                        enabled = !busy,
                        onClick = { onSelect(record) },
                        onLongClick = { actionTarget = record },
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color(0xFF34363A))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onToggleMode,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(
                    if (mode == MainMode.Chat) Icons.Rounded.Terminal else Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint = AppText,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (mode == MainMode.Chat) "终端" else "聊天",
                    color = AppText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            OutlinedButton(
                onClick = onSettings,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = AppText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("设置", color = AppText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    actionTarget?.let { record ->
        ModalBottomSheet(
            onDismissRequest = { actionTarget = null },
            containerColor = Color(0xFF17181A),
            contentColor = AppText,
            scrimColor = Color.Black.copy(alpha = 0.58f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    record.title.ifBlank { record.id.take(8) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!record.archived) {
                    ThreadActionRow(
                        icon = Icons.Rounded.PushPin,
                        label = if (record.pinned) "取消置顶" else "置顶",
                        onClick = {
                            actionTarget = null
                            onTogglePinned(record)
                        },
                    )
                }
                ThreadActionRow(
                    icon = Icons.Rounded.Info,
                    label = "会话信息",
                    onClick = {
                        actionTarget = null
                        onShowInfo(record)
                    },
                )
                ThreadActionRow(
                    icon = Icons.Rounded.Edit,
                    label = "重命名",
                    onClick = {
                        actionTarget = null
                        onRename(record)
                    },
                )
                ThreadActionRow(
                    icon = if (record.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                    label = if (record.archived) "移出归档" else "归档",
                    onClick = {
                        actionTarget = null
                        onToggleArchived(record)
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(label: String) {
    Text(
        label,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp),
        color = AppText,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationDrawerRow(
    record: ThreadRecord,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val contentColor = if (selected) Color(0xFFD7E3FF) else AppText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) AppBlueContainer else Color.Transparent)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            record.title.ifBlank { record.id.take(8) },
            modifier = Modifier.weight(1f),
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ThreadActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AppMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp)
    }
}

@Composable
private fun ChatPane(
    messages: List<ChatItem>,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNewThread: () -> Unit,
    currentThread: ThreadRecord?,
    onRenameCurrent: (ThreadRecord) -> Unit,
    onShowCurrentInfo: (ThreadRecord) -> Unit,
    onToggleCurrentPinned: (ThreadRecord) -> Unit,
    onToggleCurrentArchived: (ThreadRecord) -> Unit,
    running: Boolean,
    configured: Boolean,
    model: String,
    reasoningEffort: String,
    availableModels: List<CodexModelOption>,
    modelsRefreshing: Boolean,
    modelRefreshError: String?,
    onRefreshModels: () -> Unit,
    onModelSelected: (String) -> Unit,
    onEffortSelected: (String) -> Unit,
    pendingAttachments: List<MessageAttachment>,
    pendingLocation: PhoneLocation?,
    onPickImages: () -> Unit,
    onRemoveAttachment: (MessageAttachment) -> Unit,
    onAttachLocation: () -> Unit,
    onRemoveLocation: () -> Unit,
    greeting: String,
    welcomeConnection: String?,
    welcomeConnecting: Boolean,
    connectionIndicator: ConnectionIndicator,
    onWelcomeClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var composerHeightPx by remember { mutableStateOf(0) }
    val composerBottomPadding = with(density) { composerHeightPx.toDp() }.coerceAtLeast(112.dp) + 12.dp
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 76.dp,
                bottom = composerBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            greeting = greeting,
                            connectionStatus = welcomeConnection,
                            connecting = welcomeConnecting,
                            onClick = onWelcomeClick,
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { item ->
                ChatBubble(item)
            }
        }
        Composer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { composerHeightPx = it.height },
            input = input,
            running = running,
            configured = configured,
            connectionIndicator = connectionIndicator,
            onInputChange = onInputChange,
            onSend = onSend,
            onStop = onStop,
            model = model,
            reasoningEffort = reasoningEffort,
            availableModels = availableModels,
            modelsRefreshing = modelsRefreshing,
            modelRefreshError = modelRefreshError,
            onRefreshModels = onRefreshModels,
            onModelSelected = onModelSelected,
            onEffortSelected = onEffortSelected,
            attachments = pendingAttachments,
            location = pendingLocation,
            onPickImages = onPickImages,
            onRemoveAttachment = onRemoveAttachment,
            onAttachLocation = onAttachLocation,
            onRemoveLocation = onRemoveLocation,
        )
        ChatTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f),
            running = running,
            onOpenDrawer = onOpenDrawer,
            onNewThread = onNewThread,
            currentThread = currentThread,
            onRename = onRenameCurrent,
            onShowInfo = onShowCurrentInfo,
            onTogglePinned = onToggleCurrentPinned,
            onToggleArchived = onToggleCurrentArchived,
        )
    }
}

@Composable
private fun TerminalPane(
    terminal: JschPtyTerminal,
    config: VpsConfig,
    onOpenDrawer: () -> Unit,
    onClose: () -> Unit,
    workDir: String,
    configured: Boolean,
) {
    val scope = rememberCoroutineScope()
    val controller = remember { TerminalWebController() }
    var readySize by remember { mutableStateOf<TerminalSize?>(null) }
    var retryKey by remember { mutableStateOf(0) }
    var connectionState by remember { mutableStateOf(if (configured) "正在连接" else "请先配置 SSH") }
    var connected by remember { mutableStateOf(false) }
    var locked by rememberSaveable { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableStateOf(14) }

    LaunchedEffect(readySize, retryKey, config.host, config.port, config.username, workDir) {
        val size = readySize ?: return@LaunchedEffect
        if (!configured) return@LaunchedEffect
        terminal.disconnect()
        connectionState = "正在连接"
        connected = false
        controller.clear()
        try {
            terminal.run(
                config = config,
                workDir = workDir,
                initialColumns = size.columns,
                initialRows = size.rows,
                onConnected = {
                    withContext(Dispatchers.Main) {
                        connected = true
                        connectionState = "已连接"
                        controller.write("\r\n[Comso1] SSH PTY 已连接\r\n".toByteArray())
                        controller.focus()
                    }
                },
                onData = controller::write,
            )
            connectionState = "连接已关闭"
        } catch (_: CancellationException) {
            connectionState = "连接已关闭"
        } catch (error: Throwable) {
            connectionState = error.message ?: "终端连接失败"
            controller.write("\r\n[Comso1] $connectionState\r\n".toByteArray())
        } finally {
            connected = false
        }
    }

    DisposableEffect(terminal) {
        onDispose { terminal.disconnect() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        TerminalWebView(
            controller = controller,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 106.dp, bottom = 54.dp),
            onReady = { columns, rows, width, height ->
                scope.launch {
                    if (readySize == null) {
                        readySize = TerminalSize(columns, rows, width, height)
                    } else if (!locked) {
                        terminal.resize(columns, rows, width, height)
                    }
                    controller.setFontSize(fontSize)
                    controller.setLocked(locked)
                    controller.write("[Comso1] 正在打开交互式终端...\r\n".toByteArray())
                }
            },
            onInput = terminal::write,
            onResize = { columns, rows, width, height ->
                if (!locked) terminal.resize(columns, rows, width, height)
            },
            onFontSize = { size -> scope.launch { fontSize = size.coerceIn(9, 28) } },
            onError = { message -> connectionState = "终端渲染失败 · $message" },
        )

        TerminalTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f),
            status = connectionState,
            connected = connected,
            locked = locked,
            onOpenDrawer = onOpenDrawer,
            onToggleLock = {
                locked = !locked
                controller.setLocked(locked)
            },
            onReconnect = {
                terminal.disconnect()
                retryKey += 1
            },
            onClose = {
                terminal.disconnect()
                onClose()
            },
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState()),
            color = Color(0xEE111317),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TerminalKey("Esc", connected) { terminal.write(byteArrayOf(0x1B)) }
                TerminalKey("Tab", connected) { terminal.write(byteArrayOf(0x09)) }
                TerminalKey("Ctrl+C", connected) { terminal.write(byteArrayOf(0x03)) }
                TerminalKey("Ctrl+D", connected) { terminal.write(byteArrayOf(0x04)) }
                TerminalKey("↑", connected) { terminal.write("\u001B[A".toByteArray()) }
                TerminalKey("↓", connected) { terminal.write("\u001B[B".toByteArray()) }
                TerminalKey("←", connected) { terminal.write("\u001B[D".toByteArray()) }
                TerminalKey("→", connected) { terminal.write("\u001B[C".toByteArray()) }
                TerminalKey("−", !locked) {
                    fontSize = (fontSize - 1).coerceAtLeast(9)
                    controller.setFontSize(fontSize)
                }
                Text("$fontSize", color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp))
                TerminalKey("+", !locked) {
                    fontSize = (fontSize + 1).coerceAtMost(28)
                    controller.setFontSize(fontSize)
                }
            }
        }
    }
}

@Composable
private fun TerminalKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 12.dp)) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

private data class TerminalSize(
    val columns: Int,
    val rows: Int,
    val widthPx: Int,
    val heightPx: Int,
)

@Composable
private fun TerminalTopBar(
    modifier: Modifier = Modifier,
    status: String,
    connected: Boolean,
    locked: Boolean,
    onOpenDrawer: () -> Unit,
    onToggleLock: () -> Unit,
    onReconnect: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.82f))
            .statusBarsPadding()
            .height(68.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Rounded.Menu, contentDescription = "打开侧边栏")
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(if (connected) AppBlue else AppMuted, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(status, color = AppMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onToggleLock) {
            Icon(if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, contentDescription = if (locked) "解锁终端尺寸" else "锁定终端尺寸")
        }
        IconButton(onClick = onReconnect) {
            Icon(Icons.Rounded.Refresh, contentDescription = "重新连接")
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "关闭终端")
        }
    }
}

@Composable
private fun ChatTopBar(
    modifier: Modifier = Modifier,
    running: Boolean,
    onOpenDrawer: () -> Unit,
    onNewThread: () -> Unit,
    currentThread: ThreadRecord?,
    onRename: (ThreadRecord) -> Unit,
    onShowInfo: (ThreadRecord) -> Unit,
    onTogglePinned: (ThreadRecord) -> Unit,
    onToggleArchived: (ThreadRecord) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp)
            .background(
                Brush.verticalGradient(
                    0f to AppBackground,
                    0.56f to AppBackground.copy(alpha = 0.9f),
                    1f to Color.Transparent,
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(68.dp)
                .padding(start = 8.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Rounded.Menu, contentDescription = "打开侧边栏")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onNewThread, enabled = !running) {
                Icon(Icons.Rounded.Edit, contentDescription = "新对话")
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    enabled = currentThread != null && !running,
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                }
                AppPopupMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    alignment = Alignment.TopEnd,
                    offset = DpOffset(x = (-22).dp, y = 72.dp),
                    width = 164.dp,
                ) {
                    currentThread?.let { record ->
                        AppPopupMenuItem(
                            label = if (record.pinned) "取消置顶" else "置顶",
                            icon = Icons.Rounded.PushPin,
                            onClick = {
                                menuExpanded = false
                                onTogglePinned(record)
                            },
                        )
                        AppPopupMenuItem(
                            label = "会话信息",
                            icon = Icons.Rounded.Info,
                            onClick = {
                                menuExpanded = false
                                onShowInfo(record)
                            },
                        )
                        AppPopupMenuItem(
                            label = "重命名",
                            icon = Icons.Rounded.Edit,
                            onClick = {
                                menuExpanded = false
                                onRename(record)
                            },
                        )
                        AppPopupMenuItem(
                            label = "归档",
                            icon = Icons.Rounded.Archive,
                            onClick = {
                                menuExpanded = false
                                onToggleArchived(record)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    alignment: Alignment,
    offset: DpOffset = DpOffset.Zero,
    width: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val density = LocalDensity.current
    Popup(
        alignment = alignment,
        offset = with(density) {
            IntOffset(offset.x.roundToPx(), offset.y.roundToPx())
        },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier.width(width),
            shape = RoundedCornerShape(28.dp),
            color = AppSurfaceHigh,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun AppPopupMenuItem(
    label: String,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = AppMuted,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(14.dp))
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = AppText,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelEffortPicker(
    model: String,
    reasoningEffort: String,
    enabled: Boolean,
    availableModels: List<CodexModelOption>,
    refreshing: Boolean,
    refreshError: String?,
    onRefresh: () -> Unit,
    onModelSelected: (String) -> Unit,
    onEffortSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var effortMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val modelOption = CodexModelCatalog.model(model, availableModels)
    val effortOptions = CodexModelCatalog.effortsFor(model, availableModels)
    val selectedEffort = effortOptions.firstOrNull { it.id == reasoningEffort } ?: effortOptions.first()

    TextButton(
        onClick = { expanded = true },
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
    ) {
        Text(
            "${modelOption.shortLabel} ${selectedEffort.label}",
            color = if (enabled) AppText else AppMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = "选择模型和推理档位",
            tint = AppMuted,
            modifier = Modifier.size(20.dp),
        )
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            sheetState = sheetState,
            containerColor = Color(0xFF17181A),
            contentColor = AppText,
            scrimColor = Color.Black.copy(alpha = 0.58f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("配置", modifier = Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh, enabled = !refreshing) {
                        if (refreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = if (refreshError == null) "刷新模型目录" else "模型目录刷新失败，点击重试",
                                tint = if (refreshError == null) AppText else AppError,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF4A4D53), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = AppSurface,
                ) {
                    Column {
                        Box {
                            ConfigurationRow(
                                label = "模型",
                                value = modelOption.shortLabel,
                                expanded = modelMenuExpanded,
                                onClick = { modelMenuExpanded = !modelMenuExpanded },
                            )
                            AppPopupMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false },
                                alignment = Alignment.Center,
                                width = 250.dp,
                            ) {
                                availableModels.forEach { option ->
                                    AppPopupMenuItem(
                                        label = option.shortLabel,
                                        selected = option.id == model,
                                        onClick = {
                                            onModelSelected(option.id)
                                            modelMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = Color(0xFF3A3C40))
                        Box {
                            ConfigurationRow(
                                label = "思考强度",
                                value = selectedEffort.label,
                                expanded = effortMenuExpanded,
                                onClick = { effortMenuExpanded = !effortMenuExpanded },
                            )
                            AppPopupMenu(
                                expanded = effortMenuExpanded,
                                onDismissRequest = { effortMenuExpanded = false },
                                alignment = Alignment.Center,
                                width = 164.dp,
                            ) {
                                effortOptions.forEach { option ->
                                    AppPopupMenuItem(
                                        label = option.label,
                                        selected = option.id == selectedEffort.id,
                                        onClick = {
                                            onEffortSelected(option.id)
                                            effortMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .border(1.dp, Color(0xFF4A4D53), RoundedCornerShape(24.dp))
                        .clickable {
                            val defaultModel = availableModels.firstOrNull { it.isDefault }
                            onModelSelected(defaultModel?.id.orEmpty())
                            onEffortSelected(defaultModel?.defaultEffort.orEmpty())
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = AppSurface,
                ) {
                    Text(
                        "重置为默认设置",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        color = Color(0xFFAEC6F4),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Button(
                    onClick = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF2F2F2),
                        contentColor = Color(0xFF111214),
                    ),
                ) {
                    Text("完成", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ConfigurationRow(
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 19.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(value, color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = AppMuted,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun EmptyState(
    greeting: String,
    connectionStatus: String?,
    connecting: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = !connecting, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = AppBlue,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.height(16.dp))
        }
        Text(
            connectionStatus ?: greeting,
            color = if (connectionStatus?.startsWith("连接失败") == true) AppError else AppText,
            fontWeight = FontWeight.Normal,
            fontSize = 25.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChatBubble(item: ChatItem) {
    when (item.role) {
        ChatRole.User -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.86f),
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
                color = AppSurfaceHigh,
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (item.attachments.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item.attachments.forEach { attachment ->
                                AttachmentThumbnail(attachment, onRemove = null)
                            }
                        }
                        if (item.text.isNotBlank()) Spacer(Modifier.height(10.dp))
                    }
                    if (item.text.isNotBlank()) MarkdownText(item.text, AppText)
                    item.location?.let {
                        Spacer(Modifier.height(9.dp))
                        LocationChip(it, onRemove = null)
                    }
                }
            }
        }

        ChatRole.Agent -> Column(Modifier.fillMaxWidth()) {
            Text(
                if (item.status == "streaming") "Comso1 正在回复" else "Comso1",
                color = AppMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            MarkdownText(item.text, AppText)
        }

        ChatRole.Process -> ProcessCard(item)

        ChatRole.Tool -> ToolCard(title = item.text, details = item.details.orEmpty())

        ChatRole.Error -> StatusMessage(
            icon = Icons.Rounded.ErrorOutline,
            label = "错误",
            text = item.text,
            tint = AppError,
        )

        ChatRole.System -> StatusMessage(
            icon = Icons.Rounded.ChatBubbleOutline,
            label = "系统",
            text = item.text,
            tint = AppMuted,
        )
    }
}

@Composable
private fun StatusMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    text: String,
    tint: Color,
) {
    Surface(shape = RoundedCornerShape(8.dp), color = AppSurface) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, color = tint, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
            Spacer(Modifier.height(7.dp))
            MarkdownText(text, AppText)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProcessCard(item: ChatItem) {
    var expanded by remember(item.id) { mutableStateOf(item.status == "running") }
    LaunchedEffect(item.status) {
        expanded = item.status == "running"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .combinedClickable(onClick = { expanded = !expanded })
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(AppBlue, CircleShape),
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (item.status == "running") "Comso1 正在工作" else "工作过程",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Text(
                    "${item.processSteps.size} 个步骤${if (item.status == "running") " · 实时更新" else " · 已完成"}",
                    color = AppMuted,
                    fontSize = 11.sp,
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "收起工作过程" else "展开工作过程",
                tint = AppMuted,
            )
        }
        if (expanded) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF34363A))
            item.processSteps.forEachIndexed { index, step ->
                ProcessStepView(step)
                if (index != item.processSteps.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 27.dp, top = 11.dp, bottom = 11.dp),
                        color = Color(0xFF2B2D30),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessStepView(step: ProcessStep) {
    val icon = when (step.kind) {
        ProcessStepKind.Commentary -> Icons.Rounded.ChatBubbleOutline
        ProcessStepKind.Command -> Icons.Rounded.Terminal
        ProcessStepKind.Tool -> Icons.Rounded.AutoAwesome
        ProcessStepKind.Error -> Icons.Rounded.ErrorOutline
    }
    val tint = when (step.kind) {
        ProcessStepKind.Command -> AppBlue
        ProcessStepKind.Tool -> AppBlue
        ProcessStepKind.Commentary -> AppMuted
        ProcessStepKind.Error -> AppError
    }
    val visibleStatus = when (step.status) {
        "running" -> "进行中"
        "error" -> "错误"
        "completed", "candidate_final" -> ""
        else -> step.status
    }
    Column {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(step.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 3)
                if (visibleStatus.isNotBlank()) {
                    Text(visibleStatus, color = AppMuted, fontSize = 10.sp)
                }
            }
        }
        if (step.content.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            when (step.kind) {
                ProcessStepKind.Commentary -> MarkdownText(
                    step.content,
                    AppText,
                    Modifier.padding(start = 27.dp),
                )
                ProcessStepKind.Command -> SelectionContainer {
                    Text(
                        step.content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 27.dp)
                            .background(Color(0xFF17181A), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        color = Color(0xFFC6C9CE),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                ProcessStepKind.Tool, ProcessStepKind.Error -> Text(
                    step.content,
                    modifier = Modifier.padding(start = 27.dp),
                    color = if (step.kind == ProcessStepKind.Error) AppError else AppMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolCard(title: String, details: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppSurface)
            .combinedClickable(onClick = { expanded = !expanded })
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Terminal, contentDescription = null, tint = AppBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = AppText,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = AppMuted,
            )
        }
        if (expanded) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF3A3C40))
            SelectionContainer {
                Text(
                    details,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFC6C9CE),
                )
            }
        }
    }
}

@Composable
private fun MarkdownText(text: String, color: Color, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseMarkdown(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownInline(
                    block.text,
                    color,
                    TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = when (block.level) {
                            1 -> 23.sp
                            2 -> 20.sp
                            else -> 18.sp
                        },
                        lineHeight = when (block.level) {
                            1 -> 31.sp
                            2 -> 28.sp
                            else -> 26.sp
                        },
                    ),
                )
                is MarkdownBlock.Paragraph -> MarkdownInline(
                    block.text,
                    color,
                    TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
                )
                is MarkdownBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    block.items.forEachIndexed { index, item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                if (block.ordered) "${index + 1}." else "•",
                                modifier = Modifier.width(25.dp),
                                color = color,
                                fontSize = 17.sp,
                                lineHeight = 27.sp,
                            )
                            MarkdownInline(
                                item,
                                color,
                                TextStyle(fontSize = 17.sp, lineHeight = 27.sp),
                                Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MarkdownBlock.Quote -> Row {
                    Box(
                        Modifier
                            .width(2.dp)
                            .heightIn(min = 24.dp)
                            .background(AppBlue, RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    MarkdownInline(
                        block.text,
                        AppMuted,
                        TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
                        Modifier.weight(1f),
                    )
                }
                is MarkdownBlock.Code -> SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF191A1D), RoundedCornerShape(4.dp))
                            .padding(horizontal = 13.dp, vertical = 12.dp),
                    ) {
                        if (block.language.isNotBlank()) {
                            Text(block.language, color = AppMuted, fontSize = 11.sp)
                            Spacer(Modifier.height(7.dp))
                        }
                        Text(
                            block.code,
                            color = Color(0xFFD4D7DC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }
                is MarkdownBlock.Table -> MarkdownTable(block, color)
                MarkdownBlock.Rule -> HorizontalDivider(color = Color(0xFF2B2D31))
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListBlock(val ordered: Boolean, val items: List<String>) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val language: String, val code: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data object Rule : MarkdownBlock
}

private fun parseMarkdown(text: String): List<MarkdownBlock> {
    val lines = text.replace("\r\n", "\n").lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index].trimEnd()
        if (line.isBlank()) {
            index++
            continue
        }
        if (line.trimStart().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim()
            val code = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                code += lines[index]
                index++
            }
            if (index < lines.size) index++
            blocks += MarkdownBlock.Code(language, code.joinToString("\n"))
            continue
        }
        if (index + 1 < lines.size && isTableDivider(lines[index + 1]) && line.contains('|')) {
            val headers = splitTableRow(line)
            val rows = mutableListOf<List<String>>()
            index += 2
            while (index < lines.size && lines[index].contains('|') && lines[index].isNotBlank()) {
                rows += splitTableRow(lines[index])
                index++
            }
            blocks += MarkdownBlock.Table(headers, rows)
            continue
        }
        val headingMatch = Regex("^(#{1,6})\\s+(.+)$").matchEntire(line.trim())
        if (headingMatch != null) {
            blocks += MarkdownBlock.Heading(
                headingMatch.groupValues[1].length,
                headingMatch.groupValues[2],
            )
            index++
            continue
        }
        if (isHorizontalRule(line)) {
            blocks += MarkdownBlock.Rule
            index++
            continue
        }
        val listMatch = Regex("^\\s*([-+*]|\\d+[.)])\\s+(.+)$").matchEntire(line)
        if (listMatch != null) {
            val ordered = listMatch.groupValues[1].first().isDigit()
            val items = mutableListOf<String>()
            while (index < lines.size) {
                val match = Regex("^\\s*([-+*]|\\d+[.)])\\s+(.+)$").matchEntire(lines[index]) ?: break
                if (match.groupValues[1].first().isDigit() != ordered) break
                items += match.groupValues[2]
                index++
            }
            blocks += MarkdownBlock.ListBlock(ordered, items)
            continue
        }
        if (line.trimStart().startsWith(">")) {
            val quote = mutableListOf<String>()
            while (index < lines.size && lines[index].trimStart().startsWith(">")) {
                quote += lines[index].trimStart().removePrefix(">").trimStart()
                index++
            }
            blocks += MarkdownBlock.Quote(quote.joinToString(" "))
            continue
        }
        val paragraph = mutableListOf(line.trim())
        index++
        while (index < lines.size && lines[index].isNotBlank() && !startsMarkdownBlock(lines, index)) {
            paragraph += lines[index].trim()
            index++
        }
        blocks += MarkdownBlock.Paragraph(paragraph.joinToString(" "))
    }
    return blocks
}

private fun startsMarkdownBlock(lines: List<String>, index: Int): Boolean {
    val line = lines[index]
    return line.trimStart().startsWith("```") ||
        line.trimStart().startsWith(">") ||
        Regex("^(#{1,6})\\s+(.+)$").matches(line.trim()) ||
        Regex("^\\s*([-+*]|\\d+[.)])\\s+(.+)$").matches(line) ||
        isHorizontalRule(line) ||
        (index + 1 < lines.size && line.contains('|') && isTableDivider(lines[index + 1]))
}

private fun isHorizontalRule(line: String): Boolean {
    val compact = line.trim().replace(" ", "")
    return compact.length >= 3 && compact.all { it == '-' } ||
        compact.length >= 3 && compact.all { it == '*' } ||
        compact.length >= 3 && compact.all { it == '_' }
}

private fun isTableDivider(line: String): Boolean = splitTableRow(line).let { cells ->
    cells.isNotEmpty() && cells.all { it.trim().matches(Regex(":?-{3,}:?")) }
}

private fun splitTableRow(line: String): List<String> = line.trim().trim('|').split('|').map { it.trim() }

@Composable
private fun MarkdownInline(
    text: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val annotated = remember(text) { inlineMarkdown(text) }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = color),
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                runCatching { uriHandler.openUri(annotation.item) }
            }
        },
    )
}

private fun inlineMarkdown(text: String): AnnotatedString {
    val pattern = Regex("""\[([^]]+)]\(([^)]+)\)|\*\*([^*]+)\*\*|__([^_]+)__|`([^`]+)`|~~([^~]+)~~|\*([^*]+)\*""")
    return AnnotatedString.Builder().apply {
        var cursor = 0
        pattern.findAll(text).forEach { match ->
            append(text.substring(cursor, match.range.first))
            when {
                match.groupValues[1].isNotEmpty() -> {
                    pushStringAnnotation("URL", match.groupValues[2])
                    pushStyle(SpanStyle(color = AppBlue, textDecoration = TextDecoration.Underline))
                    append(match.groupValues[1])
                    pop()
                    pop()
                }
                match.groupValues[3].isNotEmpty() || match.groupValues[4].isNotEmpty() -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(match.groupValues[3].ifEmpty { match.groupValues[4] })
                    pop()
                }
                match.groupValues[5].isNotEmpty() -> {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF2A2C30)))
                    append(match.groupValues[5])
                    pop()
                }
                match.groupValues[6].isNotEmpty() -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(match.groupValues[6])
                    pop()
                }
                else -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(match.groupValues[7])
                    pop()
                }
            }
            cursor = match.range.last + 1
        }
        append(text.substring(cursor))
    }.toAnnotatedString()
}

@Composable
private fun MarkdownTable(table: MarkdownBlock.Table, color: Color) {
    val columns = table.headers.size.coerceAtLeast(1)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = maxOf(136.dp, maxWidth / columns)
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Column(modifier = Modifier.width(columnWidth * columns)) {
                MarkdownTableRow(table.headers, columns, columnWidth, color, header = true)
                table.rows.forEach { row ->
                    HorizontalDivider(color = Color(0xFF2A2C30))
                    MarkdownTableRow(row, columns, columnWidth, color, header = false)
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(
    cells: List<String>,
    columns: Int,
    columnWidth: Dp,
    color: Color,
    header: Boolean,
) {
    Row {
        repeat(columns) { index ->
            Box(
                modifier = Modifier
                    .width(columnWidth)
                    .padding(horizontal = 10.dp, vertical = 13.dp),
            ) {
                MarkdownInline(
                    cells.getOrElse(index) { "" },
                    if (header) AppText else color,
                    TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = if (header) FontWeight.Medium else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Composer(
    modifier: Modifier = Modifier,
    input: String,
    running: Boolean,
    configured: Boolean,
    connectionIndicator: ConnectionIndicator,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    model: String,
    reasoningEffort: String,
    availableModels: List<CodexModelOption>,
    modelsRefreshing: Boolean,
    modelRefreshError: String?,
    onRefreshModels: () -> Unit,
    onModelSelected: (String) -> Unit,
    onEffortSelected: (String) -> Unit,
    attachments: List<MessageAttachment> = emptyList(),
    location: PhoneLocation? = null,
    onPickImages: () -> Unit = {},
    onRemoveAttachment: (MessageAttachment) -> Unit = {},
    onAttachLocation: () -> Unit = {},
    onRemoveLocation: () -> Unit = {},
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (attachments.isNotEmpty() || location != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                attachments.forEach { attachment ->
                    AttachmentThumbnail(attachment, onRemove = { onRemoveAttachment(attachment) })
                }
                location?.let { LocationChip(it, onRemoveLocation) }
            }
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = AppSurface,
            shadowElevation = 3.dp,
        ) {
            Column(modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 6.dp)) {
                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 5,
                    placeholder = { Text("给 Comso1 发消息") },
                    leadingIcon = { ConnectionStatusDot(connectionIndicator) },
                    enabled = !running,
                    colors = transparentTextFieldColors(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPickImages, enabled = !running) {
                        Icon(Icons.Rounded.AttachFile, contentDescription = "添加图片", tint = AppMuted)
                    }
                    IconButton(onClick = onAttachLocation, enabled = !running) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = "附加当前位置",
                            tint = if (location == null) AppMuted else AppBlue,
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        ModelEffortPicker(
                            model = model,
                            reasoningEffort = reasoningEffort,
                            enabled = !running,
                            availableModels = availableModels,
                            refreshing = modelsRefreshing,
                            refreshError = modelRefreshError,
                            onRefresh = onRefreshModels,
                            onModelSelected = onModelSelected,
                            onEffortSelected = onEffortSelected,
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            keyboard?.hide()
                            if (running) onStop() else onSend()
                        },
                        enabled = running || configured &&
                            (input.isNotBlank() || attachments.isNotEmpty() || location != null),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            if (running) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                            contentDescription = if (running) "停止" else "发送",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusDot(state: ConnectionIndicator) {
    val color = when (state) {
        ConnectionIndicator.Connected -> AppBlue
        ConnectionIndicator.Error -> AppError
        ConnectionIndicator.Disconnected,
        ConnectionIndicator.Connecting -> Color(0xFF55585E)
    }
    Box(
        Modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun AttachmentThumbnail(attachment: MessageAttachment, onRemove: (() -> Unit)?) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = attachment.localPath,
    ) {
        value = withContext(Dispatchers.IO) {
            decodeSampledBitmap(attachment.localPath, 320)?.asImageBitmap()
        }
    }
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppSurfaceHigh),
    ) {
        val thumbnail = bitmap
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = attachment.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Rounded.Image,
                contentDescription = attachment.name,
                tint = AppMuted,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "移除图片", modifier = Modifier.size(15.dp))
            }
        }
    }
}

private fun decodeSampledBitmap(path: String, maxDimension: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > maxDimension * 2 ||
        bounds.outHeight / sampleSize > maxDimension * 2
    ) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        },
    )
}

@Composable
private fun LocationChip(location: PhoneLocation, onRemove: (() -> Unit)?) {
    Surface(shape = RoundedCornerShape(12.dp), color = AppBlueContainer) {
        Row(
            modifier = Modifier.padding(start = 9.dp, end = 3.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = AppBlue, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                buildString {
                    if (location.isCached) append("最近 · ")
                    append("%.5f, %.5f".format(location.latitude, location.longitude))
                },
                color = Color(0xFFD7E3FF),
                fontSize = 11.sp,
            )
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "移除位置", modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    focusedTextColor = AppText,
    unfocusedTextColor = AppText,
    disabledTextColor = AppMuted,
    focusedPlaceholderColor = AppMuted,
    unfocusedPlaceholderColor = AppMuted,
)

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppText,
    unfocusedTextColor = AppText,
    disabledTextColor = Color(0xFF8B9098),
    cursorColor = Color(0xFFA9BEE2),
    focusedBorderColor = AppBlue,
    unfocusedBorderColor = Color(0xFF4A4D53),
    disabledBorderColor = Color(0xFF303238),
    focusedLabelColor = Color(0xFFDDE4EF),
    unfocusedLabelColor = Color(0xFFB8BDC5),
    disabledLabelColor = Color(0xFF777C84),
    focusedContainerColor = Color(0xFF191B1F),
    unfocusedContainerColor = Color(0xFF191B1F),
    disabledContainerColor = Color(0xFF17191C),
    focusedPlaceholderColor = AppMuted,
    unfocusedPlaceholderColor = AppMuted,
    disabledPlaceholderColor = Color(0xFF6F747C),
)

@Composable
private fun SettingsDialog(
    initial: VpsConfig,
    hasPrivateKey: Boolean,
    initialTitleApi: TitleApiConfig,
    hasTitleApiKey: Boolean,
    onDismiss: () -> Unit,
    onSave: (VpsConfig, String, TitleApiConfig, String) -> Unit,
) {
    var host by rememberSaveable { mutableStateOf(initial.host) }
    var port by rememberSaveable { mutableStateOf(initial.port.toString()) }
    var username by rememberSaveable { mutableStateOf(initial.username) }
    var workDir by rememberSaveable { mutableStateOf(initial.workDir) }
    var codexCommand by rememberSaveable { mutableStateOf(initial.codexCommand) }
    var privateKey by rememberSaveable { mutableStateOf("") }
    var privateKeyPassphrase by rememberSaveable { mutableStateOf("") }
    var titleEnabled by rememberSaveable { mutableStateOf(initialTitleApi.enabled) }
    var titleBaseUrl by rememberSaveable { mutableStateOf(initialTitleApi.baseUrl) }
    var titleModel by rememberSaveable { mutableStateOf(initialTitleApi.model) }
    var titleApiKey by rememberSaveable { mutableStateOf("") }
    var titleTestStatus by rememberSaveable { mutableStateOf("") }
    val titleTestScope = rememberCoroutineScope()
    val titleTestGenerator = remember { ConversationTitleGenerator() }
    val fieldColors = settingsTextFieldColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF15171B),
        titleContentColor = AppText,
        textContentColor = AppText,
        title = { Text("SSH 设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(host, { host = it }, label = { Text("Host / 主机") }, singleLine = true, colors = fieldColors)
                OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, label = { Text("Port / 端口") }, singleLine = true, colors = fieldColors)
                OutlinedTextField(username, { username = it }, label = { Text("Username / 用户名") }, singleLine = true, colors = fieldColors)
                OutlinedTextField(workDir, { workDir = it }, label = { Text("工作目录") }, singleLine = true, colors = fieldColors)
                OutlinedTextField(codexCommand, { codexCommand = it }, label = { Text("Codex 命令") }, singleLine = true, colors = fieldColors)
                OutlinedTextField(
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    label = { Text(if (hasPrivateKey) "私钥（留空则保留已保存私钥）" else "私钥") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = privateKeyPassphrase,
                    onValueChange = { privateKeyPassphrase = it },
                    label = { Text("私钥口令（没有则留空）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
                Text(
                    "建议使用 VPS 上的 codex 用户。私钥会在本机加密保存，不写入日志。",
                    fontSize = 12.sp,
                    color = AppMuted,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF34363A))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("自动生成会话标题", color = AppText, fontWeight = FontWeight.Medium)
                        Text("首次回复完成后发送首轮内容到所配置的模型 API", color = AppMuted, fontSize = 11.sp)
                    }
                    Switch(checked = titleEnabled, onCheckedChange = { titleEnabled = it })
                }
                OutlinedTextField(
                    titleBaseUrl,
                    { titleBaseUrl = it },
                    label = { Text("标题 API Base URL") },
                    singleLine = true,
                    enabled = titleEnabled,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    titleModel,
                    { titleModel = it },
                    label = { Text("标题模型") },
                    singleLine = true,
                    enabled = titleEnabled,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = titleApiKey,
                    onValueChange = { titleApiKey = it },
                    label = { Text(if (hasTitleApiKey) "标题 API Key（留空则保留）" else "标题 API Key") },
                    singleLine = true,
                    enabled = titleEnabled,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = fieldColors,
                )
                Text(
                    "默认使用 DeepSeek deepseek-flash。API Key 由 Android Keystore 加密；启用后首轮消息和回复会发送给该服务生成标题。",
                    fontSize = 11.sp,
                    color = AppMuted,
                )
                Text(
                    when {
                        !titleEnabled -> "当前状态：未启用"
                        titleApiKey.isNotBlank() || hasTitleApiKey -> "当前状态：已配置，首轮完成后自动生成"
                        else -> "当前状态：缺少 API Key"
                    },
                    fontSize = 11.sp,
                    color = if (titleEnabled && (titleApiKey.isNotBlank() || hasTitleApiKey)) AppBlue else AppMuted,
                )
                OutlinedButton(
                    onClick = {
                        val testKey = titleApiKey.ifBlank { initialTitleApi.apiKey }
                        if (testKey.isBlank()) {
                            titleTestStatus = "请先填写标题 API Key"
                        } else {
                            titleTestStatus = "正在测试标题 API..."
                            titleTestScope.launch {
                                runCatching {
                                    titleTestGenerator.generate(
                                        TitleApiConfig(
                                            enabled = true,
                                            baseUrl = titleBaseUrl.trim(),
                                            model = titleModel.trim(),
                                            apiKey = testKey,
                                        ),
                                        userMessage = "请检查 VPS 服务状态",
                                        assistantMessage = "已完成服务状态检查，主要服务运行正常。",
                                    )
                                }.onSuccess { titleTestStatus = "测试成功：$it" }
                                    .onFailure { titleTestStatus = "测试失败：${it.message ?: "未知错误"}" }
                            }
                        }
                    },
                    enabled = titleEnabled && titleTestStatus != "正在测试标题 API...",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("测试标题 API", color = Color(0xFFC2D1EA))
                }
                if (titleTestStatus.isNotBlank()) {
                    Text(
                        titleTestStatus,
                        fontSize = 11.sp,
                        color = if (titleTestStatus.startsWith("测试成功")) AppBlue else AppMuted,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        VpsConfig(
                            host = host,
                            port = port.toIntOrNull() ?: 22,
                            username = username.ifBlank { "codex" },
                            workDir = workDir.ifBlank { "/home/codex" },
                            codexCommand = codexCommand.ifBlank { "codex" },
                            model = initial.model,
                            reasoningEffort = initial.reasoningEffort,
                            privateKeyPassphrase = privateKeyPassphrase,
                        ),
                        privateKey,
                        TitleApiConfig(
                            enabled = titleEnabled,
                            baseUrl = titleBaseUrl,
                            model = titleModel,
                            apiKey = initialTitleApi.apiKey,
                        ),
                        titleApiKey,
                    )
                },
                enabled = host.isNotBlank(),
            ) {
                Text("保存", color = Color(0xFFF4F7FF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFFC2D1EA))
            }
        },
    )
}
