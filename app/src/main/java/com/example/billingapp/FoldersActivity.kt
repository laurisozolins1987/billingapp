package com.example.billingapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.billingapp.databinding.ActivityFoldersBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FoldersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoldersBinding
    private val viewModel: TransactionViewModel by viewModels()

    companion object {
        const val EXTRA_FOLDER_ID = "folder_id"
        const val EXTRA_FOLDER_NAME = "folder_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarFolders.setNavigationOnClickListener { finish() }

        val adapter = FolderAdapter(
            onItemClick = { folder ->
                val intent = Intent(this, FolderDetailActivity::class.java)
                intent.putExtra(EXTRA_FOLDER_ID, folder.id)
                intent.putExtra(EXTRA_FOLDER_NAME, folder.name)
                startActivity(intent)
            },
            onDeleteClick = { folder ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_folder)
                    .setMessage(getString(R.string.delete_folder_confirm, folder.name))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deleteFolder(folder)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )

        binding.rvFolders.layoutManager = LinearLayoutManager(this)
        binding.rvFolders.adapter = adapter

        viewModel.allFolders.observe(this) { folders ->
            adapter.submitList(folders)
            binding.tvEmptyFolders.visibility = if (folders.isEmpty()) View.VISIBLE else View.GONE
            binding.rvFolders.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE

            // Observe transaction counts for each folder
            folders.forEach { folder ->
                viewModel.getTransactionCountByFolder(folder.id).observe(this) { count ->
                    val position = adapter.currentList.indexOfFirst { it.id == folder.id }
                    if (position >= 0) {
                        val vh = binding.rvFolders.findViewHolderForAdapterPosition(position)
                        (vh as? FolderAdapter.FolderViewHolder)?.setCount(count)
                    }
                }
            }
        }

        binding.btnAddFolder.setOnClickListener {
            val name = binding.etNewFolder.text.toString().trim()
            if (name.isNotEmpty()) {
                viewModel.insertFolder(name)
                binding.etNewFolder.text?.clear()
            } else {
                Toast.makeText(this, R.string.enter_folder_name, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
