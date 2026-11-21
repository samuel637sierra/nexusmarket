package application.configuration;

import application.domain.models.User;
import application.domain.ports.out.PasswordServicePort;
import application.domain.ports.out.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    UserDetailsService userDetailsService(UserRepositoryPort users) {
        return username -> users.findByUsername(username)
                .map(SecurityConfiguration::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña no válidos"));
    }

    @Bean
    PasswordEncoder passwordEncoder(PasswordServicePort passwords) {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return passwords.hash(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return passwords.matches(rawPassword.toString(), encodedPassword);
            }
        };
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                        PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            DaoAuthenticationProvider authenticationProvider) throws Exception {
        http.authenticationProvider(authenticationProvider)
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/index.html", "/assets/**").permitAll()
                        .requestMatchers("/api/auth/register/**", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*",
                            "/api/products/*/availability", "/api/products/analytics").permitAll()
                        .requestMatchers("/api/products/**").hasAnyRole("SELLER", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/returns")
                            .hasAnyRole("CUSTOMER", "ADMINISTRATOR")
                        .requestMatchers("/api/customers/**", "/api/orders/**", "/api/invoices/**")
                            .hasAnyRole("CUSTOMER", "ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers("/api/warehouses/**", "/api/inventory/**")
                            .hasAnyRole("ADMINISTRATOR", "LOGISTICS_PROVIDER")
                        .requestMatchers(HttpMethod.GET, "/api/shipments/analytics")
                            .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers(HttpMethod.GET, "/api/shipments/*")
                            .hasAnyRole("CUSTOMER", "ADMINISTRATOR", "LOGISTICS_PROVIDER", "SUPERVISOR")
                        .requestMatchers("/api/shipments/**")
                            .hasAnyRole("ADMINISTRATOR", "LOGISTICS_PROVIDER", "SUPERVISOR")
                        .requestMatchers("/api/sellers/**")
                            .hasAnyRole("SELLER", "ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers("/api/admin/**")
                            .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .requestMatchers("/api/reports/**")
                            .hasAnyRole("ADMINISTRATOR", "SUPERVISOR")
                        .anyRequest().authenticated())
                .httpBasic(basic -> { });
        return http.build();
    }

    private static UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }
}