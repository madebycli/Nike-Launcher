// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import org.junit.Assert.*
import org.junit.Test

class MediaProgressMathTest {
    @Test fun `progress handles unknown metadata speed and bounds`() {
        assertNull(mediaProgress(-1, 1000, 0, 1f))
        assertNull(mediaProgress(0, 0, 0, 1f))
        assertNull(mediaProgress(0, 1000, 0, Float.NaN))
        assertEquals(.5f, mediaProgress(300, 1000, 100, 2f)!!, .001f)
        assertEquals(1f, mediaProgress(900, 1000, 1000, 1f)!!, .001f)
        assertEquals(.3f, mediaProgress(300, 1000, 1000, 0f)!!, .001f)
    }
}
