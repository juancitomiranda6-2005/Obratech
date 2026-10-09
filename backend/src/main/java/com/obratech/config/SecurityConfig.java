// SecurityConfig
package com.obratech.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.obratech.security.CustomAccessDeniedHandler;
import com.obratech.security.CustomAuthenticationSuccessHandler;
import com.obratech.security.JwtAuthFilter;
import com.obratech.security.OAuth2LoginSuccessHandler;
import com.obratech.security.UserDetailsServiceImpl;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider(UserDetailsServiceImpl userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
                .authenticationProvider(authenticationProvider)

                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/", "/index.html", "/assets/**", "/favicon.ico",
                                "/login", "/registro", "/recuperar-contrasena", "/restablecer-contrasena", "/oauth/callback",
                                "/css/**", "/js/**", "/img/**", "/styles/**", "/error",
                                "/oauth2/**", "/login/oauth2/**", "/completar-registro-oauth2")
                        .permitAll()

                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/client/projects/*/applications/**")
                        .hasAnyAuthority("ROLE_CLIENT", "ROLE_ADMIN")
                        .requestMatchers("/api/directory/contractors", "/api/directory/workers")
                        .hasAnyAuthority("ROLE_CLIENT", "ROLE_CONTRACTOR", "ROLE_ADMIN")
                        .requestMatchers("/api/directory/clients").hasAnyAuthority("ROLE_CONTRACTOR", "ROLE_ADMIN")
                        .requestMatchers("/api/client/**").hasAuthority("ROLE_CLIENT")
                        .requestMatchers("/api/contractor/**").hasAuthority("ROLE_CONTRACTOR")
                        .requestMatchers("/api/worker/**").hasAuthority("ROLE_WORKER")
                        .requestMatchers("/api/participant/**").hasAnyAuthority("ROLE_WORKER", "ROLE_CONTRACTOR")
                        .requestMatchers("/api/dashboard/cliente").hasAuthority("ROLE_CLIENT")
                        .requestMatchers("/api/dashboard/contratista").hasAuthority("ROLE_CONTRACTOR")
                        .requestMatchers("/api/dashboard/trabajador/**").hasAuthority("ROLE_WORKER")

                        .requestMatchers("/api/**").authenticated()

                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/proyectos/documento/**", "/trabajadores/cv/**", "/uploads/**", "/actuator/**")
                        .authenticated()

                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/perfil-cliente/**", "/mis-proyectos/**")
                        .hasAuthority("ROLE_CLIENT")

                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/desboard-contratista", "/perfil-contratista/**", "/contratista-proyectos/**")
                        .hasAuthority("ROLE_CONTRACTOR")

                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/desboard-trabajador", "/perfil-laboral/**")
                        .hasAuthority("ROLE_WORKER")

                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/admin/**")
                        .hasAuthority("ROLE_ADMIN")

                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/**").permitAll()

                        .anyRequest().authenticated()

                )

                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                                request -> request.getServletPath().startsWith("/api/"))
                                                .accessDeniedHandler((request, response, exception) -> {
                                                        if (request.getServletPath().startsWith("/api/")) {
                                                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                                response.setContentType("application/json");
                                                                response.setCharacterEncoding("UTF-8");
                                                                response.getWriter().write("{\"error\":\"Acceso denegado\"}");
                                                        } else {
                                                                accessDeniedHandler.handle(request, response, exception);
                                                        }
                                                }))

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .failureUrl("/login?error=true")
                        .successHandler(authenticationSuccessHandler)
                        .permitAll())

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oauth2LoginSuccessHandler))

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("OBRATECH_SESSION")
                        .permitAll())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .expiredUrl("/login?expired=true"));

        return http.build();
    }
    
}
