package com.geosurfer.ai

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.util.Locale

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

    // SURFER FILTERS CENTER

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
                showDataInput(filters[position])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // GEOLOGICAL DATA INPUT

    private fun showDataInput(filterName: String) {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 15, 25, 10)
        }

        val label = TextView(this).apply {
            text = "Enter geological grid data"
            textSize = 16f
            setTextColor(Color.BLACK)
        }

        layout.addView(label)

        val input = EditText(this).apply {
            hint = "Enter rows separated by commas"
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE

            minLines = 5
            gravity = Gravity.TOP
            setText(
                "10,12,13,12,10\n" +
                "12,15,18,15,12\n" +
                "13,18,25,18,13\n" +
                "12,15,18,15,12\n" +
                "10,12,13,12,10"
            )
        }

        layout.addView(input)

        val cellLabel = TextView(this).apply {
            text = "Cell Size"
            setTextColor(Color.BLACK)
            textSize = 15f
        }

        layout.addView(cellLabel)

        val cellInput = EditText(this).apply {
            inputType =
                InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL

            setText("1.0")
            hint = "Cell size"
        }

        layout.addView(cellInput)

        AlertDialog.Builder(this)
            .setTitle(filterName)
            .setView(layout)
            .setPositiveButton("RUN ANALYSIS") { _, _ ->

                try {

                    val grid = parseGrid(input.text.toString())

                    val cellSize =
                        cellInput.text.toString().toDouble()

                    require(cellSize > 0.0) {
                        "Cell size must be positive"
                    }

                    val result = executeFilter(
                        filterName,
                        grid,
                        cellSize
                    )

                    showResult(filterName, result)

                } catch (e: Exception) {

                    AlertDialog.Builder(this)
                        .setTitle("Analysis Error")
                        .setMessage(
                            e.message ?: "Invalid input data"
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // PARSE GRID DATA

    private fun parseGrid(text: String): Array<DoubleArray> {

        val rows = text.trim()
            .lines()
            .filter { it.isNotBlank() }

        require(rows.isNotEmpty()) {
            "Grid data is empty"
        }

        val grid = rows.map { row ->

            row.trim()
                .split(Regex("[,;\\s]+"))
                .filter { it.isNotBlank() }
                .map { it.toDouble() }
                .toDoubleArray()

        }.toTypedArray()

        require(grid.size >= 2 && grid[0].size >= 2) {
            "Grid must contain at least 2 rows and 2 columns"
        }

        require(grid.all {
            it.size == grid[0].size
        }) {
            "All rows must have the same number of columns"
        }

        return grid
    }

    // EXECUTE FILTER ENGINE

    private fun executeFilter(
        name: String,
        grid: Array<DoubleArray>,
        cellSize: Double
    ): String {

        return when (name) {

            "Gaussian Filter" -> {
                formatGrid(
                    GridFilterEngine.gaussian(grid)
                )
            }

            "Low Pass Filter" -> {
                formatGrid(
                    GridFilterEngine.lowPass(grid)
                )
            }

            "High Pass Filter" -> {
                formatGrid(
                    GridFilterEngine.highPass(grid)
                )
            }

            "Median Filter" -> {
                formatGrid(
                    GridFilterEngine.median(grid)
                )
            }

            "Laplacian Filter" -> {
                formatGrid(
                    GridFilterEngine.laplacian(grid)
                )
            }

            "Directional Filter" -> {

                val horizontal =
                    GridFilterEngine.directionalHorizontal(grid)

                val vertical =
                    GridFilterEngine.directionalVertical(grid)

                "HORIZONTAL DIRECTION:\n\n" +
                formatGrid(horizontal) +
                "\n\nVERTICAL DIRECTION:\n\n" +
                formatGrid(vertical)
            }

            "First Derivative" -> {

                val dx =
                    GridFilterEngine.firstDerivativeX(
                        grid, cellSize
                    )

                val dy =
                    GridFilterEngine.firstDerivativeY(
                        grid, cellSize
                    )

                "FIRST DERIVATIVE X:\n\n" +
                formatGrid(dx) +
                "\n\nFIRST DERIVATIVE Y:\n\n" +
                formatGrid(dy)
            }

            "Second Derivative" -> {

                formatGrid(
                    GridFilterEngine.secondDerivative(
                        grid, cellSize
                    )
                )
            }

            else -> "Unknown filter"
        }
    }

    // FORMAT RESULTS

    private fun formatGrid(
        grid: Array<DoubleArray>
    ): String {

        return grid.joinToString("\n") { row ->

            row.joinToString("   ") {
                String.format(Locale.US, "%.3f", it)
            }

        }
    }

    // DISPLAY RESULTS

    private fun showResult(
        filterName: String,
        result: String
    ) {

        val scroll = ScrollView(this)

        val output = TextView(this).apply {
            text = result
            textSize = 15f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.BLACK)
            setPadding(20, 20, 20, 20)
        }

        scroll.addView(output)

        AlertDialog.Builder(this)
            .setTitle("$filterName - RESULTS")
            .setView(scroll)
            .setPositiveButton("DONE", null)
            .show()
    }
}
