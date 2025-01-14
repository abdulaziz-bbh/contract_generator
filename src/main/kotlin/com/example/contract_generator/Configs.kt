package com.example.contract_generator

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import java.util.*

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    @Bean
    fun localeResolver() = SessionLocaleResolver().apply { setDefaultLocale(Locale("uz")) }

    @Bean
    fun errorMessageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setBasename("errors")
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtFilter,
    private val authenticationProvider: AuthenticationProvider,
    private val accessDeniedHandler: CustomAccessDeniedHandler,
    private val authenticationEntryPoint: CustomAuthenticationEntryPoint
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
       http
           .csrf{it.disable()}
           .authorizeHttpRequests{
               auth -> auth
               .requestMatchers(
                   "api/v1/auth/**").permitAll()
               .requestMatchers("/api/v1/templates/**").hasAnyRole(Role.DIRECTOR.name, Role.OPERATOR.name)
               .requestMatchers("/api/v1/user/**").hasAnyRole(Role.DIRECTOR.name)
               .anyRequest().authenticated()
           }
           .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
           .authenticationProvider(authenticationProvider)
           .sessionManagement{
               it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
           }
           .exceptionHandling{
               it.accessDeniedHandler(accessDeniedHandler)
               it.authenticationEntryPoint(authenticationEntryPoint)
           }
        return http.build()
    }
}

@Component
class JobRunning(private val jobService: JobService,
    private val jobRepository: JobRepository,){
    @Scheduled(fixedRate = 3_000)
    fun schedulePendingJobs() {
        val pendingJobs = jobRepository.findAllByStatus(JobStatus.PENDING)
        pendingJobs.forEach { job ->
            jobService.generateZip(job)
        }
    }
}

@Configuration
class AuthConfig(
    private val userRepository: UserRepository
) {

    @Bean
    fun userDetailsService(): UserDetailsService = CustomUserDetailsService(userRepository)

    @Bean
    fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager {
        return configuration.authenticationManager
    }

    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val daoAuthenticationProvider = DaoAuthenticationProvider()
        daoAuthenticationProvider.setUserDetailsService(userDetailsService())
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder())
        return daoAuthenticationProvider
    }

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()
}

@Configuration
@EnableJpaAuditing
class AppConfiguration : AuditorAware<Long> {

    override fun getCurrentAuditor(): Optional<Long> {
        return Optional.ofNullable(getCurrentUserId()?.id)
    }
}

@Component
class CustomAccessDeniedHandler(
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        accessDeniedException: AccessDeniedException?
    ) {
        response?.contentType = MediaType.APPLICATION_JSON_VALUE
        response?.status = HttpStatus.FORBIDDEN.value()
        this.resolver.resolveException(request!!, response!!, null, accessDeniedException!!)
    }
}

@Component
class CustomAuthenticationEntryPoint(
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        authException: AuthenticationException?
    ) {
        response?.status = HttpStatus.UNAUTHORIZED.value()
        response?.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer Token")
        this.resolver.resolveException(request!!, response!!, null, authException!!)
    }
}