package cn.sgnxotsmicf.common.tools;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


public class TimeConverter {
    
    /**
     * Double 毫秒时间戳转 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Double score) {
        if (score == null) return null;
        return Instant.ofEpochMilli(score.longValue())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }
    
    /**
     * LocalDateTime 转毫秒时间戳（存入Redis用）
     */
    public static double toTimestamp(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }
    
    /**
     * 当前时间转时间戳（更新会话用）
     */
    public static double nowTimestamp() {
        return System.currentTimeMillis();
    }
}