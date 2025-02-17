package com.palmabrahma.slidetray

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView


class TrayUtil {
    companion object {
        fun addEmojis(parent: View) {
            val drawer : HorizontalScrollView = parent.findViewById(R.id.emoji_drawer)
            val emojis = listOf("😀", "😂", "❤️", "👍", "👋", "🎉")
            val container = parent.findViewById<LinearLayout>(R.id.emoji_container)

            emojis.forEach { emoji ->
                val textView = TextView(parent.context).apply {
                    text = emoji
                    textSize = 40f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                        marginEnd = 8
                    }
                }
                container.addView(textView)
                setupEmojiTouch(textView, drawer)
            }
            drawer.post {
                drawer.translationX = drawer.width.toFloat()
                drawer.visibility = View.INVISIBLE
            }

        }

        fun closeEmojiDrawer(drawer: HorizontalScrollView) {
            drawer.post {
                drawer.animate()
                    .translationX(drawer.width.toFloat())
                    .setDuration(300)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction { drawer.visibility = View.INVISIBLE }
                    .start()
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setupEmojiTouch(view: View, drawer: HorizontalScrollView) {
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(1.5f)
                            .scaleY(1.5f)
                            .setDuration(200)
                            .setInterpolator(OvershootInterpolator())
                            .start()
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                        if (isTouchInsideView(event, v)) {
                            closeEmojiDrawer(drawer)
                            // Handle emoji selection here
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        private fun isTouchInsideView(event: MotionEvent, view: View): Boolean {
            val rect = Rect()
            view.getGlobalVisibleRect(rect)
            return rect.contains(event.rawX.toInt(), event.rawY.toInt())
        }



        fun toggleEmojiDrawer(parentView: View) {
            val drawer : HorizontalScrollView = parentView.findViewById(R.id.emoji_drawer)
            if (drawer.translationX == 0f) {
                closeEmojiDrawer(drawer)
            } else {
                openEmojiDrawer(drawer)
            }
        }

        private fun openEmojiDrawer(drawer: HorizontalScrollView) {
            drawer.post {
                drawer.visibility = View.VISIBLE
                drawer.animate()
                    .translationX(0f)
                    .setDuration(300)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }


    }
}