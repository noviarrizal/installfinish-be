package com.installfinish.security;

import com.installfinish.common.error.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes 401/403 responses in the standard problem format. The standard bearer-token handlers still set the
 * {@code WWW-Authenticate} header; token validation details are not echoed in the body.
 */
@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Pattern RESOURCE_METADATA_LAST = Pattern.compile("(,\\s*|\\s+)resource_metadata=\"[^\"]*\"$");

    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler bearerAccessDeniedHandler = new BearerTokenAccessDeniedHandler();
    private final JsonMapper jsonMapper;

    public SecurityProblemHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        bearerEntryPoint.commence(request, new WithoutResourceMetadata(response), authException);
        write(response, ApiProblems.create(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "A valid bearer token is required."));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        bearerAccessDeniedHandler.handle(request, response, accessDeniedException);
        write(response, ApiProblems.create(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to perform this action."));
    }

    private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }

    /**
     * Removes the {@code resource_metadata} parameter Spring Security 7 adds to bearer challenges. Only a parameter
     * in the <em>last</em> position is removed — where {@code BearerTokenAuthenticationEntryPoint} always puts it — so
     * no other parameter (e.g. an {@code error_description} that mentions the name) can be altered. If a future
     * Spring Security release moves it, the pointer stays in the header and the compatibility tests fail.
     */
    static String withoutResourceMetadata(String challenge) {
        return RESOURCE_METADATA_LAST.matcher(challenge).replaceFirst("");
    }

    /**
     * Applies {@link #withoutResourceMetadata} to the challenge only. The metadata document it points at is not
     * published (see {@link ProtectedResourceMetadataBlocker}); every other header passes through untouched.
     */
    private static final class WithoutResourceMetadata extends HttpServletResponseWrapper {

        WithoutResourceMetadata(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, strip(name, value));
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, strip(name, value));
        }

        private static String strip(String name, String value) {
            return HttpHeaders.WWW_AUTHENTICATE.equalsIgnoreCase(name) && value != null
                    ? withoutResourceMetadata(value)
                    : value;
        }
    }
}
