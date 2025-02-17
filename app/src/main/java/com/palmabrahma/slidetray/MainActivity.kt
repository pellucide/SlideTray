package com.palmabrahma.slidetray

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.palmabrahma.emojidrawer.EmojiDrawer

class MainActivity : AppCompatActivity() {
    lateinit var drawer :HorizontalScrollView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val parentView = findViewById<View>(android.R.id.content)

        findViewById<TextView>(R.id.text).setOnClickListener {
            TrayUtil.toggleEmojiDrawer(parentView)
        }
        TrayUtil.addEmojis(parentView)


        findViewById<TextView>(R.id.text1).setOnClickListener {
            TrayUtil1.toggleEmojiDrawer(parentView)
        }
        TrayUtil1.addEmojis(parentView)


        val emojiDrawer = findViewById<EmojiDrawer>(R.id.emojiDrawer)
        val emojis = listOf(
            R.drawable.angel,
            R.drawable.santa,
            R.drawable.female_fairy,
            R.drawable.vampire,
            R.drawable.mx_claus
        )
        emojiDrawer.setEmojis(emojis)
        findViewById<TextView>(R.id.text2).setOnClickListener {
            emojiDrawer.toggleEmojiDrawer()
        }
        emojiDrawer.onEmojiSelectedListener = object : EmojiDrawer.OnEmojiSelectedListener {
            override fun onEmojiSelected(position: Int, drawableRes: Int) {
                val snack = Snackbar.make(emojiDrawer, "emoji was selected", LENGTH_LONG)
                snack.show()
            }
        }



        val popButton = findViewById<Button>(R.id.text3)
        val emojiDrawerPopupWindow = EmojiPopupWindow(this, popButton)
        emojiDrawerPopupWindow.setEmojis(emojis)

        emojiDrawerPopupWindow.onEmojiSelectedListener = object : EmojiPopupWindow.OnEmojiSelectedListener {
            override fun onEmojiSelected(index: Int, drawableRes: Int) {
                val snack = Snackbar.make(emojiDrawer, "emoji $index was selected", LENGTH_LONG)
                snack.show()
            }
        }

        popButton.setOnClickListener {
            emojiDrawerPopupWindow.toggleEmojiDrawer()
        }
    }
}
