package com.raxa.domain.invite;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InviteCodeGenerator {

    public String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toLowerCase();
    }
}
