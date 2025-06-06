// app/src/main/java/com/example/japaneselearning/ui/fragments/PracticeFragment.kt
package com.example.japaneselearning.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.japaneselearning.databinding.FragmentPracticeBinding
import com.example.japaneselearning.ui.viewmodels.PracticeViewModel
import com.example.japaneselearning.ui.viewmodels.SentenceViewModel
import com.example.japaneselearning.utils.AudioManager

class PracticeFragment : Fragment() {
    private var _binding: FragmentPracticeBinding? = null
    private val binding get() = _binding!!
    
    private val practiceViewModel: PracticeViewModel by viewModels()
    private val sentenceViewModel: SentenceViewModel by viewModels()
    private lateinit var audioManager: AudioManager
    
    private val RECORD_AUDIO_PERMISSION_CODE = 1001
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        audioManager = AudioManager(requireContext())
        setupObservers()
        setupClickListeners()
        loadSentences()
    }
    
    private fun setupObservers() {
        sentenceViewModel.allSentences.observe(viewLifecycleOwner, Observer { sentences ->
            if (sentences.isNotEmpty()) {
                practiceViewModel.setSentences(sentences)
                updateProgress(1, sentences.size)
            }
        })
        
        practiceViewModel.currentSentence.observe(viewLifecycleOwner, Observer { sentence ->
            sentence?.let {
                updateCardContent(it)
            }
        })
        
        practiceViewModel.practiceSettings.observe(viewLifecycleOwner, Observer { settings ->
            updateDisplaySettings(settings)
        })
        
        practiceViewModel.isCardFlipped.observe(viewLifecycleOwner, Observer { isFlipped ->
            updateCardDisplay(isFlipped)
        })
    }
    
    private fun setupClickListeners() {
        binding.btnFlipCard.setOnClickListener {
            practiceViewModel.flipCard()
        }
        
        binding.btnFlipBack.setOnClickListener {
            practiceViewModel.flipCard()
        }
        
        binding.btnNext.setOnClickListener {
            practiceViewModel.nextSentence()
        }
        
        binding.btnSkip.setOnClickListener {
            practiceViewModel.nextSentence()
        }
        
        binding.btnRecord.setOnClickListener {
            if (checkAudioPermission()) {
                startRecording()
            } else {
                requestAudioPermission()
            }
        }
        
        binding.switchReverseMode.setOnCheckedChangeListener { _, isChecked ->
            updateReverseMode(isChecked)
        }
        
        binding.chipJapanese.setOnCheckedChangeListener { _, isChecked ->
            updateDisplayOption("japanese", isChecked)
        }
        
        binding.chipRomaji.setOnCheckedChangeListener { _, isChecked ->
            updateDisplayOption("romaji", isChecked)
        }
        
        binding.chipAudio.setOnCheckedChangeListener { _, isChecked ->
            updateDisplayOption("audio", isChecked)
        }
        
        binding.chipAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            updateDisplayOption("autoPlay", isChecked)
        }
        
        binding.btnCheckAnswer.setOnClickListener {
            checkTranslationAnswer()
        }
    }
    
    private fun updateCardContent(sentence: com.example.japaneselearning.data.entities.Sentence) {
        binding.japaneseText.text = sentence.japanese
        binding.romajiText.text = sentence.romaji
        binding.englishText.text = sentence.english
        binding.englishPrompt.text = sentence.english
    }
    
    private fun updateDisplaySettings(settings: com.example.japaneselearning.ui.viewmodels.PracticeSettings) {
        // Update UI based on settings
        binding.japaneseText.visibility = if (settings.showJapanese) View.VISIBLE else View.GONE
        binding.romajiText.visibility = if (settings.showRomaji) View.VISIBLE else View.GONE
        
        // Show/hide reverse mode content
        if (settings.reverseMode) {
            binding.cardFront.visibility = View.GONE
            binding.cardBack.visibility = View.GONE
            binding.reverseModeContent.visibility = View.VISIBLE
            binding.displayOptions.alpha = 0.5f
            binding.progressText.text = "${binding.progressText.text} (Reverse Mode)"
        } else {
            binding.reverseModeContent.visibility = View.GONE
            binding.displayOptions.alpha = 1.0f
        }
    }
    
    private fun updateCardDisplay(isFlipped: Boolean) {
        if (isFlipped) {
            binding.cardFront.visibility = View.GONE
            binding.cardBack.visibility = View.VISIBLE
        } else {
            binding.cardFront.visibility = View.VISIBLE
            binding.cardBack.visibility = View.GONE
        }
    }
    
    private fun updateReverseMode(isEnabled: Boolean) {
        val currentSettings = practiceViewModel.practiceSettings.value ?: 
            com.example.japaneselearning.ui.viewmodels.PracticeSettings()
        practiceViewModel.updateSettings(currentSettings.copy(reverseMode = isEnabled))
    }
    
    private fun updateDisplayOption(option: String, isEnabled: Boolean) {
        val currentSettings = practiceViewModel.practiceSettings.value ?: 
            com.example.japaneselearning.ui.viewmodels.PracticeSettings()
        
        val newSettings = when (option) {
            "japanese" -> currentSettings.copy(showJapanese = isEnabled)
            "romaji" -> currentSettings.copy(showRomaji = isEnabled)
            "audio" -> currentSettings.copy(showAudio = isEnabled)
            "autoPlay" -> currentSettings.copy(autoPlay = isEnabled)
            else -> currentSettings
        }
        
        practiceViewModel.updateSettings(newSettings)
    }
    
    private fun updateProgress(current: Int, total: Int) {
        binding.progressBar.max = total
        binding.progressBar.progress = current
        binding.progressText.text = "$current of $total"
    }
    
    private fun checkTranslationAnswer() {
        val userInput = binding.translationInput.text.toString()
        val currentSentence = practiceViewModel.currentSentence.value
        
        // Simple check - in a real app, you'd want more sophisticated comparison
        currentSentence?.let { sentence ->
            val isCorrect = userInput.contains(sentence.japanese, ignoreCase = true)
            if (isCorrect) {
                Toast.makeText(context, "Correct!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Try again. Correct answer: ${sentence.japanese}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun loadSentences() {
        // Sentences will be loaded automatically through observer
    }
    
    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }
    
    private fun startRecording() {
        // Implement audio recording logic
        Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(context, "Audio permission required for recording", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}