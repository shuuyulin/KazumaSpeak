package com.example.japaneselearning.ui.fragments

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
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
import androidx.lifecycle.lifecycleScope
import com.example.japaneselearning.R
import com.example.japaneselearning.data.entities.Sentence
import com.example.japaneselearning.databinding.FragmentPracticeBinding
import com.example.japaneselearning.ui.viewmodels.SentenceViewModel
import com.example.japaneselearning.utils.AudioManager
import kotlinx.coroutines.launch
import java.io.File

class PracticeFragment : Fragment() {
    private var _binding: FragmentPracticeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SentenceViewModel by viewModels()
    private lateinit var audioManager: AudioManager
    
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
        
        setupObservers()
        setupClickListeners()
        setupDisplayOptions()
        setupModeSelection()
    }
    
    private fun setupObservers() {
        viewModel.allSentences.observe(viewLifecycleOwner, Observer { sentenceList ->
            sentences = sentenceList
            if (sentences.isNotEmpty()) {
                currentIndex = 0
                displayCurrentSentence()
            }
        })
    }
    
    private fun setupClickListeners() {
        // Card flip on click
        binding.flashcard.setOnClickListener {
            flipCard()
        }
        
        // Audio play button
        binding.btnPlayAudio.setOnClickListener {
            playCurrentAudio()
        }
        
        // Record button
        binding.btnRecord.setOnClickListener {
            toggleRecording()
        }
        
        // Check answer button (reverse translation)
        binding.btnCheckAnswer.setOnClickListener {
            checkAnswer()
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
        binding.chipFlashcardMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isReverseTranslation = false
                updateModeDisplay()
            }
        }
        
        binding.chipReverseTranslation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isReverseTranslation = true
                updateModeDisplay()
            }
        }
    }
    
    private fun displayCurrentSentence() {
        if (sentences.isEmpty()) return
        
        val sentence = sentences[currentIndex]
        
        // Reset card to front
        isCardFlipped = false
        binding.cardFront.visibility = View.VISIBLE
        binding.cardBack.visibility = View.GONE
        
        // Set content
        binding.japaneseText.text = sentence.japanese
        binding.kanaText.text = sentence.kana
        binding.romajiText.text = sentence.romaji
        binding.englishText.text = sentence.english
        
        // Clear previous input
        binding.inputTranslation.text?.clear()
        binding.userAnswerDisplay.text = ""
        
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
            // Hide Japanese texts, show English, show input
            binding.japaneseText.visibility = View.GONE
            binding.kanaText.visibility = View.GONE
            binding.romajiText.visibility = View.GONE
            binding.btnPlayAudio.visibility = View.GONE

            // Show English on front
            binding.englishText.visibility = View.VISIBLE
            binding.inputLayout.visibility = View.VISIBLE
            binding.btnCheckAnswer.visibility = View.VISIBLE

            // Move English to front side for reverse translation
            val parent = binding.englishText.parent as? ViewGroup
            parent?.removeView(binding.englishText) // Remove from current parent if it exists
            binding.cardFront.addView(binding.englishText, binding.cardFront.childCount - 1)
        } else {
            // Normal flashcard mode
            binding.inputLayout.visibility = View.GONE
            binding.btnCheckAnswer.visibility = View.GONE

            // Move English back to back side
            val parent = binding.englishText.parent as? ViewGroup
            parent?.removeView(binding.englishText) // Remove from current parent if it exists
            binding.cardBack.addView(binding.englishText, 0)

            updateCardDisplay()
        }
    }
    
    private fun flipCard() {
        if (isReverseTranslation) return // Don't flip in reverse translation mode manually
        
        // Create flip animation
        val flipOut = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_out)
        val flipIn = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_in)
        
        flipOut.setTarget(if (isCardFlipped) binding.cardBack else binding.cardFront)
        flipIn.setTarget(if (isCardFlipped) binding.cardFront else binding.cardBack)
        
        flipOut.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Switch visibility
                if (isCardFlipped) {
                    binding.cardBack.visibility = View.GONE
                    binding.cardFront.visibility = View.VISIBLE
                } else {
                    binding.cardFront.visibility = View.GONE
                    binding.cardBack.visibility = View.VISIBLE
                }
                flipIn.start()
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
        
        flipOut.start()
        isCardFlipped = !isCardFlipped
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
        
        if (recordedFile != null) {
            Toast.makeText(context, "Recording saved", Toast.LENGTH_SHORT).show()
            // TODO: Add similarity comparison logic here
        } else {
            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkAnswer() {
        val userAnswer = binding.inputTranslation.text.toString().trim()
        val correctAnswer = sentences[currentIndex].japanese
        
        // Show user answer and correct answer on back of card
        binding.userAnswerText.visibility = View.VISIBLE
        binding.userAnswerDisplay.visibility = View.VISIBLE
        binding.userAnswerDisplay.text = userAnswer
        
        // Move English back to back side and show Japanese texts
        binding.cardFront.removeView(binding.englishText)
        binding.cardBack.addView(binding.englishText, 0)
        
        // Add Japanese texts to back side
        val sentence = sentences[currentIndex]
        binding.japaneseText.text = sentence.japanese
        binding.kanaText.text = sentence.kana
        binding.romajiText.text = sentence.romaji
        
        // Create text views for back side if they don't exist
        val backJapanese = android.widget.TextView(requireContext()).apply {
            text = sentence.japanese
            textSize = 20f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            gravity = android.view.Gravity.CENTER
        }
        
        val backKana = android.widget.TextView(requireContext()).apply {
            text = sentence.kana
            textSize = 16f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            gravity = android.view.Gravity.CENTER
        }
        
        binding.cardBack.addView(backJapanese, 1)
        binding.cardBack.addView(backKana, 2)
        
        // Flip to show answer
        flipCardToBack()
    }
    
    private fun flipCardToBack() {
        if (isCardFlipped) return
        
        val flipOut = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_out)
        val flipIn = AnimatorInflater.loadAnimator(requireContext(), R.animator.flip_in)
        
        flipOut.setTarget(binding.cardFront)
        flipIn.setTarget(binding.cardBack)
        
        flipOut.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                binding.cardFront.visibility = View.GONE
                binding.cardBack.visibility = View.VISIBLE
                flipIn.start()
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
        
        flipOut.start()
        isCardFlipped = true
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        audioManager.release()
        _binding = null
    }
}