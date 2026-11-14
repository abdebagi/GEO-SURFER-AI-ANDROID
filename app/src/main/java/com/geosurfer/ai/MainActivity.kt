package com.geosurfer.ai

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this)
        title.text = "GEO SURFER AI"
        title.textSize = 28f
        title.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

        setContentView(title)
    }
}
