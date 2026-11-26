
package com.example.agent;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class DateTimeTools {

    @Tool(description = """
            Get the current date and time in a specific time zone.
            Use this tool when answering questions involving today,
            tomorrow, next week, next month, or other relative dates.
            """)
    public String getCurrentDateTime(
            @ToolParam(description = """
                    Time zone ID, for example:
                    America/Los_Angeles, Europe/Paris, Asia/Kolkata, UTC
                    """)
            String timeZone) {

        try {
            return ZonedDateTime.now(ZoneId.of(timeZone))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            return "Invalid time zone: " + timeZone;
        }
    }
}
