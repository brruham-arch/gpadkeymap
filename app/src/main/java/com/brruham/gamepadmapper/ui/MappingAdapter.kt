package com.brruham.gamepadmapper.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.brruham.gamepadmapper.databinding.ItemMappingBinding
import com.brruham.gamepadmapper.model.ButtonMapping

class MappingAdapter(
    private val mappings: List<ButtonMapping>,
    private val onEdit: (ButtonMapping) -> Unit,
    private val onDelete: (ButtonMapping) -> Unit
) : RecyclerView.Adapter<MappingAdapter.VH>() {

    inner class VH(val binding: ItemMappingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMappingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = mappings.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = mappings[position]
        with(holder.binding) {
            tvButtonName.text = m.buttonLabel
            tvActionType.text = m.actionType.name
            tvCoords.text = when (m.actionType) {
                com.brruham.gamepadmapper.model.ActionType.TAP,
                com.brruham.gamepadmapper.model.ActionType.HOLD,
                com.brruham.gamepadmapper.model.ActionType.MULTI_TAP ->
                    "(${m.point.x.toInt()}, ${m.point.y.toInt()})"
                com.brruham.gamepadmapper.model.ActionType.SWIPE ->
                    "(${m.swipeFrom.x.toInt()},${m.swipeFrom.y.toInt()}) → (${m.swipeTo.x.toInt()},${m.swipeTo.y.toInt()})"
                com.brruham.gamepadmapper.model.ActionType.GESTURE_PATH ->
                    "${m.gesturePath.size} points"
                com.brruham.gamepadmapper.model.ActionType.JOYSTICK_SWIPE ->
                    "center (${m.joystickCenterX.toInt()}, ${m.joystickCenterY.toInt()})"
            }
            btnEdit.setOnClickListener { onEdit(m) }
            btnDelete.setOnClickListener { onDelete(m) }
        }
    }
}
