package com.stockpilot.core.port.out;

import com.stockpilot.core.domain.User;

public interface TokenIssuer {

    AccessToken issue(User user);
}
