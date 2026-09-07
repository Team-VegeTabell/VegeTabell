package com.example.VegeTabell.app.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PickupWindowPresenterTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Test
    void label_sameDay_omitsEndDateLabel() {
        LocalDate today = LocalDate.now(ZONE);
        Instant start = today.atTime(LocalTime.of(16, 0)).atZone(ZONE).toInstant();
        Instant end = today.atTime(LocalTime.of(19, 0)).atZone(ZONE).toInstant();

        assertEquals("本日 16:00〜19:00まで", PickupWindowPresenter.label(start, end));
    }

    @Test
    void label_crossesMidnight_labelsEndAsNextDay() {
        LocalDate today = LocalDate.now(ZONE);
        Instant start = today.atTime(LocalTime.of(21, 46)).atZone(ZONE).toInstant();
        Instant end = today.plusDays(1).atTime(LocalTime.of(0, 35)).atZone(ZONE).toInstant();

        assertEquals("本日 21:46〜翌日 00:35まで", PickupWindowPresenter.label(start, end));
    }
}
