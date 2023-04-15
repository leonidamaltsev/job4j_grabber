package ru.job4j.grabber.utils;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HabrCareerDateTimeParserTest {

    @Test
    public void parseWithoutTimeZonesCompleted() {
        String date = "2023-02-14T17:59:14+03:00";
        LocalDateTime result = new HabrCareerDateTimeParser().parse(date);
        LocalDateTime expected = LocalDateTime.parse("023-02-14T17:59:14");
        assertThat(expected).isEqualTo(result);
    }
}