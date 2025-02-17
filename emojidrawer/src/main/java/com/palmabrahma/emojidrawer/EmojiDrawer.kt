package com.palmabrahma.emojidrawer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class EmojiDrawer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val TAG = "EmojiDrawer"
    private val emojiContainer: LinearLayout
    private var emojiSize = 64
    private var emojiSpacing = 16
    private var animationDuration = 200L
    private var scaleFactor = 1.8f
    private var cornerRadius = 32f
    private var currentSelected: AppCompatImageView? = null
    private var defaultElevation = 8f.dpToPx()

    var onEmojiSelectedListener: OnEmojiSelectedListener? = null

    interface OnEmojiSelectedListener {
        fun onEmojiSelected(position: Int, drawableRes: Int)
    }

    init {
        clipChildren = false
        clipToPadding = false
        overScrollMode = OVER_SCROLL_NEVER

        emojiContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
            clipChildren = false
            clipToPadding = false
            gravity = Gravity.CENTER
        }
        emojiContainer.background = ContextCompat.getDrawable(context, R.drawable.rounded_rect_background)

        addView(emojiContainer)
        setupAttributes(attrs)
        close()
    }


    private fun createEmojiImageView(drawableRes: Int): AppCompatImageView {
        return AppCompatImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                emojiSize.dpToPx(),
                emojiSize.dpToPx()
            ).apply {
                marginEnd = emojiSpacing.dpToPx()
            }

            // VectorDrawableCompat support
            val drawable = ContextCompat.getDrawable(context, drawableRes)
            setImageDrawable(drawable)

            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = createRoundedBackground()

            // Add LTR/RTL support
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
            tag = drawableRes // Store resource ID here
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEmojiTouch(view: AppCompatImageView) {
        view.setOnTouchListener { vv, event ->
            val v = vv as AppCompatImageView
            val position = emojiContainer.indexOfChild(v)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    android.util.Log.d(TAG, "ACTION_DOWN view:$position")
                    animateEmojiSelection(v, true)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    android.util.Log.d(TAG, "ACTION_UP view:$position")
                    animateEmojiSelection(v, false)
                    if (isTouchInsideView(event, v)) {
                        handleEmojiSelection(v)
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    android.util.Log.d(TAG, "ACTION_CANCEL view:$position")
                    animateEmojiSelection(v, false)
                    true
                }

                else ->{
                    android.util.Log.d(TAG, "${event.action}. view:$position")
                    false
                }
            }
        }
    }

    private fun animateEmojiSelection(view: AppCompatImageView, selected: Boolean) {
        val animator = ValueAnimator.ofFloat(
            if (selected) 0f else 1f,
            if (selected) 1f else 0f
        ).apply {
            duration = animationDuration
            addUpdateListener {
                val fraction = it.animatedValue as Float
                view.scaleX = 1 + (scaleFactor - 1) * fraction
                view.scaleY = 1 + (scaleFactor - 1) * fraction
                if (selected) view.elevation += fraction
                else view.elevation += (fraction - 1)
                (view.background as MaterialShapeDrawable).let { bg ->
                    bg.shapeAppearanceModel = createShapeModel(cornerRadius * fraction)
                }
            }
        }
        //android.util.Log.d(TAG, "selected: $selected, elevation=${view.elevation}")
        currentSelected?.let { resetPreviousSelection(it) }
        currentSelected = view.takeIf { selected }
        animator.start()
    }

    private fun createShapeModel(radius: Float): ShapeAppearanceModel {
        return ShapeAppearanceModel.Builder()
            .setAllCorners(CornerFamily.ROUNDED, radius)
            .build()
    }


    fun setEmojis(drawableResIds: List<Int>) {
        emojiContainer.removeAllViews()
        drawableResIds.forEachIndexed { _, resId ->
            val imageView = createEmojiImageView(resId).apply {
                tag = resId // Store resource ID in view tag
            }
            setupEmojiTouch(imageView)
            emojiContainer.addView(imageView)
        }
    }

    // Corrected handleEmojiSelection
    private fun handleEmojiSelection(view: AppCompatImageView) {
        val position = emojiContainer.indexOfChild(view)
        val drawableRes = view.tag as? Int ?: 0
        onEmojiSelectedListener?.onEmojiSelected(position, drawableRes)
        close()
    }

    private fun resetPreviousSelection(view: AppCompatImageView) {
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(animationDuration)
            .start()
        (view.background as? MaterialShapeDrawable)?.shapeAppearanceModel =
            createShapeModel(80f)

        // Elevation fallback
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //view.elevation = defaultElevation
        //}

    }

    private fun createRoundedBackground(): MaterialShapeDrawable {
        return MaterialShapeDrawable(createShapeModel(80f)).apply {
            fillColor = ColorStateList.valueOf(Color.TRANSPARENT)
            //fillColor = ColorStateList.valueOf(Color.MAGENTA)
            setTint(Color.BLUE)

            // Elevation fallback
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //elevation = defaultElevation
            //}
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density

    private fun setupAttributes(attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.EmojiDrawer)

        try {
            emojiSize = a.getDimensionPixelSize(
                R.styleable.EmojiDrawer_emojiSize,
                64.dpToPx()
            )
            emojiSpacing = a.getDimensionPixelSize(
                R.styleable.EmojiDrawer_emojiSpacing,
                16.dpToPx()
            )
            scaleFactor = a.getFloat(
                R.styleable.EmojiDrawer_scaleFactor,
                1.8f
            )
            cornerRadius = a.getDimension(
                R.styleable.EmojiDrawer_cornerRadius,
                32f.dpToPx()
            )
            animationDuration = a.getInteger(
                R.styleable.EmojiDrawer_animationDuration,
                200
            ).toLong()
        } finally {
            a.recycle()
        }
    }

    private fun isTouchInsideView(event: MotionEvent, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return event.rawX >= location[0] &&
                event.rawX <= location[0] + view.width &&
                event.rawY >= location[1] &&
                event.rawY <= location[1] + view.height
    }

    fun toggleEmojiDrawer() {
        if (isOpen()) {
            close()
        } else {
            open()
        }
    }

    // Add this function inside the EmojiDrawer class
    fun isOpen(): Boolean {
        return visibility == View.VISIBLE && translationX == 0f
    }

    // Updated close() function to maintain state consistency
    fun close() {
        post {
            animate().translationX(width.toFloat())
                .setDuration(animationDuration)
                .withEndAction {
                    visibility = View.GONE
                    translationX = width.toFloat() // Maintain closed position
                }
                .start()
        }
    }

    // Updated open() function
    fun open() {
        post {
            visibility = View.VISIBLE
            animate().translationX(0f)
                .setDuration(animationDuration)
                .start()
        }
    }
    fun open1() {
        post {
           animate()
                .translationX(0f)
                .setDuration(animationDuration)
                .withLayer()
                .start()
        }
    }

    private fun close1() {
        post {
            animate()
                .translationX(width.toFloat())
                .setDuration(animationDuration)
                .withLayer()
                .withEndAction { visibility = GONE }
                .start()
        }
    }
}