package com.example.demo

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import java.util.*

@Configuration
@EnableWebSecurity
// Aktiverar @PreAuthorize
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    fun authServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authorizationServerConfigurer =
            OAuth2AuthorizationServerConfigurer.authorizationServer()
        http
            .securityMatcher(authorizationServerConfigurer.endpointsMatcher)
            .with(
                authorizationServerConfigurer,
                Customizer.withDefaults()
            )
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().authenticated()
            }
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
                it.jwt(Customizer.withDefaults())
                it.jwt { jwt ->
                    // Spring konverterar inte automatiskt scopes till roller så det gör vi manuellt.
                    // Om det däremot finns en JWT claim som heter "roles" läses dessa uatomatiskt in som roller.
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
    Spring Authorization server har ingen inbyggd funktionalitet för att läsa klientregistreringar från application.yml
    men det går att lösa mha egen konfiguration i application.yml samt @ConfigurationProperties och @EnableConfigurationProperties
    och sen injecta dessa properties in i funktionen nedan och skapa RegisteredClient(s) uti från dessa.
     */
    @Bean
    fun registeredClientRepository(
        passwordEncoder: PasswordEncoder,
        authProperties: AuthProperties
    ): InMemoryRegisteredClientRepository {
        val clients = authProperties.clients.map { client ->
            RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(client.clientId)
                .clientSecret(passwordEncoder.encode(client.clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scopes {
                    client.scopes.forEach { scope -> it.add(scope) }
                }
                .build()
        }

        return InMemoryRegisteredClientRepository(clients)
    }
}

@ConfigurationProperties(prefix = "auth")
data class AuthProperties(
    val clients: List<AuthClientProperties>
)

data class AuthClientProperties(
    val clientId: String,
    val clientSecret: String,
    val scopes: List<String>
)
