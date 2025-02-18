package com.example.ssaver.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.fragment.app.Fragment
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
        // Interface section
        binding.headerInterface.setOnClickListener {
            toggleSection(
                binding.contentInterface,
                binding.arrowInterface
            )
        }

        // Images section
        binding.headerImages.setOnClickListener {
            toggleSection(
                binding.contentImages,
                binding.arrowImages
            )
        }

        // Videos section
        binding.headerVideos.setOnClickListener {
            toggleSection(
                binding.contentVideos,
                binding.arrowVideos
            )
        }

        // Managing Files section
        binding.headerManage.setOnClickListener {
            toggleSection(
                binding.contentManage,
                binding.arrowManage
            )
        }

        // Troubleshooting section
        binding.headerTroubleshoot.setOnClickListener {
            toggleSection(
                binding.contentTroubleshoot,
                binding.arrowTroubleshoot
            )
        }

        // Business Support section
        binding.headerBusiness.setOnClickListener {
            toggleSection(
                binding.contentBusiness,
                binding.arrowBusiness
            )
        }
    }

    private fun toggleSection(content: View, arrow: View) {
        // Toggle content visibility with animation
        if (content.visibility == View.VISIBLE) {
            content.visibility = View.GONE
            rotateArrow(arrow, 0f)
        } else {
            content.visibility = View.VISIBLE
            rotateArrow(arrow, 180f)
        }
    }

    private fun rotateArrow(arrow: View, degree: Float) {
        val currentRotation = if (degree == 0f) 180f else 0f
        val anim = RotateAnimation(
            currentRotation, degree,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
            fillAfter = true
        }
        arrow.startAnimation(anim)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 