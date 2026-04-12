package org.ipredencao.ipredencao_manager.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.ZoneId;

public class TimezoneContext {

    public static final String HEADER = "X-Timezone";
    private static final ZoneId DEFAULT = ZoneId.of("America/Sao_Paulo");

    public static ZoneId getZoneId() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String tz = attrs.getRequest().getHeader(HEADER);
                if (tz != null) return ZoneId.of(tz);
            }
        } catch (Exception e) {
            // Header invalido ou fora de contexto web (testes, jobs)
        }
        return DEFAULT;
    }

    public static LocalDate now() {
        return LocalDate.now(getZoneId());
    }
}
