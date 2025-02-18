package com.example.ssaver.ui.wapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.PopupMenu
import com.bumptech.glide.Glide
import com.example.ssaver.R
import com.example.ssaver.databinding.FragmentStatusGridBinding
import com.example.ssaver.databinding.ItemStatusBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import android.view.ContextThemeWrapper

class StatusGridFragment : Fragment() {

    private var _binding: FragmentStatusGridBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WappViewModel
    private lateinit var statusType: StatusType
    private lateinit var adapter: StatusAdapter

    companion object {
        private const val ARG_STATUS_TYPE = "status_type"

        fun newInstance(type: StatusType) = StatusGridFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_STATUS_TYPE, type)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_STATUS_TYPE, StatusType::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable(ARG_STATUS_TYPE)
        } as StatusType
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireParentFragment()).get(WappViewModel::class.java)
        
        setupRecyclerView()
        observeStatuses()
    }

    private fun setupRecyclerView() {
        adapter = StatusAdapter(
            onDownloadClick = { status ->
                if (!status.isDownloaded) {
                    val success = viewModel.downloadStatus(status)
                    if (success) {
                        // Show success dialog
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.download_success_title)
                            .setMessage(R.string.status_saved)
                            .setPositiveButton(R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    } else {
                        // Show error Snackbar with retry option
                        Snackbar.make(
                            binding.root,
                            R.string.error_saving,
                            Snackbar.LENGTH_LONG
                        ).apply {
                            setAction(R.string.retry) {
                                viewModel.downloadStatus(status)
                            }
                            show()
                        }
                    }
                }
            },
            onShareClick = { status ->
                shareStatus(status)
            }
        )

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = this@StatusGridFragment.adapter
        }
    }

    private fun observeStatuses() {
        viewModel.statuses.observe(viewLifecycleOwner) { statuses ->
            adapter.submitList(statuses.filter { it.type == statusType })
        }
    }

    private fun shareStatus(status: Status) {
        val file = File(status.uri.path!!)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (status.type) {
                StatusType.IMAGE -> "image/*"
                StatusType.VIDEO -> "video/*"
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        startActivity(Intent.createChooser(intent, null))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class StatusAdapter(
    private val onDownloadClick: (Status) -> Unit,
    private val onShareClick: (Status) -> Unit
) : RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {

    private var items = listOf<Status>()

    fun submitList(newItems: List<Status>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val binding = ItemStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class StatusViewHolder(
        private val binding: ItemStatusBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(status: Status) {
            Glide.with(binding.root)
                .load(status.uri)
                .centerCrop()
                .into(binding.imageView)

            // Show/hide play icon for videos
            binding.playIcon.visibility = if (status.type == StatusType.VIDEO) View.VISIBLE else View.GONE
            
            // Update action button icon based on download status
            binding.actionButtonImage.setImageResource(
                if (status.isDownloaded) R.drawable.ic_check else R.drawable.ic_download
            )

            // Set up click listener for both the action button and the image button
            binding.actionButton.setOnClickListener { view ->
                showPopupMenu(view, status)
            }
            binding.actionButtonImage.setOnClickListener { view ->
                showPopupMenu(view, status)
            }
        }

        private fun showPopupMenu(view: View, status: Status) {
            val wrapper = ContextThemeWrapper(view.context, R.style.CustomPopupMenu)
            val popup = PopupMenu(wrapper, view)
            popup.menuInflater.inflate(R.menu.status_actions_menu, popup.menu)
            
            // Update download menu item based on status
            popup.menu.findItem(R.id.action_download).apply {
                isEnabled = !status.isDownloaded
                title = if (status.isDownloaded) {
                    view.context.getString(R.string.downloaded)
                } else {
                    view.context.getString(R.string.download)
                }
            }

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_download -> {
                        if (!status.isDownloaded) {
                            onDownloadClick(status)
                        }
                        true
                    }
                    R.id.action_share -> {
                        onShareClick(status)
                        true
                    }
                    else -> false
                }
            }
            
            try {
                popup.show()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to direct action if popup fails
                if (!status.isDownloaded) {
                    onDownloadClick(status)
                }
            }
        }
    }
} 