package com.geosurfer.ai

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 30)
            setBackgroundColor(Color.rgb(15, 25, 38))
        }

        val title = TextView(this).apply {
            text = "GEO SURFER AI PRO"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 28, 8, 12)
        }

        main.addView(title)

        val subtitle = TextView(this).apply {
            text = "ADVANCED GEOLOGICAL ANALYSIS"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 28)
        }

        main.addView(subtitle)

        val features = listOf(
            "🗺 Geological Maps",
            "🛰 Satellite Image Analysis",
            "🥇 Gold & Silver Detection Indicators",
            "🌡 Thermal Analysis",
            "🌈 Spectral Analysis",
            "📍 Coordinate Explorer",
            "📊 3D Geological Mapping",
            "🔎 Underground Voids & Cavities Detection",
            "⚙ Metal Density & Buried Objects Analysis",
            "📐 Surfer Filters Center"
        )

        features.forEach { feature ->

            val item = TextView(this).apply {
                text = feature
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(18, 22, 18, 22)

                background = GradientDrawable().apply {
                    setColor(Color.rgb(30, 48, 65))
                    cornerRadius = 18f
                }

                isClickable = true
                isFocusable = true

                setOnClickListener {
                    if (feature.contains("Surfer Filters")) {
                        showSurferFilters()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "$feature - Module under development",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(0, 0, 0, 14)

            main.addView(item, params)
        }

        val footer = TextView(this).apply {
            text = "GEO SURFER AI PRO\nGeological Intelligence Platform"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(8, 25, 8, 10)
        }

        main.addView(footer)

        scrollView.addView(main)

        setContentView(scrollView)
    }

    private fun showSurferFilters() {

        val filters = arrayOf(
            "Gaussian Filter",
            "Low Pass Filter",
            "High Pass Filter",
            "Median Filter",
            "Laplacian Filter",
            "Directional Filter",
            "First Derivative",
            "Second Derivative"
        )

        AlertDialog.Builder(this)
            .setTitle("SURFER FILTERS CENTER")
            .setItems(filters) { _, position ->

                Toast.makeText(
                    this,
                    filters[position] + " - Engine under development",
                    Toast.LENGTH_SHORT
                ).show()

            }
            .setNegativeButton("Close", null)
            .show()
    }
}
