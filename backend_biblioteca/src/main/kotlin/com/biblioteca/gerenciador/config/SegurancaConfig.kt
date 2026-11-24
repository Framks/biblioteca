package com.biblioteca.gerenciador.config

import com.biblioteca.gerenciador.security.JwtAutenticacaoFiltro
import com.biblioteca.gerenciador.security.RespostaErroSeguranca
import com.biblioteca.gerenciador.security.TokenService
import com.biblioteca.gerenciador.services.Relogio
import com.biblioteca.gerenciador.services.SessaoService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Regras gerais de acesso. As permissões de cada endpoint ficam nos controllers (@PreAuthorize),
 * com os códigos da seção 7 (ex.: hasAuthority('book_create')).
 */
@Configuration
@EnableMethodSecurity
class SegurancaConfig {

    @Bean
    fun filterChain(
        http: HttpSecurity,
        tokenService: TokenService,
        sessaoService: SessaoService,
        relogio: Relogio,
        respostas: RespostaErroSeguranca,
    ): SecurityFilterChain {
        http
            // Sem sessão no servidor nem cookie de autenticação: o access token vai no header Authorization,
            // então não há CSRF a explorar. O refresh_token (cookie) é SameSite=Strict.
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { it.frameOptions { frame -> frame.sameOrigin() } } // console do H2 (perfil dev)
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**", "/api/v1/genres").permitAll() // RF05
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll() // /h2-console (só existe no perfil dev) e /error
            }
            .exceptionHandling {
                it.authenticationEntryPoint(respostas)
                it.accessDeniedHandler(respostas)
            }
            // Instanciado aqui (e não como @Component) para não ser registrado duas vezes como filtro do servlet.
            .addFilterBefore(
                JwtAutenticacaoFiltro(tokenService, sessaoService, relogio, respostas),
                UsernamePasswordAuthenticationFilter::class.java,
            )
        return http.build()
    }
}
