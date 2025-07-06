package com.example.personalassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, RecognitionListener {

    // Load the native library
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        // Add permission request code for storage if needed later
        // private const val REQUEST_WRITE_STORAGE_PERMISSION = 201 
        init {
            try {
                System.loadLibrary("AssistantCore") // Matches library name in CMake
                Log.i(TAG, "Native library loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Error loading native library: ", e)
            }
        }
    }

    // Declare native methods
    private external fun initializeCore(): Long // Returns pointer to CoreLogic instance
    private external fun processCoreInput(corePtr: Long, inputText: String): String
    private external fun destroyCore(corePtr: Long)
    private external fun onCoreVoiceRecognized(corePtr: Long, recognizedText: String) // From platform to core

    private var coreLogicPtr: Long = 0
    private lateinit var statusTextView: TextView
    private lateinit var tts: TextToSpeech
    private var ttsInitialized = false
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Basic UI setup (replace with actual UI later)
        statusTextView = TextView(this)
        statusTextView.textSize = 18f
        setContentView(statusTextView)

        statusTextView.text = "Initializing Assistant Core..."

        // Initialize TTS
        tts = TextToSpeech(this, this)

        // Initialize STT
        setupSpeechRecognizer()

        // Request permissions
        requestAudioPermission()
        // Request storage permissions if needed for downloads
        // requestStoragePermission()

        // Initialize the core logic via JNI 
        coreLogicPtr = initializeCore()

        if (coreLogicPtr != 0L) {
            statusTextView.append("\nCore Initialized.")
            // Example: Process a command
            val response = processCoreInput(coreLogicPtr, "hello")
            statusTextView.append("\nResponse from Core: $response")

            val timeResponse = processCoreInput(coreLogicPtr, "what time is it")
            statusTextView.append("\nTime Response: $timeResponse")
        } else {
            statusTextView.append("\nError initializing Core.")
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available on this device.")
            statusTextView.append("\nSpeech recognition not available.")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) 
        }
        Log.i(TAG, "SpeechRecognizer initialized.")
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            Log.i(TAG, "Record audio permission already granted.")
        }
    }
    
    // Example for storage permission (needed for downloads)
    /*
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE_PERMISSION)
            } else {
                 Log.i(TAG, "Write storage permission already granted.")
            }
        } else {
             Log.i(TAG, "Write storage permission not required below Marshmallow.")
        }
    }
    */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Record audio permission granted by user.")
                } else {
                    Log.w(TAG, "Record audio permission denied by user.")
                    statusTextView.append("\nAudio permission denied. Voice input disabled.")
                }
            }
            // Handle storage permission result if implemented
            /*
            REQUEST_WRITE_STORAGE_PERMISSION -> {
                 if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Write storage permission granted by user.")
                } else {
                    Log.w(TAG, "Write storage permission denied by user.")
                    statusTextView.append("\nStorage permission denied. File downloads may fail.")
                }
            }
            */
        }
    }

    // --- TextToSpeech.OnInitListener --- 
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US) // Set default language
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS language not supported or missing data.")
                statusTextView.append("\nTTS Language not supported.")
            } else {
                ttsInitialized = true
                Log.i(TAG, "TTS Initialized successfully.")
                statusTextView.append("\nTTS Ready.")
            }
        } else {
            Log.e(TAG, "TTS Initialization failed! Status: $status")
            statusTextView.append("\nTTS Initialization Failed.")
        }
    }

    // --- SpeechRecognizer.RecognitionListener --- 
    override fun onReadyForSpeech(params: Bundle?) { Log.d(TAG, "onReadyForSpeech"); statusTextView.append("\nListening...") }
    override fun onBeginningOfSpeech() { Log.d(TAG, "onBeginningOfSpeech") }
    override fun onRmsChanged(rmsdB: Float) { /* Log.d(TAG, "onRmsChanged: $rmsdB") */ }
    override fun onBufferReceived(buffer: ByteArray?) { Log.d(TAG, "onBufferReceived") }
    override fun onEndOfSpeech() { Log.d(TAG, "onEndOfSpeech"); statusTextView.append("\nProcessing...") }
    override fun onError(error: Int) {
        val errorMessage = getErrorText(error)
        Log.e(TAG, "onError: $errorMessage")
        statusTextView.append("\nSTT Error: $errorMessage")
        isListening = false // Ensure listening stops on error
    }
    override fun onResults(results: Bundle?) {
        isListening = false // Recognition finished
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            val recognizedText = matches[0]
            Log.i(TAG, "onResults: $recognizedText")
            statusTextView.append("\nRecognized: $recognizedText")
            // Send recognized text to the core logic
            if (coreLogicPtr != 0L) {
                onCoreVoiceRecognized(coreLogicPtr, recognizedText)
            }
        } else {
            Log.w(TAG, "onResults: No recognition results.")
            statusTextView.append("\nCould not recognize speech.")
        }
    }
    override fun onPartialResults(partialResults: Bundle?) { Log.d(TAG, "onPartialResults") }
    override fun onEvent(eventType: Int, params: Bundle?) { Log.d(TAG, "onEvent") }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown speech recognition error"
        }
    }

    // --- Callback functions to be called FROM C++ --- 
    fun speak(text: String) {
        Log.i(TAG, "Speak request from Core: $text")
        if (ttsInitialized) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_ADD, null, "utteranceId_${System.currentTimeMillis()}")
            } else {
                @Suppress("DEPRECATION")
                tts.speak(text, TextToSpeech.QUEUE_ADD, null)
            }
            runOnUiThread {
                statusTextView.append("\nAssistant Says: $text")
            }
        } else {
            Log.e(TAG, "TTS not initialized, cannot speak.")
             runOnUiThread {
                statusTextView.append("\nTTS Error: Cannot speak 	'$text'	")
            }
        }
    }

    fun startListening() {
        Log.i(TAG, "Start listening request from Core")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
             if (!isListening) {
                 runOnUiThread {
                     speechRecognizer.startListening(recognizerIntent)
                     isListening = true
                 }
             } else {
                 Log.w(TAG, "Already listening.")
             }
        } else {
            Log.e(TAG, "Cannot start listening, audio permission not granted.")
            runOnUiThread {
                statusTextView.append("\nError: Audio permission needed to listen.")
            }
            requestAudioPermission() // Re-request permission
        }
    }

     fun stopListening() {
        Log.i(TAG, "Stop listening request from Core")
         if (isListening) {
             runOnUiThread {
                 speechRecognizer.stopListening() // This might trigger onError or onResults
                 isListening = false
                 statusTextView.append("\nAssistant: Stopping Listening (Platform)")
             }
         } else {
             Log.w(TAG, "Not currently listening.")
         }
    }

    // Placeholder for Upload
    fun uploadFile(localFilePath: String) {
        Log.i(TAG, "Upload file request from Core: $localFilePath")
        // TODO: Implement actual file upload logic here (e.g., using Retrofit, Volley, WorkManager)
        // Requires network permissions and likely storage permissions.
         runOnUiThread {
            statusTextView.append("\nAssistant: Uploading $localFilePath ... (Placeholder)")
            // Simulate success/failure after delay?
            speak("Starting upload for $localFilePath")
        }
    }

    // Placeholder for List Files
    fun listFiles(cloudPath: String) {
        Log.i(TAG, "List files request from Core for path: $cloudPath")
        // TODO: Implement actual cloud listing logic here
        // Requires network permissions.
        runOnUiThread {
            statusTextView.append("\nAssistant: Listing files in $cloudPath ... (Placeholder)")
            // Simulate finding some files
            val dummyFiles = listOf("file1.txt", "image.jpg", "document.pdf")
            val fileListString = dummyFiles.joinToString(", ")
            speak("Files in $cloudPath are: $fileListString")
        }
    }

    // Placeholder for Download File
    fun downloadFile(cloudFilePath: String, localSavePath: String) {
        Log.i(TAG, "Download file request from Core: $cloudFilePath to $localSavePath")
        // TODO: Implement actual file download logic here
        // Requires network permissions and storage permissions (WRITE_EXTERNAL_STORAGE before API 29, scoped storage after).
        runOnUiThread {
            statusTextView.append("\nAssistant: Downloading $cloudFilePath to $localSavePath ... (Placeholder)")
            // Simulate success
            speak("Downloaded $cloudFilePath to $localSavePath")
        }
    }
    // --- End Callbacks ---

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown TTS
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
            Log.i(TAG, "TTS shutdown.")
        }
        // Shutdown STT
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
            Log.i(TAG, "SpeechRecognizer destroyed.")
        }
        // Clean up the core logic instance
        if (coreLogicPtr != 0L) {
            destroyCore(coreLogicPtr)
            coreLogicPtr = 0L
            Log.i(TAG, "CoreLogic instance destroyed.")
        }
    }
}

