package com.installfinish.common.error;

import com.installfinish.common.web.RequestIdFilter;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/**
 * Builds the standard InstallFinish error body: an RFC 9457 problem detail extended with a stable machine-readable
 * {@code code}, the {@code requestId} for support correlation, and optional field-level {@code errors}.
 */
public final class ApiProblems {

    private static final String TYPE_PREFIX = "urn:installfinish:problem:";

    private ApiProblems() {
    }

    public static ProblemDetail create(HttpStatusCode status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        return decorate(problem, code);
    }

    public static ProblemDetail withFieldErrors(ProblemDetail problem, List<FieldViolation> errors) {
        problem.setProperty("errors", errors);
        return problem;
    }

    /** Applies the standard type, code and request id to a problem produced by Spring. */
    public static ProblemDetail decorate(ProblemDetail problem, String code) {
        problem.setType(URI.create(TYPE_PREFIX + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", code);
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }

    public record FieldViolation(String field, String message) {
    }
}
