/*
 * Copyright 2016 Priyesh Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.priyesh.chroma

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.os.Build
import android.support.annotation.ColorInt
import android.support.v4.graphics.ColorUtils
import android.support.v7.graphics.Palette
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import me.priyesh.chroma.internal.ChannelView

class ChromaView : RelativeLayout {

  companion object {
    val DefaultColor = Color.GRAY
    val DefaultModel = ColorMode.RGB
  }

  @ColorInt var currentColor: Int private set

  val colorMode: ColorMode
  private var channelViews: List<ChannelView>? = null
  private var hexView: EditText? = null

  var minSliderAlpha = 20
  var minButtonDarkness = 30F

  private var updateClickHandler: ((Palette.Swatch) -> Unit)? = null

  constructor(context: Context) : this(DefaultColor, DefaultModel, context)

  constructor(@ColorInt initialColor: Int, colorMode: ColorMode, context: Context) : super(context) {
      this.currentColor = initialColor
      this.colorMode = colorMode
      init()
  }

  private fun init() {
    inflate(context, R.layout.chroma_view, this)
    clipToPadding = false

    channelViews = colorMode.channels.map { ChannelView(it, currentColor, context) }
    hexView = findViewById(R.id.hex_view)

     applyColor()

    val seekbarChangeListener: () -> Unit = {
      currentColor = colorMode.evaluateColor(channelViews!!.map { it.channel })
      applyColor()
    }

    val channelContainer = findViewById<ViewGroup>(R.id.channel_container)
    channelViews!!.forEach {
      channelContainer.addView(it)

      val layoutParams = it.layoutParams as LinearLayout.LayoutParams
      layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.channel_view_margin_top)
      layoutParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.channel_view_margin_bottom)

      it.registerListener(seekbarChangeListener)
    }
    if (colorMode == ColorMode.ARGB) {
      hexView?.layoutParams?.width = resources.getDimensionPixelSize(R.dimen.hex_view_width_argb)
    }
    hexView?.filters = arrayOf(InputFilter.LengthFilter(colorMode.hexLength + 1), InputFilter.AllCaps(),
            InputFilter { source, start, end, dest, dstart, dend ->
               val filtered = source.filterIndexed { index, c ->
                val idx = dstart + index
                (c == '#' && idx == 0 && !dest.contains('#')) || c in "0123456789ABCDEF"
              }
              if (dstart == 0 && filtered.getOrNull(0) != '#' && !dest.removeRange(dstart until dend).contains('#')) {
                filtered.padStart(1, '#')
              } else filtered
            })
    hexView?.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        val str = s.toString()
        if(!TextUtils.isEmpty(str)) {
            try {
              currentColor = Color.parseColor(str)
              fromHex = true
              applyColor()
              fromHex = false
              channelViews?.forEach {
                it.setByColor(currentColor)
              }
            } catch (ignored: Exception) {}
        }
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
  }

 // TODO remove all these messy state handling hacks
  var fromHex = false

  private fun adjustBrightness(color: Int): Int {
    return if (luminance(color) < minButtonDarkness / 100) adjustBrightness(manipulateColor(color, .9F)) else color
  }

  fun applyColor() {
    findViewById<View>(R.id.color_view).setBackgroundColor(currentColor)
    with(findViewById<View>(R.id.button_bar)) {
      var buttonColor = Color.argb(255, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor))
      buttonColor = adjustBrightness(changeSaturation(buttonColor, Color.alpha(currentColor) / 255F))
      findViewById<Button>(R.id.positive_button).setTextColor(buttonColor)
      findViewById<Button>(R.id.negative_button).setTextColor(buttonColor)
    }
    channelViews?.forEach {
      val channelColor = if (Color.alpha(currentColor) < minSliderAlpha)
        Color.argb(minSliderAlpha, Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor))
      else currentColor
      it.applyColor(channelColor)
    }

    val swatch = Palette.Swatch(ColorUtils.compositeColors(currentColor, Color.WHITE), 1)
    hexView?.apply {
      if (!fromHex) { text = "#${colorMode.toHex(colorMode.evaluateColor(colorMode.channels))}".toEditable() }
      setTextColor(swatch.bodyTextColor)
      highlightColor = swatch.titleTextColor
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        backgroundTintList = ColorStateList.valueOf(swatch.titleTextColor)
      }
    }
    updateClickHandler?.invoke(swatch)
  }

  interface ButtonBarListener {
    fun onPositiveButtonClick(color: Int)
    fun onNegativeButtonClick()
  }

  interface PreviewClickListener {
    fun onClick(color: Int)
  }

  fun enableButtonBar(listener: ButtonBarListener?) {
    with(findViewById<View>(R.id.button_bar)) {
      val positiveButton = findViewById<View>(R.id.positive_button)
      val negativeButton = findViewById<View>(R.id.negative_button)

      if (listener != null) {
        visibility = VISIBLE
        positiveButton.setOnClickListener { listener.onPositiveButtonClick(currentColor) }
        negativeButton.setOnClickListener { listener.onNegativeButtonClick() }
      } else {
        visibility = GONE
        positiveButton.setOnClickListener(null)
        negativeButton.setOnClickListener(null)
      }
    }
  }

  fun enablePreviewClick(listener: ChromaView.PreviewClickListener) {
    with(findViewById<View>(R.id.click_handler)) {
      setOnClickListener { listener.onClick(currentColor) }
      var color = Color.TRANSPARENT
      updateClickHandler = {
        if (it.bodyTextColor != color) {
          color = it.bodyTextColor
          val pressedColor = ColorStateList.valueOf(color)
          val rippleColor = getRippleColor(color)
          background = RippleDrawable(
                  pressedColor,
                  null,
                  rippleColor
          )
        }
      }
      applyColor()
    }
  }

  private fun getRippleColor(color: Int): Drawable {
    return ShapeDrawable().apply { paint.color = color }
  }
}