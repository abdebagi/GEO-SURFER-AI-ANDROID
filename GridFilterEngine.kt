package com.geosurfer.ai

import kotlin.math.abs
import kotlin.math.sqrt

/**

* GEO-SURFER-AI-PRO

* Grid Filter Engine

* 

* محرك معالجة الشبكات الجيولوجية وتحسين البيانات المكانية.
  */
  object GridFilterEngine {
  
  data class GridData(
  val width: Int,
  val height: Int,
  val values: DoubleArray
  ) {
  init {
  require(width > 0 && height > 0) {
  "Grid dimensions must be positive"
  }
  require(values.size == width * height) {
  "Grid data size does not match dimensions"
  }
  }
  
   operator fun get(x: Int, y: Int): Double {
     require(x in 0 until width && y in 0 until height)
     return values[y * width + x]
 }
  
  }
  
  /**
  
  * Mean filter
  
  * تقليل الضوضاء باستخدام المتوسط الحسابي.
    */
    fun meanFilter(
    grid: GridData,
    radius: Int = 1
    ): GridData {
    
    require(radius >= 0)
    
    val output = DoubleArray(grid.values.size)
    
    for (y in 0 until grid.height) {
    for (x in 0 until grid.width) {
    
         var sum = 0.0
     var count = 0

     for (dy in -radius..radius) {
         for (dx in -radius..radius) {

             val nx = x + dx
             val ny = y + dy

             if (nx in 0 until grid.width &&
                 ny in 0 until grid.height
             ) {
                 val value = grid[nx, ny]

                 if (value.isFinite()) {
                     sum += value
                     count++
                 }
             }
         }
     }

     output[y * grid.width + x] =
         if (count > 0) sum / count else Double.NaN
 }
    
    }
    
    return GridData(grid.width, grid.height, output)
    }
  
  /**
  
  * Median filter
  
  * إزالة القيم الشاذة والضوضاء.
    */
    fun medianFilter(
    grid: GridData,
    radius: Int = 1
    ): GridData {
    
    require(radius >= 0)
    
    val output = DoubleArray(grid.values.size)
    
    for (y in 0 until grid.height) {
    for (x in 0 until grid.width) {
    
         val samples = mutableListOf<Double>()

     for (dy in -radius..radius) {
         for (dx in -radius..radius) {

             val nx = x + dx
             val ny = y + dy

             if (nx in 0 until grid.width &&
                 ny in 0 until grid.height
             ) {
                 val value = grid[nx, ny]

                 if (value.isFinite()) {
                     samples.add(value)
                 }
             }
         }
     }

     samples.sort()

     output[y * grid.width + x] =
         if (samples.isEmpty()) {
             Double.NaN
         } else {
             samples[samples.size / 2]
         }
 }
    
    }
    
    return GridData(grid.width, grid.height, output)
    }
  
  /**
  
  * High-pass filter
  
  * إبراز التغيرات المحلية والشذوذ المكاني.
    */
    fun highPassFilter(grid: GridData): GridData {
    
    val smooth = meanFilter(grid, 1)
    val output = DoubleArray(grid.values.size)
    
    for (i in grid.values.indices) {
    
     val original = grid.values[i]
 val filtered = smooth.values[i]

 output[i] =
     if (original.isFinite() && filtered.isFinite()) {
         original - filtered
     } else {
         Double.NaN
     }
    
    }
    
    return GridData(grid.width, grid.height, output)
    }
  
  /**
  
  * Grid gradient
  
  * حساب التغير المكاني في الاتجاهين X و Y.
    */
    fun gradient(grid: GridData): GridData {
    
    val output = DoubleArray(grid.values.size)
    
    for (y in 0 until grid.height) {
    for (x in 0 until grid.width) {
    
         val left = grid[x.coerceAtLeast(1) - 1, y]
     val right = grid[(x + 1).coerceAtMost(grid.width - 1), y]

     val top = grid[x, y.coerceAtLeast(1) - 1]
     val bottom = grid[x, (y + 1).coerceAtMost(grid.height - 1)]

     output[y * grid.width + x] =
         if (
             left.isFinite() &&
             right.isFinite() &&
             top.isFinite() &&
             bottom.isFinite()
         ) {
             sqrt(
                 abs(right - left).let { it * it } +
                 abs(bottom - top).let { it * it }
             )
         } else {
             Double.NaN
         }
 }
    
    }
    
    return GridData(grid.width, grid.height, output)
    }
  
  /**
  
  * Normalize grid values between 0 and 1.
    */
    fun normalize(grid: GridData): GridData {
    
    val valid = grid.values.filter { it.isFinite() }
    
    if (valid.isEmpty()) {
    return GridData(
    grid.width,
    grid.height,
    DoubleArray(grid.values.size) { Double.NaN }
    )
    }
    
    val min = valid.minOrNull()!!
    val max = valid.maxOrNull()!!
    
    val output = grid.values.map { value ->
    
     when {
     !value.isFinite() -> Double.NaN
     max == min -> 0.5
     else -> (value - min) / (max - min)
 }
    
    }.toDoubleArray()
    
    return GridData(grid.width, grid.height, output)
    }
  
  /**
  
  * Detect local anomalies.
  
  * استخراج القيم الأعلى من عتبة محددة.
    */
    fun detectAnomalies(
    grid: GridData,
    threshold: Double = 0.8
    ): List<Pair<Int, Int>> {
    
    require(threshold in 0.0..1.0)
    
    val normalized = normalize(grid)
    val anomalies = mutableListOf<Pair<Int, Int>>()
    
    for (y in 0 until grid.height) {
    for (x in 0 until grid.width) {
    
         val value = normalized[x, y]

     if (value.isFinite() && value >= threshold) {
         anomalies.add(x to y)
     }
 }
    
    }
    
    return anomalies
    }
  }
