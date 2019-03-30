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

package me.priyesh.chroma.internal

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.support.annotation.ColorInt
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import me.priyesh.chroma.R
import me.priyesh.chroma.ColorMode
import me.priyesh.chroma.toEditable
import kotlin.math.max
import kotlin.math.min

internal class ChannelView(
    val channel: ColorMode.Channel,
    @ColorInt color: Int,
    context: Context) : RelativeLayout(context) {

  internal var listener: (() -> Unit)? = null

  init {
    channel.progress = channel.extractor.invoke(color)

    if (channel.progress < channel.min || channel.progress > channel.max) {
      throw IllegalArgumentException("Initial progress for channel: ${channel.javaClass.simpleName}"
          + " must be between ${channel.min} and ${channel.max}.")
    }

    val rootView = inflate(context, R.layout.channel_row, this)
    bindViews(rootView)
  }

  private fun bindViews(root: View) {
    (root.findViewById(R.id.label) as TextView).text = context.getString(channel.nameResourceId)

    val progressView = root.findViewById(R.id.progress_text) as EditText
    val seekbar = root.findViewById(R.id.seekbar) as SeekBar

    progressView.text = channel.progress.toEditable()
    progressView.addTextChangedListener(object : TextWatcher {
      override fun afterTextChanged(s: Editable?) {
        if (silenceListener) return
        var start = progressView.selectionStart
        var end = progressView.selectionEnd

        val originalProgress = s.toString().toIntOrNull()
        var progress = originalProgress ?: {
          start = 1
          end = 1
          0
        }.invoke()
        progress = min(channel.max, max(channel.min, progress))

        channel.progress = progress
        seekbar.progress = progress
        if (progress != originalProgress) {
          progressView.text = progress.toEditable()
        }

        listener?.invoke()

        val len = progress.toString().length
        progressView.setSelection(min(start, len), min(end, len))
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    seekbar.max = channel.max
    seekbar.progress = channel.progress
    seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onStartTrackingTouch(seekbar: SeekBar?) { }

      override fun onStopTrackingTouch(seekbar: SeekBar?) { }

      override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (!silenceListener) channel.progress = progress
        progressView.text = progress.toEditable()
        if (!silenceListener) listener?.invoke()
      }
    })
  }
  // TODO remove all these messy state handling hacks
  var silenceListener: Boolean = false
  fun setByColor(color: Int) {
    silenceListener = true
    findViewById<SeekBar>(R.id.seekbar).progress = channel.toProgress.invoke(color)
    silenceListener = false
  }

  fun applyColor(color: Int) {
    val stateList = ColorStateList.valueOf(channel.seperate(color))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      findViewById<SeekBar>(R.id.seekbar).apply {
        thumbTintList = stateList
        progressTintList = stateList
        progressBackgroundTintList = stateList
      }
    }
    findViewById<EditText>(R.id.progress_text).apply {
      highlightColor = color
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        backgroundTintList = stateList
      }
    }
  }

  fun registerListener(listener: () -> Unit) {
    this.listener = listener
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    this.listener = null
  }
}