package com.example.VegeTabell.app.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 予約の受け取り可能時間帯を「本日16:00〜19:00まで」のような表示文言に変換する。
 */
final class PickupWindowPresenter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");

    private PickupWindowPresenter() {
    }

    static String label(Instant pickupStartAt, Instant pickupEndAt) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime start = LocalDateTime.ofInstant(pickupStartAt, zone);
        LocalDateTime end = LocalDateTime.ofInstant(pickupEndAt, zone);
        LocalDate today = LocalDate.now(zone);

        String startLabel = dayLabel(start.toLocalDate(), today) + " " + start.format(TIME_FORMAT);
        String endLabel = end.toLocalDate().equals(start.toLocalDate())
                ? end.format(TIME_FORMAT)
                : dayLabel(end.toLocalDate(), today) + " " + end.format(TIME_FORMAT);

        return startLabel + "〜" + endLabel + "まで";
    }

    private static String dayLabel(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return "本日";
        }
        if (date.equals(today.plusDays(1))) {
            return "翌日";
        }
        return date.format(DATE_FORMAT);
    }
}
