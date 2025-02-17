import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import com.palmabrahma.slidetray.R

class TrayUtil1 {
    companion object {
        fun getScaleAnim(v:View) {
            val scaleAnim = v.animate()
                .scaleX(2.2f)
                .scaleY(2.2f)
                .translationZ(64f)
                .withLayer()  // Use hardware layer during animation
                .setDuration(250)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                //scaleAnim.roundedCorners(48f)  // Add rounded corners if needed
            }
        }
        fun addEmojis(parent: View) {
            val drawer : HorizontalScrollView = parent.findViewById(R.id.emoji_drawer1)
            // Create emoji items
            val emojis = listOf(
                R.drawable.angel,
                R.drawable.santa,
                R.drawable.female_fairy,
                R.drawable.vampire,
                R.drawable.mx_claus
            )

            val container = parent.findViewById<LinearLayout>(R.id.emoji_container1)

            emojis.forEach { drawableRes ->
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.emoji_item, container, false)

                val imageView = itemView.findViewById<ImageView>(R.id.emoji_image)
                imageView.setImageResource(drawableRes)

                container.addView(itemView)
                setupEmojiTouch(imageView, drawer)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setupEmojiTouch(view: View, drawer: HorizontalScrollView) {
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(1.8f)
                            .scaleY(1.8f)
                            .translationZ(32f)
                            .setDuration(200)
                            .setInterpolator(OvershootInterpolator())
                            .start()
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationZ(0f)
                            .setDuration(200)
                            .start()
                        if (event.action == MotionEvent.ACTION_UP && isTouchInside(event, v)) {
                            closeEmojiDrawer(drawer)
                            // Handle emoji selection
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        private fun isTouchInside(event: MotionEvent, view: View): Boolean {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val x = event.rawX
            val y = event.rawY
            return x > location[0] && x < location[0] + view.width &&
                    y > location[1] && y < location[1] + view.height
        }

        private fun openEmojiDrawer(drawer: HorizontalScrollView) {
            drawer.post {
                drawer.visibility = View.VISIBLE
                drawer.animate()
                    .translationX(0f)
                    .setDuration(350)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .start()
            }
        }

        private fun closeEmojiDrawer(drawer: HorizontalScrollView) {
            drawer.post {
                drawer.animate()
                    .translationX(drawer.width.toFloat())
                    .setDuration(250)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction { drawer.visibility = View.INVISIBLE }
                    .start()
            }
        }

        fun toggleEmojiDrawer(parentView: View) {
            val drawer : HorizontalScrollView = parentView.findViewById(R.id.emoji_drawer1)
            if (drawer.translationX == 0f) {
                closeEmojiDrawer(drawer)
            } else {
                openEmojiDrawer(drawer)
            }
        }
    }
}