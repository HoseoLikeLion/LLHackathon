package com.hackathon.skinroutine.common;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 날짜 판정은 전부 KST(Asia/Seoul) 기준.
 * 배포 서버(Render)는 UTC라서 LocalDate.now()를 그냥 쓰면 저녁 시간대에 "오늘"이 어긋난다 —
 * "사용자당 하루 1기록" 판정은 반드시 이 클래스를 거칠 것.
 */
public final class KoreaTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    private KoreaTime() {}
}
