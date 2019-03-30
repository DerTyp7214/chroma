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

import android.graphics.Color

enum class ColorMode {

  ARGB {
    override val hexLength = 8
    override val channels: List<Channel> = listOf(
        Channel(R.string.channel_alpha, 0, 255, Color::alpha, {color -> Color.argb(Color.alpha(color), 0, 0, 0)}),
        Channel(R.string.channel_red, 0, 255, Color::red, {color -> Color.rgb( Color.red(color), 0, 0)}),
        Channel(R.string.channel_green, 0, 255, Color::green, {color -> Color.rgb( 0, Color.green(color), 0)}),
        Channel(R.string.channel_blue, 0, 255, Color::blue, {color -> Color.rgb(0, 0, Color.blue(color))})
    )

    override fun evaluateColor(channels: List<Channel>): Int = Color.argb(
        channels[0].progress, channels[1].progress, channels[2].progress, channels[3].progress)

    override fun toHex(color: Int): String = String.format("%08X", color).toUpperCase()
  },

  RGB {
    override val hexLength = 6
    override val channels: List<Channel> = ARGB.channels.drop(1)

    override fun evaluateColor(channels: List<Channel>): Int = Color.rgb(
        channels[0].progress, channels[1].progress, channels[2].progress)

    override fun toHex(color: Int): String = String.format("%06X", (0xFFFFFF and color)).toUpperCase()
  },

  HSV {
    override val hexLength = 6
    override val channels: List<Channel> = listOf(
        Channel(R.string.channel_hue, 0, 350, ::hue, {color -> Color.HSVToColor(floatArrayOf(hue(color).toFloat(), 100f, 100f))}),
        Channel(R.string.channel_saturation, 0, 100, ::saturation, {color -> Color.HSVToColor(floatArrayOf(hue(color).toFloat(), saturation(color).toFloat(), 100f))}, { color -> saturation(color) * 100 }),
        Channel(R.string.channel_value, 0, 100, ::value, {color -> Color.HSVToColor(floatArrayOf(hue(color).toFloat(), 100f, value(color).toFloat()))}, { color -> value(color) * 100 })
    )

    override fun evaluateColor(channels: List<Channel>): Int = Color.HSVToColor(
        floatArrayOf(
            (channels[0].progress).toFloat(),
            (channels[1].progress / 100.0).toFloat(),
            (channels[2].progress / 100.0).toFloat()
        ))

    override fun toHex(color: Int): String = RGB.toHex(color)
  };

  internal abstract val hexLength: Int

  internal abstract val channels: List<Channel>

  internal abstract fun evaluateColor(channels: List<Channel>): Int

  internal abstract fun toHex(color: Int): String

  internal data class Channel(val nameResourceId: Int,
                              val min: Int, val max: Int,
                              val extractor: (color: Int) -> Int,
                              val seperate: (color: Int) -> Int,
                              val toProgress: (color: Int) -> Int = { color -> extractor.invoke(color) },
                              var progress: Int = 0)

  companion object {
    @JvmStatic fun fromName(name: String) = values().find { it.name == name } ?: ColorMode.RGB
  }
}