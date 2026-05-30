package com.pomodoro.app.ui.timer

import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.DialogFragment
import com.pomodoro.app.databinding.DialogBeeCompleteBinding
import com.pomodoro.app.service.TimerService

class BeeCompleteDialog : DialogFragment() {

    private var _binding: DialogBeeCompleteBinding? = null
    private val binding get() = _binding!!

    var timerService: TimerService? = null
    var onDismissed: (() -> Unit)? = null

    companion object {
        private const val ARG_PRIMARY  = "primary"
        private const val ARG_ACCENT   = "accent"
        private const val ARG_SURFACE  = "surface"
        private const val ARG_IS_FOCUS = "is_focus"

        fun newInstance(
            primary: Int,
            accent: Int,
            surface: Int,
            isFocusSession: Boolean
        ): BeeCompleteDialog {
            return BeeCompleteDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PRIMARY,  primary)
                    putInt(ARG_ACCENT,   accent)
                    putInt(ARG_SURFACE,  surface)
                    putBoolean(ARG_IS_FOCUS, isFocusSession)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBeeCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val primary        = arguments?.getInt(ARG_PRIMARY,  0xFFE7992C.toInt()) ?: 0xFFE7992C.toInt()
        val accent         = arguments?.getInt(ARG_ACCENT,   0xFF483320.toInt()) ?: 0xFF483320.toInt()
        val surface        = arguments?.getInt(ARG_SURFACE,  0xFFFEE49A.toInt()) ?: 0xFFFEE49A.toInt()
        val isFocusSession = arguments?.getBoolean(ARG_IS_FOCUS, true) ?: true

        applyColors(primary, accent, surface)
        applyContent(isFocusSession)
        startBuzzAnimation()

        binding.btnAction.setOnClickListener {
            timerService?.stopAlarmVibration()
            onDismissed?.invoke()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
        }
    }

    private fun applyContent(isFocusSession: Boolean) {
        if (isFocusSession) {
            binding.tvTitle.text   = " "
            binding.tvMessage.text = "Super boulot ! Ta session focus est terminée.\nC\u2019est l\u2019heure de souffler \uD83C\uDF6F"
            binding.btnAction.text = "Prendre une pause \u2615"
        } else {
            binding.tvMessage.text = "Pause terminée, petite abeille !\nPrête à revoler au travail ? \uD83D\uDCAA"
            binding.btnAction.text = "Retour au focus \uD83D\uDCA1"
        }
    }

    private fun applyColors(primary: Int, accent: Int, surface: Int) {
        binding.apply {
            cardContent.backgroundTintList = ColorStateList.valueOf(surface)
            tvTitle.setTextColor(accent)
            tvMessage.setTextColor(accent)
            divider.setBackgroundColor(primary)
            btnAction.backgroundTintList   = ColorStateList.valueOf(primary)
            btnAction.setTextColor(Color.WHITE)
        }
    }

    private fun startBuzzAnimation() {
        val v = binding.tvBeeEmoji

        val sx = ObjectAnimator.ofFloat(v, View.SCALE_X, 1f, 1.2f, 1f).apply {
            duration = 280; repeatCount = ObjectAnimator.INFINITE
            interpolator = OvershootInterpolator(3f)
        }
        val sy = ObjectAnimator.ofFloat(v, View.SCALE_Y, 1f, 1.2f, 1f).apply {
            duration = 280; repeatCount = ObjectAnimator.INFINITE
            interpolator = OvershootInterpolator(3f)
        }
        val rot = ObjectAnimator.ofFloat(v, View.ROTATION, -12f, 12f).apply {
            duration = 140; repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        AnimatorSet().apply {
            playTogether(sx, sy, rot)
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}