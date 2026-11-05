package com.stockpilot.core.port.out;

import java.time.Duration;

public record AccessToken(String value, Duration validity) {
}
