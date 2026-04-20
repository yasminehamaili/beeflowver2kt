package com.pomodoro.app.ui.tasks

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pomodoro.app.data.models.Task
import com.pomodoro.app.databinding.FragmentTasksBinding
import com.pomodoro.app.ui.SharedViewModel

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SharedViewModel
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        setupRecyclerView()
        setupInput()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            onComplete = { viewModel.completeTask(it) },
            onDelete = { viewModel.deleteTask(it) },
            onSetActive = { task ->
                viewModel.setActiveTask(task)
                findNavController().navigate(
                    com.pomodoro.app.R.id.action_tasks_to_timer
                )
            },
            onSessionsChange = { task, sessions ->
                viewModel.updateTaskSessions(task, sessions)
            },
            getActiveTaskId = { viewModel.activeTask.value?.id },
            getColors = { viewModel.themeColors.value }
        )
        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter
    }

    private fun setupInput() {
        binding.btnAddTask.setOnClickListener { addTask() }
        binding.etNewTask.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addTask(); true } else false
        }
    }

    private fun addTask() {
        val name = binding.etNewTask.text.toString().trim()
        if (name.isNotEmpty()) {
            viewModel.addTask(name)
            binding.etNewTask.text?.clear()
        }
    }

    private fun setupObservers() {
        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
        }

        viewModel.themeColors.observe(viewLifecycleOwner) { colors ->
            binding.inputCard.setCardBackgroundColor(colors.surface)
            binding.btnAddTask.imageTintList = ColorStateList.valueOf(colors.accent)
            binding.tvTitle.setTextColor(colors.accent)
        }

        viewModel.activeTask.observe(viewLifecycleOwner) {
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}