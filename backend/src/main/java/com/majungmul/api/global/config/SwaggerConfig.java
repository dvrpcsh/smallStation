package com.majungmul.api.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(SpringDoc OpenAPI 3) 설정.
 *
 * <p>UI 접근: http://localhost:8080/swagger-ui/index.html
 *
 * <p>Bearer 토큰 인증 방법:
 * <ol>
 *   <li>POST /api/v1/auth/anonymous 호출 → accessToken 획득</li>
 *   <li>우측 상단 "Authorize" 버튼 클릭</li>
 *   <li>Value 입력란에 accessToken 값만 입력 (Bearer 접두사 불필요)</li>
 *   <li>이후 모든 API 요청에 Authorization: Bearer {token} 자동 포함</li>
 * </ol>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT 인증 스킴 이름
        String bearerAuth = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("간이역 (마중물) API")
                        .description("""
                                고립·은둔 청년의 정서적 회복을 돕는 심리적 안전지대 플랫폼 API 문서.

                                **인증 방법**: 익명 로그인(/api/v1/auth/anonymous)으로 토큰을 발급받은 뒤,
                                우측 상단 Authorize 버튼에 accessToken을 입력하세요.
                                """)
                        .version("v1.0.0"))
                // 전역 Bearer 토큰 인증 요구 선언
                .addSecurityItem(new SecurityRequirement().addList(bearerAuth))
                .components(new Components()
                        .addSecuritySchemes(bearerAuth,
                                new SecurityScheme()
                                        .name(bearerAuth)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 액세스 토큰을 입력하세요 ('Bearer ' 접두사 제외)")
                        ));
    }
}
