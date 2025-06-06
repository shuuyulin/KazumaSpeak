// app/src/main/java/com/example/japaneselearning/ui/dialogs/AddSentenceDialogFragment.kt
package com.example.japaneselearning.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.japaneselearning.R
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.databinding.DialogAddSentenceBinding
import com.example.japaneselearning.ui.viewmodels.SentenceViewModel
import com.example.japaneselearning.utils.AudioManager
import com.example.japaneselearning.utils.SimpleJapaneseRomanizer
import com.google.android.material.chip.Chip
import java.io.File
import java.io.FileOutputStream

class AddSentenceDialogFragment : DialogFragment() {
    private var _binding: DialogAddSentenceBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SentenceViewModel by viewModels()
    private var editingSentence: Sentence? = null
    private lateinit var audioManager: AudioManager
    private lateinit var japaneseRomanizer: SimpleJapaneseRomanizer
    
    private var selectedAudioUri: Uri? = null
    private var audioSourceType: AudioSourceType = AudioSourceType.TTS
    private val categories = mutableSetOf<String>()
    private var selectedCategory: String? = null
    
    enum class AudioSourceType {
        TTS, IMPORTED_FILE
    }
    
    companion object {
        private const val ARG_SENTENCE = "sentence"
        
        fun newInstance(sentence: Sentence? = null): AddSentenceDialogFragment {
            val fragment = AddSentenceDialogFragment()
            sentence?.let {
                val args = Bundle()
                args.putSerializable(ARG_SENTENCE, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
    
    private val audioImportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedAudioUri = uri
                audioSourceType = AudioSourceType.IMPORTED_FILE
                updateAudioStatus("Audio file imported")
                selectAudioSource(false) // Update UI
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogTheme)
        
        arguments?.let {
            editingSentence = it.getSerializable(ARG_SENTENCE) as? Sentence
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddSentenceBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        audioManager = AudioManager(requireContext())
        japaneseRomanizer = SimpleJapaneseRomanizer()
        
        setupUI()
        setupClickListeners()
        populateFields()
    }
    
    private fun setupUI() {
        // Set up category chips
        binding.categoryChips.setOnCheckedStateChangeListener { group, checkedIds ->
            // Handle category selection
        }
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveSentence()
        }
        
        binding.btnTtsAudio.setOnClickListener {
            selectAudioSource(true)
        }
        
        binding.btnImportAudio.setOnClickListener {
            importAudioFile()
        }
        
        binding.btnPreviewAudio.setOnClickListener {
            previewAudio()
        }
        
        binding.btnAutoRomaji.setOnClickListener {
            autoGenerateRomaji()
        }
    }
    
    private fun selectAudioSource(isTTS: Boolean) {
        audioSourceType = if (isTTS) AudioSourceType.TTS else AudioSourceType.IMPORTED_FILE
        
        if (isTTS) {
            binding.btnTtsAudio.backgroundTintList = 
                context?.let { androidx.core.content.ContextCompat.getColorStateList(it, R.color.blue_primary) }
            binding.btnImportAudio.backgroundTintList = 
                context?.let { androidx.core.content.ContextCompat.getColorStateList(it, R.color.background_secondary) }
            updateAudioStatus("TTS audio selected")
        } else {
            binding.btnTtsAudio.backgroundTintList = 
                context?.let { androidx.core.content.ContextCompat.getColorStateList(it, R.color.background_secondary) }
            binding.btnImportAudio.backgroundTintList = 
                context?.let { androidx.core.content.ContextCompat.getColorStateList(it, R.color.blue_primary) }
            
            if (selectedAudioUri == null) {
                updateAudioStatus("No file selected")
            }
        }
    }
    
    private fun importAudioFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        audioImportLauncher.launch(Intent.createChooser(intent, "Select Audio File"))
    }
    
    private fun updateAudioStatus(status: String) {
        binding.audioStatus.text = status
        binding.btnPreviewAudio.visibility = if (status != "No audio selected" && status != "No file selected") 
            View.VISIBLE else View.GONE
    }
    
    private fun previewAudio() {
        val japanese = binding.editJapanese.text.toString()
        
        when (audioSourceType) {
            AudioSourceType.TTS -> {
                if (japanese.isNotEmpty()) {
                    audioManager.speakJapanese(japanese)
                } else {
                    Toast.makeText(context, "Enter Japanese text first", Toast.LENGTH_SHORT).show()
                }
            }
            AudioSourceType.IMPORTED_FILE -> {
                selectedAudioUri?.let { uri ->
                    try {
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val tempFile = File.createTempFile("preview", ".audio", requireContext().cacheDir)
                        inputStream?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        audioManager.playAudio(tempFile.absolutePath)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    private fun populateFields() {
        editingSentence?.let { sentence ->
            binding.editJapanese.setText(sentence.japanese)
            binding.editKana.setText(sentence.kana)
            binding.editRomaji.setText(sentence.romaji)
            binding.editEnglish.setText(sentence.english)
            binding.editCustomCategory.setText(sentence.category)
        }
    }
    
    private fun saveSentence() {
        val japanese = binding.editJapanese.text.toString().trim()
        val kana = binding.editKana.text.toString().trim()
        val romaji = binding.editRomaji.text.toString().trim()
        val english = binding.editEnglish.text.toString().trim()
        
        if (japanese.isEmpty() || english.isEmpty()) {
            Toast.makeText(context, "Please fill in Japanese and English fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Auto-generate kana and romaji if empty
        val finalKana = if (kana.isEmpty()) japanese else kana // Default to japanese if kana not provided
        val finalRomaji = if (romaji.isEmpty()) {
            japaneseRomanizer.romanize(finalKana)
        } else romaji

        var audioPath: String? = null
        
        // Handle audio based on selected source
        when (audioSourceType) {
            AudioSourceType.TTS -> {
                // Generate TTS audio file
                val ttsFile = audioManager.generateTTSAudio(japanese)
                audioPath = ttsFile?.absolutePath
                if (audioPath == null) {
                    Toast.makeText(context, "Failed to generate TTS audio", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            AudioSourceType.IMPORTED_FILE -> {
                selectedAudioUri?.let { uri ->
                    try {
                        // Copy imported file to app's audio directory
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val audioFile = audioManager.createAudioFile("imported_${System.currentTimeMillis()}")
                        inputStream?.use { input ->
                            FileOutputStream(audioFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        audioPath = audioFile.absolutePath
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to save audio file: ${e.message}", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
        }
        
        val sentence = editingSentence?.copy(
            japanese = japanese,
            kana = finalKana,
            romaji = finalRomaji,
            english = english,
            category = selectedCategory,
            audioPath = audioPath
        ) ?: Sentence(
            japanese = japanese,
            kana = finalKana,
            romaji = finalRomaji,
            english = english,
            category = selectedCategory,
            audioPath = audioPath
        )
        
        if (editingSentence != null) {
            viewModel.updateSentence(sentence)
            Toast.makeText(context, "Sentence updated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertSentence(sentence)
            Toast.makeText(context, "Sentence added", Toast.LENGTH_SHORT).show()
        }
        
        dismiss()
    }
    
    private fun getSelectedCategory(): String? {
        return when (binding.categoryChips.checkedChipId) {
            R.id.chip_greetings -> "Greetings"
            R.id.chip_basic -> "Basic"
            R.id.chip_conversation -> "Conversation"
            else -> binding.editCustomCategory.text.toString().takeIf { it.isNotBlank() }
        }
    }
    
    private fun selectAudioSource(isNative: Boolean) {
        if (isNative) {
            binding.btnNativeAudio.backgroundTintList = 
                context?.let { androidx.core.content.ContextCompat.getColorStateList(it, R.color.blue_primary) }
            binding.btnTtsAudio.backgroundTintList = 
                context?.let { androidx.core.content.ContextCompat.getColorStateList(it, R.color.background_secondary) }
        } else {
            binding.btnNativeAudio.backgroundTintList = 
                context?.let { androidx.core.content.ContextCompat.getColorStateList(it, R.color.background_secondary) }
            binding.btnTtsAudio.backgroundTintList = 
                context?.let { androidx.core.content.ContextCompat.getColorStateList(it, R.color.blue_primary) }
        }
    }
    
    private fun playAudio() {
        Toast.makeText(context, "Playing audio", Toast.LENGTH_SHORT).show()
    }
    
    private fun recordAudio() {
        Toast.makeText(context, "Recording audio", Toast.LENGTH_SHORT).show()
    }
    
    private fun generateTTSAudio() {
        val japanese = binding.editJapanese.text.toString()
        if (japanese.isNotEmpty()) {
            Toast.makeText(context, "Generating TTS audio for: $japanese", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun autoGenerateRomaji() {
        val kana = binding.editKana.text.toString().trim()
        val japanese = binding.editJapanese.text.toString().trim()
        
        val textToRomanize = if (kana.isNotEmpty()) kana else japanese
        
        if (textToRomanize.isNotEmpty()) {
            try {
                val romaji = japaneseRomanizer.romanize(textToRomanize)
            binding.editRomaji.setText(romaji)
                Toast.makeText(context, "Romaji generated from ${if (kana.isNotEmpty()) "kana" else "japanese"} text", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to generate romaji: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Enter Japanese or Kana text first", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        audioManager.release()
        _binding = null
    }
}