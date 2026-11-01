package com.lentikr.reminder.data

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

@Singleton
class DateProvider @Inject constructor(private val clock: Clock) {

  fun today(): LocalDate = LocalDate.now(clock)

  /** Emits today's date, and again right after each midnight while collected. */
  val todayFlow: Flow<LocalDate> =
    flow {
        while (true) {
          emit(today())
          val now = ZonedDateTime.now(clock)
          val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
          delay(Duration.between(now, nextMidnight).toMillis() + 500)
        }
      }
      .distinctUntilChanged()
}
