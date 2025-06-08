package com.example.japaneselearning.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.japaneselearning.R
import com.example.japaneselearning.data.database.AppDatabase
import com.example.japaneselearning.data.repository.SentenceRepository
import com.example.japaneselearning.databinding.FragmentCompareBinding
import com.example.japaneselearning.ui.adapters.RecordingAdapter
import com.example.japaneselearning.ui.viewmodels.PracticeViewModel
import com.example.japaneselearning.utils.AudioManager

class CompareFragment : Fragment() {
    private var _binding: FragmentCompareBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PracticeViewModel by viewModels()
    private lateinit var recordingAdapter: RecordingAdapter
    private lateinit var repository: SentenceRepository
    private lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize repository and audio manager
        val database = AppDatabase.getDatabase(requireContext())
        repository = SentenceRepository(database)
        audioManager = AudioManager(requireContext())

        setupRecyclerView()
        setupObservers()
        setupFilterChips()
    }

    private fun setupRecyclerView() {
        recordingAdapter = RecordingAdapter(repository, audioManager)

        binding.recyclerRecordings.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recordingAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allRecordings.observe(viewLifecycleOwner, Observer { recordings ->
            if (recordings.isEmpty()) {
                showEmptyState(true)
            } else {
                showEmptyState(false)
                recordingAdapter.submitList(recordings)
            }
        })
    }

    private fun showEmptyState(show: Boolean) {
        if (show) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerRecordings.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recyclerRecordings.visibility = View.VISIBLE
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.isChecked = true

        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterRecordings("all")
        }

        binding.chipExcellent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterRecordings("excellent")
        }

        binding.chipGood.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterRecordings("good")
        }

        binding.chipNeedsWork.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterRecordings("needs_work")
        }
    }

    private fun filterRecordings(filter: String) {
        viewModel.allRecordings.value?.let { recordings ->
            val filteredRecordings = when (filter) {
                "excellent" -> recordings.filter { it.similarityScore >= 90f }
                "good" -> recordings.filter { it.similarityScore in 70f..89f }
                "needs_work" -> recordings.filter { it.similarityScore < 70f }
                else -> recordings
            }

            if (filteredRecordings.isEmpty()) {
                showEmptyState(true)
            } else {
                showEmptyState(false)
                recordingAdapter.submitList(filteredRecordings)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        audioManager.releaseMediaPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioManager.release()
        _binding = null
    }
}