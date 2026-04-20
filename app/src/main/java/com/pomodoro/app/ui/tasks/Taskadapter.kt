package com.pomodoro.app.ui.tasks

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pomodoro.app.R
import com.pomodoro.app.data.models.Task
import com.pomodoro.app.databinding.ItemTaskBinding
import com.pomodoro.app.ui.ThemeColors

class TaskAdapter(
    private val onComplete: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onSetActive: (Task) -> Unit,
    private val onSessionsChange: (Task, Int) -> Unit,
    private val getActiveTaskId: () -> String?,
    private val getColors: () -> ThemeColors?
) : ListAdapter<Task, TaskAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val b: ItemTaskBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(task: Task) {
            val colors = getColors()
            val accent = colors?.accent ?: Color.parseColor("#483320")
            val surface = colors?.surface ?: Color.parseColor("#FEE49A")
            val primary = colors?.primary ?: Color.parseColor("#E7992C")
            val isActive = getActiveTaskId() == task.id

            b.tvTaskName.text = task.name
            b.tvSessionCount.text = "${task.sessions}"

            if (task.completed) {
                b.tvTaskName.paintFlags = b.tvTaskName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                b.tvTaskName.alpha = 0.6f
                b.tagDone.visibility = android.view.View.VISIBLE
                b.btnSetActive.visibility = android.view.View.GONE
                b.tagActive.visibility = android.view.View.GONE
                b.ivStatus.setImageResource(R.drawable.ic_check_circle)
                b.ivStatus.imageTintList = ColorStateList.valueOf(Color.parseColor("#FFA656"))
            } else {
                b.tvTaskName.paintFlags = b.tvTaskName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                b.tvTaskName.alpha = 1f
                b.tagDone.visibility = android.view.View.GONE
                b.ivStatus.setImageResource(R.drawable.ic_circle)
                b.ivStatus.imageTintList = ColorStateList.valueOf(Color.parseColor("#FD5B71"))

                if (isActive) {
                    b.btnSetActive.visibility = android.view.View.GONE
                    b.tagActive.visibility = android.view.View.VISIBLE
                    b.tagActive.backgroundTintList = ColorStateList.valueOf(primary)
                } else {
                    b.btnSetActive.visibility = android.view.View.VISIBLE
                    b.tagActive.visibility = android.view.View.GONE
                    b.btnSetActive.backgroundTintList = ColorStateList.valueOf(surface)
                    b.btnSetActive.setTextColor(accent)
                }
            }

            b.sessionControls.backgroundTintList = ColorStateList.valueOf(surface)
            b.btnDecSessions.imageTintList = ColorStateList.valueOf(accent)
            b.btnIncSessions.imageTintList = ColorStateList.valueOf(accent)
            b.tvSessionCount.setTextColor(accent)
            b.tvTaskName.setTextColor(accent)
            b.tvSessionLabel.setTextColor(accent)

            b.ivStatus.setOnClickListener { onComplete(task) }
            b.btnDelete.setOnClickListener { onDelete(task) }
            b.btnSetActive.setOnClickListener { onSetActive(task) }
            b.btnIncSessions.setOnClickListener { onSessionsChange(task, task.sessions + 1) }
            b.btnDecSessions.setOnClickListener {
                if (task.sessions > 0) onSessionsChange(task, task.sessions - 1)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(old: Task, new: Task) = old.id == new.id
        override fun areContentsTheSame(old: Task, new: Task) = old == new
    }
}