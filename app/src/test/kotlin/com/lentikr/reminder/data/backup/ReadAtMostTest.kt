package com.lentikr.reminder.data.backup

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadAtMostTest {

  private fun bytes(size: Int) = ByteArray(size) { (it % 251).toByte() }

  @Test
  fun readsEverythingUpToTheLimit() {
    val data = bytes(20_000) // Several buffer-sized reads.
    assertArrayEquals(data, ByteArrayInputStream(data).readAtMost(20_000))
  }

  @Test
  fun emptyStream() {
    assertEquals(0, ByteArrayInputStream(ByteArray(0)).readAtMost(10).size)
  }

  @Test(expected = BackupFormatException::class)
  fun oneByteOverTheLimitFails() {
    ByteArrayInputStream(bytes(20_001)).readAtMost(20_000)
  }
}
