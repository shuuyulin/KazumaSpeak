// app/src/main/java/com/example/japaneselearning/ui/fragments/SentencesFragment.kt
package com.example.japaneselearning.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.japaneselearning.databinding.FragmentSentencesBinding
import com.example.japaneselearning.ui.adapters.SentenceAdapter
import com.example.japaneselearning.ui.dialogs.AddSentenceDialogFragment
import com.example.japaneselearning.ui.viewmodels.SentenceViewModel

class SentencesFragment : Fragment() {
    private var _binding: FragmentSentencesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SentenceViewModel by viewModels()
    private lateinit var sentenceAdapter: SentenceAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSentencesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupRecyclerView() {
        sentenceAdapter = SentenceAdapter { sentence ->
            // Handle sentence edit
            val dialog = AddSentenceDialogFragment.newInstance(sentence)
            dialog.show(parentFragmentManager, "edit_sentence")
        }
        
        binding.recyclerSentences.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = sentenceAdapter
        }
    }
    
    private fun setupObservers() {
        viewModel.allSentences.observe(viewLifecycleOwner, Observer { sentences ->
            sentenceAdapter.submitList(sentences)
        })
    }
    
    private fun setupClickListeners() {
        binding.btnAddSentence.setOnClickListener {
            val dialog = AddSentenceDialogFragment()
            dialog.show(parentFragmentManager, "add_sentence")
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}