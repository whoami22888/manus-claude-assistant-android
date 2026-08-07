package com.manus.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, RecognitionListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    private val assistantEngine by lazy { AssistantEngine(this) }
    private val agentPlanStore by lazy { AgentPlanStore(this) }
    private val cloudStorageManager by lazy { CloudStorageManager(this) }
    private val pythonScriptManager by lazy { PythonScriptManager(this) }
    private val skillsManager by lazy { SkillsManager(this) }
    private val terminalManager by lazy { TerminalManager(filesDir) }

    // Chat UI
    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private lateinit var btnSend: Button
    private lateinit var btnMic: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnResearch: Button
    private lateinit var btnPlan: Button
    private lateinit var btnSkills: Button
    private lateinit var btnAutomations: Button
    private lateinit var btnConnect: Button

    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    // TTS
    private lateinit var tts: TextToSpeech
    private var ttsInitialized = false

    // STT
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var isListening = false
    private var pendingOpenDocumentAction = OpenDocumentAction.LOCAL_UPLOAD
    private var pendingCreateDocumentAction = CreateDocumentAction.LOCAL_DOWNLOAD

    // -----------------------------------------------------------------------
    // Storage Access Framework launchers
    // -----------------------------------------------------------------------

    /** Picks any file from device storage (upload). */
    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val fileName = getFileNameFromUri(uri)
                val message = when (pendingOpenDocumentAction) {
                    OpenDocumentAction.LOCAL_UPLOAD ->
                        getString(R.string.file_upload_success, fileName)
                    OpenDocumentAction.CLOUD_UPLOAD ->
                        cloudStorageManager.uploadPlaceholder(fileName)
                    OpenDocumentAction.PYTHON_SCRIPT ->
                        pythonScriptManager.loadScript(uri, contentResolver)
                }
                appendMessage(ChatMessage.Sender.SYSTEM, message)
            } else {
                val cancelledMessage = when (pendingOpenDocumentAction) {
                    OpenDocumentAction.PYTHON_SCRIPT ->
                        getString(R.string.python_script_load_cancelled)
                    else ->
                        getString(R.string.file_upload_cancelled)
                }
                appendMessage(ChatMessage.Sender.SYSTEM, cancelledMessage)
            }
        }

    /** Creates a new text document on device storage (download/save). */
    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) {
                try {
                    val contents = when (pendingCreateDocumentAction) {
                        CreateDocumentAction.LOCAL_DOWNLOAD ->
                            "Personal Assistant note — saved by the app.\n"
                        CreateDocumentAction.CLOUD_DOWNLOAD ->
                            cloudStorageManager.createDownloadPlaceholder().contents
                    }
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(contents.toByteArray(Charsets.UTF_8))
                    }
                    val successMessage = when (pendingCreateDocumentAction) {
                        CreateDocumentAction.LOCAL_DOWNLOAD ->
                            getString(R.string.file_download_saved)
                        CreateDocumentAction.CLOUD_DOWNLOAD ->
                            getString(R.string.cloud_download_saved)
                    }
                    appendMessage(ChatMessage.Sender.SYSTEM, successMessage)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write file: ${e.message}")
                    val errorMessage = when (pendingCreateDocumentAction) {
                        CreateDocumentAction.LOCAL_DOWNLOAD ->
                            getString(R.string.file_download_error)
                        CreateDocumentAction.CLOUD_DOWNLOAD ->
                            getString(R.string.cloud_download_error)
                    }
                    appendMessage(ChatMessage.Sender.SYSTEM, errorMessage)
                }
            } else {
                val cancelledMessage = when (pendingCreateDocumentAction) {
                    CreateDocumentAction.LOCAL_DOWNLOAD ->
                        getString(R.string.file_download_cancelled)
                    CreateDocumentAction.CLOUD_DOWNLOAD ->
                        getString(R.string.cloud_download_cancelled)
                }
                appendMessage(ChatMessage.Sender.SYSTEM, cancelledMessage)
            }
        }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        tvStatus         = findViewById(R.id.tvStatus)
        etInput          = findViewById(R.id.etInput)
        btnSend          = findViewById(R.id.btnSend)
        btnMic           = findViewById(R.id.btnMic)
        btnSettings      = findViewById(R.id.btnSettings)
        btnResearch      = findViewById(R.id.btnResearch)
        btnPlan          = findViewById(R.id.btnPlan)
        btnSkills        = findViewById(R.id.btnSkills)
        btnAutomations   = findViewById(R.id.btnAutomations)
        btnConnect       = findViewById(R.id.btnConnect)

        chatAdapter = ChatAdapter(chatMessages)
        recyclerViewChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).also {
                it.stackFromEnd = true
            }
        }

        tts = TextToSpeech(this, this)
        setupSpeechRecognizer()
        requestAudioPermission()

        btnSend.setOnClickListener { sendMessage() }
        btnMic.setOnClickListener { toggleVoiceInput() }
        btnSettings.setOnClickListener { showApiKeyDialog() }
        btnResearch.setOnClickListener { appendMessage(ChatMessage.Sender.SYSTEM, "Research opens a planning prompt; verify sources before acting.") }
        btnPlan.setOnClickListener { showCurrentPlan() }
        btnSkills.setOnClickListener { routeInput("skills dashboard") }
        btnAutomations.setOnClickListener { appendMessage(ChatMessage.Sender.SYSTEM, "Automations are disabled in this shell. Any future run needs explicit approval.") }
        btnConnect.setOnClickListener { appendMessage(ChatMessage.Sender.SYSTEM, "Connections require owner setup and explicit approval; no account is connected.") }
        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        appendMessage(
            ChatMessage.Sender.ASSISTANT,
            "Hello! I'm your Personal Assistant. Type or speak a message to get started."
        )
        setStatus(getString(R.string.status_ready))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
            Log.i(TAG, "TTS shut down.")
        }
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // -----------------------------------------------------------------------
    // Messaging
    // -----------------------------------------------------------------------

    private fun sendMessage() {
        val input = etInput.text.toString().trim()
        if (input.isEmpty()) return
        etInput.text.clear()
        routeInput(input)
    }

    private fun handleVoiceInput(recognizedText: String) {
        routeInput(recognizedText)
    }

    /**
     * Routes [input] to the appropriate handler before handing off to the
     * assistant engine:
     * - local / cloud file actions → SAF placeholders
     * - Python script loading      → asset or document loader
     * - "clear"                    → reset conversation history
     * - everything else            → AssistantEngine (backend + local fallback)
     */
    private fun routeInput(input: String) {
        val lower = input.lowercase(Locale.getDefault())
        appendMessage(ChatMessage.Sender.USER, input)
        when {
            lower.startsWith("plan ") -> {
                val goal = input.substringAfter("plan", "").trim()
                if (goal.isBlank()) {
                    appendMessage(ChatMessage.Sender.SYSTEM, "Usage: plan <goal>. Plans stay on this device and never run actions automatically.")
                } else {
                    val plan = AgentPlanner.create(goal)
                    agentPlanStore.save(plan)
                    appendMessage(ChatMessage.Sender.SYSTEM, plan.summary())
                }
            }
            lower == "show plan" || lower == "current plan" -> showCurrentPlan()
            lower.startsWith("complete task ") -> {
                val taskId = input.substringAfter("complete task", "").trim().lowercase(Locale.getDefault())
                val current = agentPlanStore.load()
                if (current == null) appendMessage(ChatMessage.Sender.SYSTEM, "No local plan yet. Start with: plan <goal>")
                else {
                    val update = current.complete(taskId)
                    agentPlanStore.save(update.plan)
                    appendMessage(ChatMessage.Sender.SYSTEM, update.message)
                }
            }
            lower == "reset plan" -> {
                val current = agentPlanStore.load()
                if (current == null) appendMessage(ChatMessage.Sender.SYSTEM, "No local plan to reset.")
                else {
                    agentPlanStore.save(current.reset())
                    appendMessage(ChatMessage.Sender.SYSTEM, "Plan reset. ${current.reset().progressMessage()}")
                }
            }
            lower == "skills dashboard" -> {
                appendMessage(
                    ChatMessage.Sender.SYSTEM,
                    skillsManager.dashboardMessage(assistantEngine.isTurboModeEnabled())
                )
            }
            lower == "list skills" || lower == "show skills" -> {
                appendMessage(ChatMessage.Sender.SYSTEM, skillsManager.listSkillsMessage())
            }
            lower.startsWith("add skill ") -> {
                val name = input.substringAfter("add skill", "").trim()
                appendMessage(ChatMessage.Sender.SYSTEM, skillsManager.addSkill(name))
            }
            lower.startsWith("remove skill ") -> {
                val name = input.substringAfter("remove skill", "").trim()
                appendMessage(ChatMessage.Sender.SYSTEM, skillsManager.removeSkill(name))
            }
            lower.startsWith("update skill ") -> {
                val payload = input.substringAfter("update skill", "").trim()
                val parts = payload.split(" to ", limit = 2)
                val msg = if (parts.size == 2) {
                    skillsManager.updateSkill(parts[0], parts[1])
                } else {
                    "Usage: update skill <old name> to <new name>"
                }
                appendMessage(ChatMessage.Sender.SYSTEM, msg)
            }
            lower == "terminal help" -> {
                appendMessage(ChatMessage.Sender.SYSTEM, terminalManager.run("help"))
            }
            lower.startsWith("terminal run ") -> {
                val command = input.substringAfter("terminal run", "").trim()
                appendMessage(ChatMessage.Sender.SYSTEM, terminalManager.run(command))
            }
            lower == "turbo on" -> {
                assistantEngine.setTurboMode(true)
                appendMessage(ChatMessage.Sender.SYSTEM, "Turbo mode enabled.")
            }
            lower == "turbo off" -> {
                assistantEngine.setTurboMode(false)
                appendMessage(ChatMessage.Sender.SYSTEM, "Turbo mode disabled.")
            }
            lower == "turbo status" -> {
                appendMessage(
                    ChatMessage.Sender.SYSTEM,
                    "Turbo mode is ${if (assistantEngine.isTurboModeEnabled()) "ON" else "OFF"}."
                )
            }
            lower.contains("upload to cloud") || lower.contains("cloud upload") -> {
                pendingOpenDocumentAction = OpenDocumentAction.CLOUD_UPLOAD
                openDocumentLauncher.launch(arrayOf("*/*"))
            }
            lower.contains("download from cloud") || lower.contains("cloud download") -> {
                pendingCreateDocumentAction = CreateDocumentAction.CLOUD_DOWNLOAD
                createDocumentLauncher.launch(cloudStorageManager.createDownloadPlaceholder().fileName)
            }
            lower.contains("list cloud") || lower.contains("cloud files") -> {
                appendMessage(ChatMessage.Sender.SYSTEM, cloudStorageManager.listFilesMessage())
            }
            lower.contains("load example script") -> {
                appendMessage(ChatMessage.Sender.SYSTEM, pythonScriptManager.loadBundledExample())
            }
            lower.contains("load python script") || lower.contains("python script") -> {
                pendingOpenDocumentAction = OpenDocumentAction.PYTHON_SCRIPT
                openDocumentLauncher.launch(arrayOf("text/x-python", "text/plain", "*/*"))
            }
            lower.contains("list scripts") || lower.contains("show scripts") -> {
                appendMessage(ChatMessage.Sender.SYSTEM, pythonScriptManager.listScriptsMessage())
            }
            lower.contains("upload") -> {
                pendingOpenDocumentAction = OpenDocumentAction.LOCAL_UPLOAD
                openDocumentLauncher.launch(arrayOf("*/*"))
            }
            lower.contains("download") -> {
                pendingCreateDocumentAction = CreateDocumentAction.LOCAL_DOWNLOAD
                createDocumentLauncher.launch("assistant_note.txt")
            }
            lower.contains("list files") || lower.contains("show files") -> {
                listLocalFiles()
            }
            lower == "clear" || lower.contains("clear conversation") -> {
                assistantEngine.clearHistory()
                chatMessages.clear()
                chatAdapter.notifyDataSetChanged()
                appendMessage(ChatMessage.Sender.SYSTEM, "Conversation cleared.")
            }
            else -> {
                processWithAssistant(input)
            }
        }
    }

    private fun showCurrentPlan() {
        val plan = agentPlanStore.load()
        appendMessage(
            ChatMessage.Sender.SYSTEM,
            plan?.summary() ?: "No local plan yet. Type: plan <goal>. Plans are review-only and never execute actions automatically."
        )
    }

    private fun processWithAssistant(input: String) {
        btnSend.isEnabled = false
        btnMic.isEnabled = false
        setStatus(getString(R.string.status_processing))
        lifecycleScope.launch {
            try {
                val response = assistantEngine.processInput(input)
                appendMessage(ChatMessage.Sender.ASSISTANT, response)
                speak(response)
            } catch (e: Exception) {
                Log.e(TAG, "Assistant processing failed", e)
                appendMessage(
                    ChatMessage.Sender.SYSTEM,
                    "Sorry, something went wrong while processing that request."
                )
            } finally {
                btnSend.isEnabled = true
                btnMic.isEnabled = true
                setStatus(getString(R.string.status_ready))
            }
        }
    }

    private fun listLocalFiles() {
        val files = (filesDir.listFiles() ?: emptyArray())
            .filter { it.isFile }
            .map { it.name }
        val message = if (files.isEmpty()) {
            getString(R.string.file_list_empty)
        } else {
            getString(R.string.file_list_header) + "\n" + files.joinToString("\n") { "• $it" }
        }
        appendMessage(ChatMessage.Sender.SYSTEM, message)
    }

    private fun appendMessage(sender: ChatMessage.Sender, text: String) {
        runOnUiThread {
            chatMessages.add(ChatMessage(sender, text))
            chatAdapter.notifyItemInserted(chatMessages.size - 1)
            recyclerViewChat.scrollToPosition(chatMessages.size - 1)
        }
    }

    private fun setStatus(status: String) {
        runOnUiThread { tvStatus.text = status }
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // Backend endpoint settings (never accepts provider keys)
    // -----------------------------------------------------------------------

    private fun showApiKeyDialog() {
        val prefs = getSharedPreferences(AssistantEngine.PREFS_NAME, Context.MODE_PRIVATE)
        val editText = EditText(this).apply {
            setText(prefs.getString(AssistantEngine.PREF_BACKEND_URL, BuildConfig.AGENT_BACKEND_URL))
            hint = getString(R.string.backend_url_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.backend_url_title)
            .setMessage(R.string.backend_url_message)
            .setView(editText)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val value = editText.text.toString().trim()
                if (value.isBlank() || BackendUrlConfig.isValid(value)) {
                    prefs.edit().putString(AssistantEngine.PREF_BACKEND_URL, value).apply()
                    appendMessage(ChatMessage.Sender.SYSTEM, if (value.isBlank()) getString(R.string.backend_url_cleared) else getString(R.string.backend_url_saved))
                } else appendMessage(ChatMessage.Sender.SYSTEM, getString(R.string.backend_url_invalid))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    // -----------------------------------------------------------------------
    // Helper — resolve file name from URI
    // -----------------------------------------------------------------------

    private fun getFileNameFromUri(uri: android.net.Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // getColumnIndex returns -1 when the column is absent; guard before use.
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) result = cursor.getString(idx)
                }
            }
        }
        return result ?: uri.lastPathSegment ?: "unknown"
    }

    // -----------------------------------------------------------------------
    // Text-to-Speech
    // -----------------------------------------------------------------------

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            ttsInitialized = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsInitialized) {
                Log.e(TAG, "TTS language not supported.")
            }
        } else {
            Log.e(TAG, "TTS initialization failed, status=$status")
        }
    }

    private fun speak(text: String) {
        if (ttsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "utt_${System.currentTimeMillis()}")
        }
    }

    // -----------------------------------------------------------------------
    // Speech-to-Text
    // -----------------------------------------------------------------------

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Speech recognition not available on this device.")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }

    private fun toggleVoiceInput() {
        if (isListening) stopListeningInternal() else startListeningInternal()
    }

    private fun startListeningInternal() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!isListening && speechRecognizer != null) {
                speechRecognizer?.startListening(recognizerIntent)
                isListening = true
                setStatus(getString(R.string.status_listening))
            }
        } else {
            requestAudioPermission()
        }
    }

    private fun stopListeningInternal() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            setStatus(getString(R.string.status_ready))
        }
    }

    // RecognitionListener callbacks

    override fun onReadyForSpeech(params: Bundle?) { setStatus(getString(R.string.status_listening)) }
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { setStatus(getString(R.string.status_processing)) }

    override fun onError(error: Int) {
        Log.e(TAG, "STT error code: $error")
        isListening = false
        setStatus(getString(R.string.status_ready))
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            handleVoiceInput(matches[0])
        } else {
            setStatus(getString(R.string.status_ready))
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    // -----------------------------------------------------------------------
    // Permissions
    // -----------------------------------------------------------------------

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    private enum class OpenDocumentAction {
        LOCAL_UPLOAD,
        CLOUD_UPLOAD,
        PYTHON_SCRIPT
    }

    private enum class CreateDocumentAction {
        LOCAL_DOWNLOAD,
        CLOUD_DOWNLOAD
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Record audio permission granted.")
            } else {
                setStatus("Audio permission denied — voice input disabled")
            }
        }
    }
}
