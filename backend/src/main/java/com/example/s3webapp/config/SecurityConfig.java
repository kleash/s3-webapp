package com.example.s3webapp.config;

import com.example.s3webapp.security.AccessLevel;
import com.example.s3webapp.security.LdapUserDetails;
import com.example.s3webapp.security.LdapUserService;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider ldapAuthenticationProvider)
            throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.cors(cors -> {});
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        http.authenticationProvider(ldapAuthenticationProvider);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/buckets/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/buckets/*/folders/size").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/buckets/*/objects/**").hasRole("READ_WRITE")
                .requestMatchers(HttpMethod.POST, "/api/buckets/*/folders/**").hasRole("READ_WRITE")
                .requestMatchers(HttpMethod.DELETE, "/api/buckets/**").hasRole("READ_WRITE")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll());
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden")));
        return http.build();
    }

    @Bean
    public AuthenticationProvider ldapAuthenticationProvider(LdapUserService ldapUserService) {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = authentication.getCredentials() != null ? authentication.getCredentials().toString() : "";
                LdapUserDetails details = ldapUserService.authenticate(username, password);
                return new UsernamePasswordAuthenticationToken(
                        details,
                        null,
                        details.accessLevel() == AccessLevel.READ_WRITE
                                ? List.of(new SimpleGrantedAuthority("ROLE_READ_WRITE"))
                                : List.of(new SimpleGrantedAuthority("ROLE_READ_ONLY")));
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public LdapContextSource ldapContextSource(SecurityProperties securityProperties) {
        SecurityProperties.Ldap ldap = securityProperties.ldap();
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldap.url());
        contextSource.setUserDn(ldap.bindDn());
        contextSource.setPassword(ldap.bindPassword());
        contextSource.setPooled(false);
        if (ldap.ignoreSslValidation() && ldap.url() != null && ldap.url().startsWith("ldaps")) {
            contextSource.setBaseEnvironmentProperties(Map.of("java.naming.ldap.factory.socket", TrustAllSocketFactory.class.getName()));
        }
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(appProperties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    static class TrustAllSocketFactory extends javax.net.ssl.SSLSocketFactory {
        private final javax.net.ssl.SSLSocketFactory delegate;

        TrustAllSocketFactory() {
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, new TrustManager[] {new PermissiveTrustManager()}, new java.security.SecureRandom());
                delegate = ctx.getSocketFactory();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to init trust-all SSL context", e);
            }
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose)
                throws java.io.IOException {
            return delegate.createSocket(s, host, port, autoClose);
        }

        @Override
        public java.net.Socket createSocket(String host, int port) throws java.io.IOException {
            return delegate.createSocket(host, port);
        }

        @Override
        public java.net.Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort)
                throws java.io.IOException {
            return delegate.createSocket(host, port, localHost, localPort);
        }

        @Override
        public java.net.Socket createSocket(java.net.InetAddress host, int port) throws java.io.IOException {
            return delegate.createSocket(host, port);
        }

        @Override
        public java.net.Socket createSocket(
                java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort)
                throws java.io.IOException {
            return delegate.createSocket(address, port, localAddress, localPort);
        }
    }

    static class PermissiveTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    }
}
