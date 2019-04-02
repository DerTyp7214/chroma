package me.priyesh.chroma

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.util.DisplayMetrics
import android.view.WindowManager

fun screenDimensions(context: Context): DisplayMetrics {
  val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  val metrics = DisplayMetrics()
  manager.defaultDisplay.getMetrics(metrics)
  return metrics
}

fun orientation(context: Context) = context.resources.configuration.orientation

infix fun Int.percentOf(n: Int): Int = (n * (this / 100.0)).toInt()

fun hue(color: Int): Int = hsv(color, 0)
fun saturation(color: Int): Int = hsv(color, 1, 100)
fun value(color: Int): Int = hsv(color, 2, 100)

fun changeSaturation(color: Int, saturation: Float): Int {
  if (color == Color.WHITE) return color
  val hsv = FloatArray(3)
  Color.colorToHSV(color, hsv)
  hsv[1] = saturation
  return Color.HSVToColor(hsv)
}

private fun hsv(color: Int, index: Int, multiplier: Int = 1): Int {
  val hsv = FloatArray(3)
  Color.colorToHSV(color, hsv)
  return (hsv[index] * multiplier).toInt()
}

fun luminance(color: Int): Float {
  return (1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255).toFloat()
}

fun manipulateColor(color: Int, factor: Float): Int {
  val a = Color.alpha(color)
  val r = Math.round(Color.red(color) * factor)
  val g = Math.round(Color.green(color) * factor)
  val b = Math.round(Color.blue(color) * factor)
  return Color.argb(a,
          Math.min(r, 255),
          Math.min(g, 255),
          Math.min(b, 255))
}

fun Any.toEditable(): Editable = Editable.Factory.getInstance().newEditable(toString())