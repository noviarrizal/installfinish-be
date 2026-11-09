package com.installfinish.support;

import com.installfinish.client.ClientAccountRepository;
import com.installfinish.client.ClientUserRepository;
import com.installfinish.security.JwtActorConverter;
import com.installfinish.security.SecurityConfig;
import com.installfinish.security.SecurityProblemHandler;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Companion to {@code @WebMvcTest}: loads the real security configuration and JWT validation so controller tests
 * exercise authentication, authorization, validation and error rendering exactly as deployed. Repositories used by
 * the JWT converter are mocked; unless stubbed, every token subject resolves to a staff actor.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@Import({SecurityConfig.class, SecurityProblemHandler.class, JwtActorConverter.class, TestJwtConfig.class})
@MockitoBean(types = {ClientUserRepository.class, ClientAccountRepository.class})
public @interface ApiSliceTest {
}
