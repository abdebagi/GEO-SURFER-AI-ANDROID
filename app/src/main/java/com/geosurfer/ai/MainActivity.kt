package com.geosurfer.ai

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val main = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24, 32, 24, 24)
        setBackgroundColor(Color.rgb(15, 25, 38))
    }

    val title = TextView(this).apply {
        text = "GEO SURFER AI PRO"
        textSize = 27f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setPadding(8, 24, 8, 24)
    }

    main.addView(title)

    val subtitle = TextView(this).apply {
        text = "ADVANCED GEOLOGICAL ANALYSIS"
        textSize = 13f
        setTextColor(Color.LTGRAY)
        gravity = Gravity.CENTER
        setPadding(8, 8, 8, 30)
    }

    main.addView(subtitle)

    val features = listOf(
        "🗺 Geological Maps",
        "🛰 Satellite Image Analysis",
        "🥇 Gold & Silver Detection Indicators",
        "🌡 Thermal Analysis",
        "🌈 Spectral Analysis",
        "📍 Coordinate Explorer",
        "📊 3D Geological Mapping"
    )

    features.forEach { feature ->
        val item = TextView(this).apply {
            text = feature
            textSize = 17f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(30, 48, 65))
            setPadding(18, 20, 18, 20)
            gravity = Gravity.CENTER_VERTICAL
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, 14)
        main.addView(item, params)
    }

    setContentView(main)
}

}
