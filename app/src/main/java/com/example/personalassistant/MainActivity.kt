package com.example.personalassistant

import android.Manifest
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
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, RecognitionListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    private val assistantEngine = AssistantEngine()

    private lateinit var tvChat: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etInput: EditText
    private lateinit var btnSend: Button
    private lateinit var btnMic: ImageButton
    private lateinit var scrollView: ScrollView

    private lateinit var tts: TextToSpeech
    private var ttsInitialized = false

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var isListening = false

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvChat = findViewById(R.id.tvChat)
        tvStatus = findViewById(R.id.tvStatus)
        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        scrollView = findViewById(R.id.scrollView)

        tts = TextToSpeech(this, this)
        setupSpeechRecognizer()
        requestAudioPermission()

        btnSend.setOnClickListener { sendMessage() }
        btnMic.setOnClickListener { toggleVoiceInput() }
        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        appendMessage("Assistant", "Hello! I'm your Personal Assistant. Type or speak a message to get started.")
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

    // -------------------------------------------------------------------------
    // Messaging
    // -------------------------------------------------------------------------

    private fun sendMessage() {
        val input = etInput.text.toString().trim()
        if (input.isEmpty()) return
        etInput.text.clear()
        appendMessage("You", input)
        // Disable send controls while waiting for the (potentially async) response
        btnSend.isEnabled = false
        btnMic.isEnabled = false
        setStatus(getString(R.string.status_processing))
        lifecycleScope.launch {
            val response = assistantEngine.processInput(input)
            appendMessage("Assistant", response)
            speak(response)
            btnSend.isEnabled = true
            btnMic.isEnabled = true
            setStatus(getString(R.string.status_ready))
        }
    }

    private fun handleVoiceInput(recognizedText: String) {
        appendMessage("You (voice)", recognizedText)
        btnSend.isEnabled = false
        btnMic.isEnabled = false
        setStatus(getString(R.string.status_processing))
        lifecycleScope.launch {
            val response = assistantEngine.processInput(recognizedText)
            appendMessage("Assistant", response)
            speak(response)
            btnSend.isEnabled = true
            btnMic.isEnabled = true
            setStatus(getString(R.string.status_ready))
        }
    }

    private fun appendMessage(sender: String, message: String) {
        runOnUiThread {
            val current = tvChat.text.toString()
            val separator = if (current.isEmpty()) "" else "\n\n"
            tvChat.text = "$current$separator$sender: $message"
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    private fun setStatus(status: String) {
        runOnUiThread { tvStatus.text = status }
    }

    // -------------------------------------------------------------------------
    // Text-to-Speech
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Speech-to-Text
    // -------------------------------------------------------------------------

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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
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

    override fun onReadyForSpeech(params: Bundle?) {
        setStatus(getString(R.string.status_listening))
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        setStatus(getString(R.string.status_processing))
    }

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

    // -------------------------------------------------------------------------
    // Permissions
    // -------------------------------------------------------------------------

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
