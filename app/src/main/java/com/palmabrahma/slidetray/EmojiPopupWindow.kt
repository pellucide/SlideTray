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
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class EmojiPopupWindow(context: Context, private var anchor: View) {

    private lateinit var displayMetrics : DisplayMetrics
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

        emojiContainer = contentView.findViewById<LinearLayout>(R.id.emoji_container).apply {
            clipChildren = false
            clipToPadding = false
            gravity = Gravity.CENTER
            background = context.getDrawable(com.palmabrahma.emojidrawer.R.drawable.rounded_rect_background)
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

    /* Old code

    init {
        val inflater = LayoutInflater.from(context)
        contentView = inflater.inflate(R.layout.emoji_drawer_popup, null)
        emojiContainer = contentView.findViewById(R.id.emoji_container).apply {
            clipChildren = false
            clipToPadding = false
        }
        emojiContainer.apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
            clipChildren = false
            clipToPadding = false
            gravity = Gravity.CENTER
        }
        val horizontalScroll = contentView.findViewById<HorizontalScrollView>(R.id.horizontal_scroll)
        horizontalScroll.clipChildren = false
        horizontalScroll.clipToPadding = false
        horizontalScroll.overScrollMode = OVER_SCROLL_NEVER

        popupWindow = PopupWindow(
            contentView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ContextCompat.getDrawable(context, android.R.color.transparent))
            isOutsideTouchable = true
            elevation = 8f
        }
    }

     */


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
                    animateEmojiSelection(v, true)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    android.util.Log.d(TAG, "ACTION_UP view:$index")
                    animateEmojiSelection(v, false)
                    handleEmojiSelection(v)
                    /* TODO: isTouchInsideView does not work. Debug and fix
                    if (isTouchInsideView(event, v)) {
                        handleEmojiSelection(v)
                    }
                     */
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    android.util.Log.d(TAG, "ACTION_CANCEL view:$index")
                    animateEmojiSelection(v, false)
                    true
                }

                else ->{
                    android.util.Log.d(TAG, "${event.action}. view:$index")
                    false
                }
            }
        }
    }

    private fun setupTouchListener(view: AppCompatImageView) {
        view.setOnTouchListener { vv, event ->
            val v = vv as AppCompatImageView
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(1.8f)
                        .scaleY(1.8f)
                        .translationZ(32f)
                        .setDuration(200)
                        .withLayer() // Enable hardware layer
                        .start()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationZ(0f)
                        .setDuration(200)
                        .start()

                    if (isTouchInsideView(event, v)) {
                        //onEmojiSelectedListener?.invoke(v.tag as Int)
                        val index = emojiContainer.indexOfChild(view)
                        val drawableRes = view.tag as? Int ?: 0
                        onEmojiSelectedListener?.onEmojiSelected(index, drawableRes)
                        close()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationZ(0f)
                        .setDuration(200)
                        .start()
                    true
                }
                else -> false
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
                if (doElevation){
                    if (selected) view.elevation += fraction
                    else view.elevation += (fraction - 1)
                }
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
            emojiContainer.addView(imageView)
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
            shadowCompatibilityMode  = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
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
            shadowCompatibilityMode  = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS

            // Elevation fallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            elevation = defaultElevation
            }
        }
    }

    //TODO: this does not work. Try to debug and fix
    private fun isTouchInsideView(event: MotionEvent, view: AppCompatImageView): Boolean {
        val emojiContainerLocation = IntArray(2)
        anchor.getLocationOnScreen(emojiContainerLocation)
        android.util.Log.d(TAG, "emojiContainerLocation=${emojiContainerLocation[0]}, ${emojiContainerLocation[1]}")
        val contentViewLocation = IntArray(2)
        anchor.getLocationOnScreen(contentViewLocation)
        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)

        val viewGlobalVisibleRect = Rect()
        view.getGlobalVisibleRect(viewGlobalVisibleRect)

        val viewDrawingRect = Rect()
        view.getDrawingRect(viewDrawingRect)

        val emojiContainerWidth = emojiContainer.measuredWidth
        android.util.Log.d(TAG, "emojiContainerWidth $emojiContainerWidth")
        android.util.Log.d(TAG, "contentViewLocation=${contentViewLocation[0]}, ${contentViewLocation[1]}")
        android.util.Log.d(TAG, "anchorLocation=${anchorLocation[0]}, ${anchorLocation[1]}")
        android.util.Log.d(TAG, "viewLocation=${viewLocation[0]}, ${viewLocation[1]}")
        android.util.Log.d(TAG, "viewGlobalVisibleRect=${viewGlobalVisibleRect}")
        android.util.Log.d(TAG, "viewDrawingRect=${viewDrawingRect}")
        android.util.Log.d(TAG, "contentView.translation=${contentView.translationX}, ${contentView.translationY}")
        android.util.Log.d(TAG, "contentView.width=${contentView.width}")
        android.util.Log.d(TAG, "contentView.measuredWidth=${contentView.measuredWidth}")
        android.util.Log.d(TAG, "contentView.left=${contentView.left}")
        android.util.Log.d(TAG, "event=${event.rawX}, ${event.rawY}")
        android.util.Log.d(TAG, "contentView.paddingLeft=${contentView.paddingLeft}")
        android.util.Log.d(TAG, "anchor.width=${anchor.width}")
        android.util.Log.d(TAG, "anchor.left=${anchor.left}")
        viewLocation[0] += anchor.width + anchor.left
        android.util.Log.d(TAG, "-viewLocation=${viewLocation[0]}, ${viewLocation[1]}")
        val isInside =  event.rawX >= viewLocation[0] &&
                event.rawX <= viewLocation[0] + view.width &&
                event.rawY >= viewLocation[1] &&
                event.rawY <= viewLocation[1] + view.height
        android.util.Log.d(TAG, "isInside = $isInside")
        return isInside
    }

    fun toggleEmojiDrawer(){
        if (isShowing) close()
        else open(anchor)
    }

    fun close() {
        val width = contentView.width
        android.util.Log.d(TAG, "width = $width, isShowing=$isShowing")
        contentView.animate()
            .translationX(width.toFloat())
            .setDuration(300)
            .withEndAction {
                popupWindow.dismiss()
                isShowing = false
            }
            .start()
    }

    fun open(anchor: View) {
        contentView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val anchorLocation = intArrayOf(0,0)
        anchor.getLocationInWindow(anchorLocation)
        android.util.Log.d(TAG, "contentView.measuredWidth=${contentView.measuredWidth},isShowing=$isShowing")
        if (isShowing) return
        val xOffset = 0 //anchorLocation[0]
        val yOffset = anchorLocation[1] + anchor.height
        //popupWindow.showAsDropDown(anchor, 0, 0, Gravity.START or Gravity.TOP)
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xOffset, yOffset)
        isShowing = true

        // Initial position off-screen
        android.util.Log.d(TAG, "contentView.translationX ${contentView.translationX}")
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