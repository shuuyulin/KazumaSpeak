package com.example.japaneselearning.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.japaneselearning.R
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.databinding.FragmentPracticeBinding
import com.example.japaneselearning.ui.viewmodels.PracticeViewModel
import com.example.japaneselearning.ui.viewmodels.SentenceViewModel
import com.example.japaneselearning.utils.AudioManager
import com.example.japaneselearning.utils.PronunciationAnalyzer
import kotlinx.coroutines.launch
import java.io.File

class PracticeFragment : Fragment() {
    private var _binding: FragmentPracticeBinding? = null
    private val binding get() = _binding!!
    
    private val sentenceViewModel: SentenceViewModel by viewModels()
    private val practiceViewModel: PracticeViewModel by viewModels()
    private lateinit var audioManager: AudioManager
    private lateinit var pronunciationAnalyzer: PronunciationAnalyzer
    
    private var sentences = listOf<Sentence>()
    private var currentIndex = 0
    private var isCardFlipped = false
    private var isRecording = false
    private var currentRecordingFile: File? = null
    
    // Display options
    private var showJapanese = true
    private var showKana = false
    private var showRomaji = false
    private var showAudio = true
    private var autoPlay = false
    
    // Mode
    private var isReverseTranslation = false
    
    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 101
    }
    
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
        pronunciationAnalyzer = PronunciationAnalyzer(requireContext())
        
        // Initialize the pronunciation analyzer
        lifecycleScope.launch {
            pronunciationAnalyzer.initialize()
        }
        
        setupObservers()
        setupClickListeners()
        setupDisplayOptions()
        setupModeSelection()
    }
    
    private fun setupObservers() {
        sentenceViewModel.allSentences.observe(viewLifecycleOwner, Observer { sentenceList ->
            sentences = sentenceList
            if (sentences.isNotEmpty()) {
                currentIndex = 0
                displayCurrentSentence()
            }
        })
    }
    
    private fun setupClickListeners() {
        // Card flip on click (but not when clicking input field)
        binding.cardContainer.setOnClickListener {
            if (isReverseTranslation) {
                // In reverse mode, flip only if we haven't checked answer yet
                if (!isCardFlipped) {
                    checkAnswerAndFlip()
                } else {
                    // If already flipped, go to next
                    nextSentence()
                }
            } else {
                // Normal mode - just flip
                flipCard()
            }
        }
        
        // Prevent input field clicks from triggering card flip
        binding.inputTranslation.setOnClickListener { 
            // Do nothing - let the input field handle the click
        }
        
        // Audio play button
        binding.btnPlayAudio.setOnClickListener {
            playCurrentAudio()
        }
        
        // Record button
        binding.btnRecord.setOnClickListener {
            toggleRecording()
        }
        
        // Next button
        binding.btnNext.setOnClickListener {
            nextSentence()
        }
    }
    
    private fun setupDisplayOptions() {
        binding.chipJapanese.setOnCheckedChangeListener { _, isChecked ->
            showJapanese = isChecked
            updateCardDisplay()
        }
        
        binding.chipKana.setOnCheckedChangeListener { _, isChecked ->
            showKana = isChecked
            updateCardDisplay()
        }
        
        binding.chipRomaji.setOnCheckedChangeListener { _, isChecked ->
            showRomaji = isChecked
            updateCardDisplay()
        }
        
        binding.chipAudio.setOnCheckedChangeListener { _, isChecked ->
            showAudio = isChecked
            updateCardDisplay()
        }
        
        binding.chipAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            autoPlay = isChecked
            if (autoPlay && !isCardFlipped) {
                playCurrentAudio()
            }
        }
    }
    
    private fun setupModeSelection() {
        binding.btnFlashcardMode.setOnClickListener {
            setFlashcardMode()
        }
        
        binding.btnReverseMode.setOnClickListener {
            setReverseTranslationMode()
        }
    }

    private fun setFlashcardMode() {
        isReverseTranslation = false
        
        // Update button appearances
        binding.btnFlashcardMode.backgroundTintList = 
            ContextCompat.getColorStateList(requireContext(), R.color.blue_primary)
        binding.btnFlashcardMode.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        
        binding.btnReverseMode.backgroundTintList = 
            ContextCompat.getColorStateList(requireContext(), R.color.background_light)
        binding.btnReverseMode.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.text_secondary)
        )
        
        updateModeDisplay()
    }

    private fun setReverseTranslationMode() {
        isReverseTranslation = true
        
        // Update button appearances
        binding.btnReverseMode.backgroundTintList = 
            ContextCompat.getColorStateList(requireContext(), R.color.blue_primary)
        binding.btnReverseMode.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        
        binding.btnFlashcardMode.backgroundTintList = 
            ContextCompat.getColorStateList(requireContext(), R.color.background_light)
        binding.btnFlashcardMode.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.text_secondary)
        )
        
        updateModeDisplay()
    }

    private fun displayCurrentSentence() {
        if (sentences.isEmpty()) return
        
        val sentence = sentences[currentIndex]
        
        // Reset card to front
        isCardFlipped = false
        binding.cardFront.visibility = View.VISIBLE
        binding.cardBack.visibility = View.GONE
        binding.cardFront.alpha = 1f
        binding.cardFront.rotationY = 0f
        binding.cardBack.alpha = 0f
        binding.cardBack.rotationY = -90f
        
        // Set content
        binding.japaneseText.text = sentence.japanese
        binding.kanaText.text = sentence.kana
        binding.romajiText.text = sentence.romaji
        binding.englishTextFront.text = sentence.english
        binding.englishTextBack.text = sentence.english
        
        // Clear previous input and back side content
        binding.inputTranslation.text?.clear()
        binding.userAnswerDisplay.text = ""
        binding.correctAnswerLabel.visibility = View.GONE
        binding.japaneseTextBack.visibility = View.GONE
        binding.kanaTextBack.visibility = View.GONE
        binding.userAnswerLabel.visibility = View.GONE
        binding.userAnswerDisplay.visibility = View.GONE
        
        updateCardDisplay()
        updateModeDisplay()
        
        // Auto play if enabled
        if (autoPlay && showAudio && !isReverseTranslation) {
            playCurrentAudio()
        }
    }
    
    private fun updateCardDisplay() {
        if (sentences.isEmpty()) return
        
        // Show/hide text based on display options
        binding.japaneseText.visibility = if (showJapanese && !isReverseTranslation) View.VISIBLE else View.GONE
        binding.kanaText.visibility = if (showKana && !isReverseTranslation) View.VISIBLE else View.GONE
        binding.romajiText.visibility = if (showRomaji && !isReverseTranslation) View.VISIBLE else View.GONE
        
        // Show/hide audio button
        binding.btnPlayAudio.visibility = if (showAudio && !isReverseTranslation) View.VISIBLE else View.GONE
    }
    
    private fun updateModeDisplay() {
        if (isReverseTranslation) {
            // Reverse translation mode
            binding.japaneseText.visibility = View.GONE
            binding.kanaText.visibility = View.GONE
            binding.romajiText.visibility = View.GONE
            binding.btnPlayAudio.visibility = View.GONE
            binding.englishTextFront.visibility = View.VISIBLE
            binding.inputLayout.visibility = View.VISIBLE
            
            // Hide English on back side for reverse mode
            binding.englishTextBack.visibility = View.GONE
        } else {
            // Normal flashcard mode
            binding.englishTextFront.visibility = View.GONE
            binding.inputLayout.visibility = View.GONE
            binding.englishTextBack.visibility = View.VISIBLE
            
            updateCardDisplay()
        }
    }
    
    private fun flipCard() {
        val frontView = binding.cardFront
        val backView = binding.cardBack
        
        if (isCardFlipped) {
            // Flip from back to front
            flipCardAnimation(backView, frontView) {
                isCardFlipped = false
            }
        } else {
            // Flip from front to back
            flipCardAnimation(frontView, backView) {
                isCardFlipped = true
            }
        }
    }
    
    private fun flipCardAnimation(fromView: View, toView: View, onComplete: () -> Unit) {
        // Set initial states
        fromView.alpha = 1f
        fromView.rotationY = 0f
        toView.alpha = 0f
        toView.rotationY = -90f
        toView.visibility = View.VISIBLE
        
        // Animate out the current view
        fromView.animate()
            .rotationY(90f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                // Hide the old view
                fromView.visibility = View.GONE
                
                // Animate in the new view
                toView.animate()
                    .rotationY(0f)
                    .alpha(1f)
                    .setDuration(200)
                    .withEndAction {
                        onComplete()
                    }
                    .start()
            }
            .start()
    }
    
    private fun checkAnswerAndFlip() {
        val userAnswer = binding.inputTranslation.text.toString().trim()
        val sentence = sentences[currentIndex]
        
        // Prepare back side content
        binding.correctAnswerLabel.visibility = View.VISIBLE
        binding.japaneseTextBack.visibility = View.VISIBLE
        binding.japaneseTextBack.text = sentence.japanese
        
        if (sentence.kana.isNotEmpty()) {
            binding.kanaTextBack.visibility = View.VISIBLE
            binding.kanaTextBack.text = sentence.kana
        } else {
            binding.kanaTextBack.visibility = View.GONE
        }
        
        binding.userAnswerLabel.visibility = View.VISIBLE
        binding.userAnswerDisplay.visibility = View.VISIBLE
        binding.userAnswerDisplay.text = if (userAnswer.isNotEmpty()) userAnswer else "(No answer provided)"
        
        // Hide English on back for reverse mode
        binding.englishTextBack.visibility = View.GONE
        
        // Flip to back
        flipCardAnimation(binding.cardFront, binding.cardBack) {
            isCardFlipped = true
        }
    }
    
    private fun playCurrentAudio() {
        if (sentences.isEmpty()) return
        
        val sentence = sentences[currentIndex]
        
        // Try audio file first, then TTS
        var audioPlayed = false
        if (!sentence.audioPath.isNullOrEmpty()) {
            audioPlayed = audioManager.playAudio(sentence.audioPath)
        }
        
        if (!audioPlayed) {
            // Use kana if available, otherwise japanese
            val textToSpeak = if (sentence.kana.isNotEmpty()) sentence.kana else sentence.japanese
            audioManager.speakJapanese(textToSpeak)
        }
    }
    
    private fun toggleRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
            return
        }
        
        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }
    
    private fun startRecording() {
        currentRecordingFile = audioManager.startRecording()
        if (currentRecordingFile != null) {
            isRecording = true
            binding.btnRecord.backgroundTintList = 
                ContextCompat.getColorStateList(requireContext(), R.color.orange_primary)
            Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopRecording() {
        val recordedFile = audioManager.stopRecording()
        isRecording = false
        binding.btnRecord.backgroundTintList = 
            ContextCompat.getColorStateList(requireContext(), R.color.red_primary)
        
        if (recordedFile != null && recordedFile.exists()) {
            Log.d("PracticeFragment", "Recording saved: ${recordedFile.absolutePath}")
            Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
            
            // Get the current sentence
            val currentSentence = sentences[currentIndex]
            
            // Launch coroutine to analyze pronunciation
            lifecycleScope.launch {
                // Show loading indicator
                binding.btnRecord.isEnabled = false
                
                // Calculate similarity score using ISpikit
                val similarityScore = calculateSimilarityScore(currentSentence, recordedFile)
                
                // Save to database using the current sentence
                practiceViewModel.saveRecording(
                    sentenceId = currentSentence.id,
                    audioPath = recordedFile.absolutePath,
                    similarityScore = similarityScore
                )
                
                // Re-enable button
                binding.btnRecord.isEnabled = true
                
                // Show feedback
                val feedback = pronunciationAnalyzer.getDetailedFeedback(similarityScore)
                Toast.makeText(context, feedback, Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("PracticeFragment", "Recording failed or file doesn't exist")
            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun calculateSimilarityScore(sentence: Sentence, recordedFile: File): Float {
        // Check if we have Japanese text to use for TTS-based analysis
        if (!sentence.japanese.isNullOrEmpty()) {
            Log.d("PracticeFragment", "Using TTS-based analysis with text: ${sentence.japanese}")
            // Use the TTS-optimized method if we have Japanese text
            return pronunciationAnalyzer.analyzePronunciationWithTTS(
                sentence.japanese,
                recordedFile.absolutePath
            )
        } else if (!sentence.audioPath.isNullOrEmpty()) {
            // Fall back to file-to-file comparison if we have reference audio
            val referenceFile = File(sentence.audioPath)
            if (referenceFile.exists()) {
                Log.d("PracticeFragment", "Using file-to-file analysis")
                return pronunciationAnalyzer.analyzePronunciation(
                    sentence.audioPath,
                    recordedFile.absolutePath
                )
            }
        }
        
        // If no reference is available, use mock analysis
        Log.d("PracticeFragment", "No reference available, using mock analysis")
        return 0.0f // Will cause mockAnalysis to be used
    }
    
    private fun nextSentence() {
        if (sentences.isEmpty()) return
        
        currentIndex = (currentIndex + 1) % sentences.size
        displayCurrentSentence()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Microphone permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Microphone permission required for recording", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        audioManager.releaseMediaPlayer() // Now it's public and can be called
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
        pronunciationAnalyzer.release()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}