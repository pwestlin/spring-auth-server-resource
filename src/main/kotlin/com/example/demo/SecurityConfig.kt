package com.example.demo

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import java.util.*

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun authServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)
        return http.build()
    }

    @Bean
    fun resourceServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests {
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer {
                //it.jwt(Customizer.withDefaults())
                it.jwt(Customizer.withDefaults())
                it.jwt { jwt ->
                                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                                }
            }
        return http.build()
    }

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
            val converter = JwtAuthenticationConverter()

            // Anpassa hur roller extraheras från JWT claims
            val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
            grantedAuthoritiesConverter.setAuthoritiesClaimName("scope")
            grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_")

            converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)

            return converter
        }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

/*
    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
        val user = User.builder()
            .username("user")
            .password(passwordEncoder.encode("password"))
            .roles("USER")
            .build()
        val admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("password"))
            .roles("USER", "ADMIN")
            .build()
        return InMemoryUserDetailsManager(user, admin)
    }
*/

    /*
    Spring Authorization server har ingen inbyggd funktionalitet för att läsa klientregistreringar från application.yml
    men det går att lösa mha egen konfiguration i application.yml samt @ConfigurationProperties och @EnableConfigurationProperties
    och sen injecta dessa properties in i funktionen nedan och skapa RegisteredClient(s) uti från dessa.
     */
    @Bean
    fun registeredClientRepository(passwordEncoder: PasswordEncoder): InMemoryRegisteredClientRepository {
        val registeredUserClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("user")
            .clientSecret(passwordEncoder.encode("secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scopes {
                it.add("read")
            }
            .build()

        val registeredAdminClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("admin")
            .clientSecret(passwordEncoder.encode("secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scopes {
                it.add("read")
                it.add("write")
            }
            .build()
        // TODO pevest: Flera klienter med olika scopes
        // TODO pevest: Vad händer om client user skickar med scopes för både read och write?
        return InMemoryRegisteredClientRepository(registeredUserClient, registeredAdminClient)
    }
}
