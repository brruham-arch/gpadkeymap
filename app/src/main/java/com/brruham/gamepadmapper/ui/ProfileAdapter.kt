package com.brruham.gamepadmapper.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.brruham.gamepadmapper.databinding.ItemProfileBinding
import com.brruham.gamepadmapper.model.MappingProfile

class ProfileAdapter(
    private val profiles: List<MappingProfile>,
    private val onEdit: (MappingProfile) -> Unit,
    private val onDelete: (MappingProfile) -> Unit,
    private val onActivate: (MappingProfile) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.VH>() {

    private var activeId: String? = null

    fun setActiveId(id: String?) {
        activeId = id
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = profiles.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val profile = profiles[position]
        with(holder.binding) {
            tvProfileName.text = profile.name
            tvMappingCount.text = "${profile.mappings.size} mappings"

            val isActive = profile.id == activeId
            root.setBackgroundColor(
                if (isActive) Color.parseColor("#E3F2FD") else Color.WHITE
            )
            tvActiveTag.visibility = if (isActive) android.view.View.VISIBLE else android.view.View.GONE

            btnEdit.setOnClickListener { onEdit(profile) }
            btnDelete.setOnClickListener { onDelete(profile) }
            btnActivate.setOnClickListener { onActivate(profile) }
        }
    }
}
