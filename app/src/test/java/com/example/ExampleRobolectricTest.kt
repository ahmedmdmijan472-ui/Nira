package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.WaveformStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NIRA", appName)
  }

  @Test
  fun `waveform styles are properly defined`() {
    val styles = WaveformStyle.values()
    assertEquals(3, styles.size)
    assertTrue(styles.contains(WaveformStyle.FLUID_WAVE))
    assertTrue(styles.contains(WaveformStyle.STUDIO_BARS))
    assertTrue(styles.contains(WaveformStyle.PULSE_SPECTRUM))
  }
}
