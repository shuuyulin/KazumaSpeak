package com.example.japaneselearning.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.japaneselearning.R
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.databinding.DialogAddSentenceBinding
import com.example.japaneselearning.ui.viewmodels.SentenceViewModel
import com.example.japaneselearning.utils.AudioManager
import com.example.japaneselearning.utils.JapaneseTextConverter
import com.example.japaneselearning.utils.NetworkUtils
import com.example.japaneselearning.utils.ConversionResult
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AddSentenceDialogFragment : DialogFragment() {
    private var _binding: DialogAddSentenceBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SentenceViewModel by viewModels()
    private var editingSentence: Sentence? = null
    private lateinit var audioManager: AudioManager
    private lateinit var japaneseTextConverter: JapaneseTextConverter
    
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
        japaneseTextConverter = JapaneseTextConverter(requireContext())
        
        setupUI()
        setupClickListeners()
        populateFields()
        loadExistingCategories()
    }
    
    private fun setupUI() {
        setupNewCategoryInput()
    }
    
    private fun setupNewCategoryInput() {
        binding.editNewCategory.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                addNewCategory()
                true
            } else {
                false
            }
        }
    }
    
    private fun addNewCategory() {
        val categoryText = binding.editNewCategory.text.toString().trim()
        if (categoryText.isNotEmpty() && !categories.contains(categoryText)) {
            categories.add(categoryText)
            addCategoryChip(categoryText)
            binding.editNewCategory.text?.clear()
        }
    }
    
    private fun addCategoryChip(categoryText: String) {
        val chip = Chip(requireContext()).apply {
            text = categoryText
            isCheckable = true
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                categories.remove(categoryText)
                binding.dynamicCategoryChips.removeView(this)
                if (selectedCategory == categoryText) {
                    selectedCategory = null
                }
            }
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Uncheck other chips
                    for (i in 0 until binding.dynamicCategoryChips.childCount) {
                        val otherChip = binding.dynamicCategoryChips.getChildAt(i) as? Chip
                        if (otherChip != this) {
                            otherChip?.isChecked = false
                        }
                    }
                    selectedCategory = categoryText
                } else {
                    selectedCategory = null
                }
            }
        }
        binding.dynamicCategoryChips.addView(chip)
    }
    
    private fun loadExistingCategories() {
        // Load common categories
        val defaultCategories = listOf("Greetings", "Basic", "Conversation", "Travel", "Food")
        defaultCategories.forEach { category ->
            if (!categories.contains(category)) {
                categories.add(category)
                addCategoryChip(category)
            }
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
        
        binding.btnGenerateKana.setOnClickListener {
            generateKana()
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
            
            // Set category
            sentence.category?.let { category ->
                if (!categories.contains(category)) {
                    categories.add(category)
                    addCategoryChip(category)
                }
                // Select the category chip
                for (i in 0 until binding.dynamicCategoryChips.childCount) {
                    val chip = binding.dynamicCategoryChips.getChildAt(i) as? Chip
                    if (chip?.text == category) {
                        chip.isChecked = true
                        selectedCategory = category
                        break
                    }
                }
            }
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
        
        // Auto-generate missing fields if needed
        val finalKana = if (kana.isEmpty()) japanese else kana
        val finalRomaji = if (romaji.isEmpty()) {
            finalKana.lowercase() // Simple fallback
        } else romaji
        
        var audioPath: String? = null
        
        // Handle audio based on selected source
        when (audioSourceType) {
            AudioSourceType.TTS -> {
                try {
                    // Generate TTS audio file
                    val ttsFile = audioManager.generateTTSAudio(japanese)
                    audioPath = ttsFile?.absolutePath
                    
                    if (audioPath == null) {
                        Log.w("AddSentenceDialog", "TTS audio generation failed, but continuing to save")
                        // Don't return - save sentence without audio
                    }
                } catch (e: Exception) {
                    Log.e("AddSentenceDialog", "Error generating TTS audio: ${e.message}")
                    // Continue without audio
                }
            }
            AudioSourceType.IMPORTED_FILE -> {
                selectedAudioUri?.let { uri ->
                    try {
                        // Copy imported file to app's audio directory
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        val audioFile = audioManager.createAudioFile("imported_${System.currentTimeMillis()}")
                        inputStream?.use { input ->
                            java.io.FileOutputStream(audioFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        audioPath = audioFile.absolutePath
                    } catch (e: Exception) {
                        Log.e("AddSentenceDialog", "Error saving imported audio: ${e.message}")
                        Toast.makeText(context, "Audio import failed, saving sentence without audio", Toast.LENGTH_SHORT).show()
                        // Continue without audio
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
    
    private fun generateKana() {
        val japanese = binding.editJapanese.text.toString().trim()
        
        if (japanese.isEmpty()) {
            Toast.makeText(context, "Enter Japanese text with kanji first", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        binding.btnGenerateKana.text = "..."
        binding.btnGenerateKana.isEnabled = false
        
        try {
            val result = japaneseTextConverter.convertKanjiToKana(japanese)
            
            when (result) {
                is ConversionResult.Success -> {
                    binding.editKana.setText(result.result)
                    Toast.makeText(context, "Kana generated successfully", Toast.LENGTH_SHORT).show()
                }
                is ConversionResult.PartialSuccess -> {
                    binding.editKana.setText(result.result)
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
                is ConversionResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate kana: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            binding.btnGenerateKana.text = "Generate"
            binding.btnGenerateKana.isEnabled = true
        }
    }
    
    private fun autoGenerateRomaji() {
        val kana = binding.editKana.text.toString().trim()
        
        if (kana.isEmpty()) {
            Toast.makeText(context, "Generate kana text first, or enter kana manually", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(context, "Network connection required for romaji generation", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        binding.btnAutoRomaji.text = "..."
        binding.btnAutoRomaji.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val result = japaneseTextConverter.convertKanaToRomaji(kana)
                
                when (result) {
                    is ConversionResult.Success -> {
                        binding.editRomaji.setText(result.result)
                        Toast.makeText(context, "Romaji generated successfully", Toast.LENGTH_SHORT).show()
                    }
                    is ConversionResult.PartialSuccess -> {
                        binding.editRomaji.setText(result.result)
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                    is ConversionResult.Error -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to generate romaji: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnAutoRomaji.text = "Generate"
                binding.btnAutoRomaji.isEnabled = true
            }
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