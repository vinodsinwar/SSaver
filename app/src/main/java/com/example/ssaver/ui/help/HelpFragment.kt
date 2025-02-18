package com.example.ssaver.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.example.ssaver.R
import com.example.ssaver.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupExpandableCards()
    }

    private fun setupExpandableCards() {
        // How to save card
        binding.headerHowToSave.setOnClickListener {
            toggleContent(binding.contentHowToSave, binding.arrowHowToSave)
        }

        // Permissions card
        binding.headerPermissions.setOnClickListener {
            toggleContent(binding.contentPermissions, binding.arrowPermissions)
        }

        // Not showing card
        binding.headerNotShowing.setOnClickListener {
            toggleContent(binding.contentNotShowing, binding.arrowNotShowing)
        }
    }

    private fun toggleContent(content: View, arrow: View) {
        if (content.visibility == View.GONE) {
            // Expand
            content.visibility = View.VISIBLE
            arrow.animate().rotation(180f).setDuration(300).start()
        } else {
            // Collapse
            content.visibility = View.GONE
            arrow.animate().rotation(0f).setDuration(300).start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 