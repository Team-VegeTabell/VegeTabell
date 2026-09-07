package com.example.VegeTabell.app.dto;

import java.time.Duration;
import java.time.Instant;

/**
 * 引取期限までの残り時間を買い手向け表示用の文言に変換する。
 * 「6時間未満＝もうすぐ期限切れ」はワイヤーフレーム（product-detail.png）の
 * 警告バナー例に合わせたMVP暫定のしきい値。
 */
final class ExpiryPresenter {

    private static final long URGENT_THRESHOLD_HOURS = 6;

    private ExpiryPresenter() {
    }

    static String remainingLabel(Instant expiryAt, Instant now) {
        Duration remaining = Duration.between(now, expiryAt);
        if (remaining.isNegative() || remaining.isZero()) {
            return "期限切れ";
        }
        if (remaining.toHours() < 1) {
            return "あと" + Math.max(1, remaining.toMinutes()) + "分";
        }
        if (remaining.toHours() < 24) {
            return "あと" + remaining.toHours() + "時間";
        }
        return "あと" + remaining.toDays() + "日";
    }

    static boolean isUrgent(Instant expiryAt, Instant now) {
        Duration remaining = Duration.between(now, expiryAt);
        return !remaining.isNegative() && remaining.toHours() < URGENT_THRESHOLD_HOURS;
    }
}
