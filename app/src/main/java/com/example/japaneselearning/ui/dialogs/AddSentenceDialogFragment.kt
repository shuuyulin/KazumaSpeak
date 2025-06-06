// app/src/main/java/com/example/japaneselearning/ui/dialogs/AddSentenceDialogFragment.kt
package com.example.japaneselearning.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.japaneselearning.R
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.databinding.DialogAddSentenceBinding
import com.example.japaneselearning.ui.viewmodels.SentenceViewModel

class AddSentenceDialogFragment : DialogFragment() {
    private var _binding: DialogAddSentenceBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SentenceViewModel by viewModels()
    private var editingSentence: Sentence? = null
    
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
        
        binding.btnNativeAudio.setOnClickListener {
            selectAudioSource(true)
        }
        
        binding.btnTtsAudio.setOnClickListener {
            selectAudioSource(false)
        }
        
        binding.btnPlay.setOnClickListener {
            playAudio()
        }
        
        binding.btnRecord.setOnClickListener {
            recordAudio()
        }
        
        binding.btnGenerateTts.setOnClickListener {
            generateTTSAudio()
        }
        
        binding.btnAutoRomaji.setOnClickListener {
            autoGenerateRomaji()
        }
    }
    
    private fun populateFields() {
        editingSentence?.let { sentence ->
            binding.editJapanese.setText(sentence.japanese)
            binding.editRomaji.setText(sentence.romaji)
            binding.editEnglish.setText(sentence.english)
            binding.editCustomCategory.setText(sentence.category)
        }
    }
    
    private fun saveSentence() {
        val japanese = binding.editJapanese.text.toString().trim()
        val romaji = binding.editRomaji.text.toString().trim()
        val english = binding.editEnglish.text.toString().trim()
        
        if (japanese.isEmpty() || english.isEmpty()) {
            Toast.makeText(context, "Please fill in Japanese and English fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        val category = getSelectedCategory()
        
        val sentence = editingSentence?.copy(
            japanese = japanese,
            romaji = romaji,
            english = english,
            category = category
        ) ?: Sentence(
            japanese = japanese,
            romaji = romaji,
            english = english,
            category = category
        )
        
        if (editingSentence != null) {
            viewModel.updateSentence(sentence)
        } else {
            viewModel.insertSentence(sentence)
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
        val japanese = binding.editJapanese.text.toString()
        if (japanese.isNotEmpty()) {
            // Simple romanization - in a real app, you'd use a proper romanization library
            val romaji = romanizeJapanese(japanese)
            binding.editRomaji.setText(romaji)
        }
    }
    
    private fun romanizeJapanese(japanese: String): String {
        // This is a very basic implementation
        // In a real app, you'd use a proper romanization library like Kuroshiro
        val basicMappings = mapOf(
            "こんにちは" to "konnichiwa",
            "ありがとう" to "arigatou",
            "おはよう" to "ohayou",
            "すみません" to "sumimasen",
            "はじめまして" to "hajimemashite"
        )
        
        return basicMappings[japanese] ?: japanese
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
        _binding = null
    }
}