package com.palmabrahma.slidetray

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class EmojiPopupWindow(context: Context, private var anchor: View) {

    private var displayMetrics: DisplayMetrics
    private val doElevation: Boolean = true
    private val popupWindow: PopupWindow
    private val contentView: FrameLayout
    private val emojiContainer: LinearLayout
    private var isShowing = false
    private var emojiSize = 48
    private var emojiSpacing = 4
    private var animationDuration = 200L
    private var scaleFactor = 3.8f
    private var cornerRadius = 32f
    private var currentSelected: AppCompatImageView? = null
    private var defaultElevation = 0f

    var onEmojiSelectedListener: OnEmojiSelectedListener? = null

    interface OnEmojiSelectedListener {
        fun onEmojiSelected(index: Int, drawableRes: Int)
    }

    init {
        val inflater = LayoutInflater.from(context)
        contentView = (inflater.inflate(R.layout.emoji_drawer_popup, null) as FrameLayout).apply {
            // Disable clipping in the entire hierarchy
            clipChildren = false
            clipToPadding = false
        }
        //defaultElevation = 8f.dpToPx()

        emojiContainer = contentView.findViewById<LinearLayout>(R.id.emoji_container2).apply {
            clipChildren = false
            clipToPadding = false
            gravity = Gravity.CENTER
            background = AppCompatResources.getDrawable(
                context,
                com.palmabrahma.emojidrawer.R.drawable.rounded_rect_background
            )
            setBackgroundColor(Color.BLUE)
        }

        popupWindow = PopupWindow(
            contentView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            // Allow drawing outside the window
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isClippingEnabled = false // Critical for overflow
            //elevation = 20f
        }
        popupWindow.setOnDismissListener { isShowing = false }
        displayMetrics = contentView.context.resources.displayMetrics
        //popupWindow.overlapAnchor = true
        //popupWindow.setTouchInterceptor { v, event ->
        //false
        //}

    }

    private fun createEmojiImageView(drawableRes: Int): AppCompatImageView {
        return AppCompatImageView(contentView.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                emojiSize.dpToPx(),
                emojiSize.dpToPx()
            ).apply {
                marginEnd = emojiSpacing.dpToPx()
            }

            val drawable = ContextCompat.getDrawable(context, drawableRes)
            setImageDrawable(drawable)

            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = createRoundedBackground()

            // Add LTR/RTL support
            layoutDirection = View.LAYOUT_DIRECTION_LOCALE
            tag = drawableRes // Store resource ID here

            //outlineProvider = ViewOutlineProvider.BACKGROUND
            //cameraDistance = 12 * resources.displayMetrics.density
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupEmojiTouch(view: AppCompatImageView) {
        view.setOnTouchListener { vv, event ->
            val v = vv as AppCompatImageView
            val index = emojiContainer.indexOfChild(v)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    android.util.Log.d(TAG, "ACTION_DOWN view:$index")
                    if (currentSelected != v)
                        animateEmojiSelection(v, true)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    android.util.Log.d(TAG, "ACTION_UP view:$index")
                    findViewFromEvent(event)?.let { imageView ->
                        animateEmojiSelection(imageView, false)
                        handleEmojiSelection(imageView)
                    }?:run {
                        currentSelected?.let {
                            animateEmojiSelection(it, false)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    android.util.Log.d(TAG, "ACTION_CANCEL view:$index")
                    animateEmojiSelection(v, false)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    android.util.Log.d(TAG, "ACTION_MOVE view:$index")
                    findViewFromEvent(event)?.let { imageView ->
                        if (currentSelected != imageView)
                            animateEmojiSelection(imageView, true)
                    }
                    true
                }

                else -> {
                    android.util.Log.d(TAG, "${event.action}. view:$index")
                    false
                }
            }
        }
    }

    private fun findViewFromEvent(event: MotionEvent): AppCompatImageView? {
        emojiContainer.children.forEach { vv ->
            val v = vv as AppCompatImageView
            if (isTouchInsideView(event, v))
                return v
        }
        return null
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
                if (doElevation) {
                    if (selected) view.elevation += fraction
                    else view.elevation += (fraction - 1)
                }
                (view.background as MaterialShapeDrawable).shapeAppearanceModel =
                    createShapeModel(cornerRadius * fraction)
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

    fun setAnchor(anchor: View) {
        this.anchor = anchor
    }

    fun setEmojis(drawableResIds: List<Int>) {
        emojiContainer.removeAllViews()
        drawableResIds.forEachIndexed { _, resId ->
            val imageView = createEmojiImageView(resId).apply {
                tag = resId // Store resource ID in view tag
            }
            setupEmojiTouch(imageView)
            val layoutParams = LinearLayout.LayoutParams(
                emojiSize.dpToPx(),
                emojiSize.dpToPx()
            ).apply {
                marginEnd = emojiSpacing.dpToPx()
            }

            emojiContainer.addView(imageView, layoutParams)
        }
    }

    fun setEmojis1(emojiResIds: List<Int>) {
        emojiContainer.removeAllViews()
        emojiResIds.forEach { resId ->
            val imageView = AppCompatImageView(contentView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    64.dpToPx(),
                    64.dpToPx()
                ).apply {
                    marginEnd = 16.dpToPx()
                }
                setImageResource(resId)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                tag = resId
            }

            setupEmojiTouch(imageView)
            emojiContainer.addView(imageView)
        }
    }

    private fun handleEmojiSelection(view: AppCompatImageView) {
        val index = emojiContainer.indexOfChild(view)
        val drawableRes = view.tag as? Int ?: 0
        onEmojiSelectedListener?.onEmojiSelected(index, drawableRes)
        close()
    }

    private fun resetPreviousSelection(view: AppCompatImageView) {
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(animationDuration)
            .start()
        (view.background as? MaterialShapeDrawable)?.apply {
            shapeAppearanceModel = createShapeModel(80f)
            setShadowColor(Color.TRANSPARENT)
            setUseTintColorForShadow(true)
            setTint(Color.TRANSPARENT)
            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
        }
        // Elevation fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.elevation = defaultElevation
        }

    }

    private fun createRoundedBackground(): MaterialShapeDrawable {
        return MaterialShapeDrawable(createShapeModel(80f)).apply {
            fillColor = ColorStateList.valueOf(Color.TRANSPARENT)
            setShadowColor(Color.TRANSPARENT)
            setUseTintColorForShadow(true)
            setTint(Color.TRANSPARENT)
            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS

            // Elevation fallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = defaultElevation
            }
        }
    }

    private fun isTouchInsideView(event: MotionEvent, view: AppCompatImageView): Boolean {
        val emojiContainerLocation = IntArray(2)
        val viewLocationRect = Rect()
        view.getDrawingRect(viewLocationRect)
        emojiContainer.getLocationOnScreen(emojiContainerLocation)
        emojiContainer.offsetDescendantRectToMyCoords(view, viewLocationRect)
        viewLocationRect.offset(emojiContainerLocation[0], emojiContainerLocation[1])

        val isInside = event.rawX >= viewLocationRect.left &&
                event.rawX <= viewLocationRect.right &&
                event.rawY >= viewLocationRect.top &&
                event.rawY <= viewLocationRect.bottom
        return isInside
    }

    fun toggleEmojiDrawer() {
        if (isShowing) close()
        else open(anchor)
    }

    private fun close() {
        val width = contentView.width
        contentView.animate()
            .translationX(width.toFloat())
            .setDuration(300)
            .withEndAction {
                popupWindow.dismiss()
                isShowing = false
            }
            .start()
    }

    private fun open(anchor: View) {
        contentView.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val anchorLocation = intArrayOf(0, 0)
        anchor.getLocationInWindow(anchorLocation)
        if (isShowing)
            return
        val xOffset = 0 //anchorLocation[0]
        val yOffset = anchorLocation[1] + anchor.height
        //popupWindow.showAsDropDown(anchor, 0, 0, Gravity.START or Gravity.TOP)
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xOffset, yOffset)
        isShowing = true

        // Initial position off-screen
        val displayWidth = displayMetrics.widthPixels
        contentView.translationX = displayWidth.toFloat()
        contentView.animate()
            .translationX((displayWidth - contentView.measuredWidth).toFloat())
            .setDuration(animationDuration)
            .start()
    }


    private fun Int.dpToPx(): Int = (this * displayMetrics.density).toInt()
    private fun Float.dpToPx(): Float = this * displayMetrics.density

    companion object {
        const val TAG = "EmojiPopupWindow"
    }
}