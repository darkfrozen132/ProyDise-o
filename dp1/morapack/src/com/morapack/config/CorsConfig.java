package com.morapack.config;

/**
 * Configuración de CORS para permitir acceso desde frontend
 * Preparada para ser anotada con @Configuration cuando se integre Spring Boot
 */
public class CorsConfig {
    
    /**
     * Configuración global de CORS
     * En Spring Boot sería: @Bean public WebMvcConfigurer corsConfigurer()
     */
    public void configureCors() {
        // return new WebMvcConfigurer() {
        //     @Override
        //     public void addCorsMappings(CorsRegistry registry) {
        //         registry.addMapping("/api/**")
        //                 .allowedOrigins("http://localhost:3000", "http://localhost:8080", "https://morapack-frontend.com")
        //                 .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        //                 .allowedHeaders("*")
        //                 .allowCredentials(true)
        //                 .maxAge(3600);
        //         
        //         registry.addMapping("/ws/**")
        //                 .allowedOrigins("http://localhost:3000", "http://localhost:8080", "https://morapack-frontend.com")
        //                 .allowCredentials(true);
        //     }
        // };
    }
    
    /**
     * Configuración específica para endpoints de pedidos
     */
    public static class PedidosCorsConfig {
        public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",  // React dev server
            "http://localhost:8080",  // Local frontend
            "https://morapack-frontend.com",  // Producción
            "https://dashboard.morapack.com"  // Dashboard administrativo
        };
        
        public static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        };
        
        public static final String[] ALLOWED_HEADERS = {
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-API-Version",
            "X-Client-Version"
        };
    }
    
    /**
     * Configuración específica para endpoints de optimización
     */
    public static class OptimizacionCorsConfig {
        public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:8080", 
            "https://dashboard.morapack.com",  // Solo dashboard para optimización
            "https://admin.morapack.com"       // Panel administrativo
        };
        
        public static final String[] ALLOWED_METHODS = {
            "GET", "POST", "OPTIONS"  // Solo lectura y ejecución para optimización
        };
    }
    
    /**
     * Configuración para WebSocket CORS
     */
    public static class WebSocketCorsConfig {
        public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:8080",
            "https://morapack-frontend.com",
            "https://dashboard.morapack.com"
        };
        
        /**
         * Valida si un origen está permitido para WebSocket
         */
        public static boolean isOriginAllowed(String origin) {
            if (origin == null) return false;
            
            for (String allowedOrigin : ALLOWED_ORIGINS) {
                if (allowedOrigin.equals(origin)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Filtro CORS personalizado para casos especiales
     */
    public static class CustomCorsFilter {
        
        /**
         * Aplica headers CORS a la respuesta
         */
        public static void applyCorsHeaders(HttpResponse response, String origin) {
            // if (WebSocketCorsConfig.isOriginAllowed(origin)) {
            //     response.addHeader("Access-Control-Allow-Origin", origin);
            //     response.addHeader("Access-Control-Allow-Credentials", "true");
            //     response.addHeader("Access-Control-Allow-Methods", 
            //                       String.join(", ", PedidosCorsConfig.ALLOWED_METHODS));
            //     response.addHeader("Access-Control-Allow-Headers", 
            //                       String.join(", ", PedidosCorsConfig.ALLOWED_HEADERS));
            //     response.addHeader("Access-Control-Max-Age", "3600");
            // }
        }
        
        /**
         * Maneja preflight requests (OPTIONS)
         */
        public static boolean handlePreflightRequest(HttpRequest request, HttpResponse response) {
            // if ("OPTIONS".equals(request.getMethod())) {
            //     String origin = request.getHeader("Origin");
            //     applyCorsHeaders(response, origin);
            //     response.setStatus(200);
            //     return true;
            // }
            return false;
        }
    }
    
    /**
     * Configuración de seguridad para CORS
     */
    public static class CorsSecurityConfig {
        
        // Patrones de URL que requieren autenticación
        public static final String[] AUTHENTICATED_PATTERNS = {
            "/api/pedidos/*/asignar-sede",
            "/api/pedidos/*/estado", 
            "/api/optimizacion/**",
            "/api/admin/**"
        };
        
        // Patrones de URL públicos (solo lectura)
        public static final String[] PUBLIC_PATTERNS = {
            "/api/pedidos",
            "/api/pedidos/*/",
            "/api/pedidos/estadisticas",
            "/api/aeropuertos",
            "/api/sedes"
        };
        
        /**
         * Valida si un endpoint requiere autenticación
         */
        public static boolean requiresAuthentication(String path) {
            for (String pattern : AUTHENTICATED_PATTERNS) {
                if (matchesPattern(path, pattern)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Valida si un endpoint es público
         */
        public static boolean isPublicEndpoint(String path) {
            for (String pattern : PUBLIC_PATTERNS) {
                if (matchesPattern(path, pattern)) {
                    return true;
                }
            }
            return false;
        }
        
        private static boolean matchesPattern(String path, String pattern) {
            // Implementación simple de matching de patrones
            // En una implementación real se usaría AntPathMatcher de Spring
            return path.matches(pattern.replace("*", ".*"));
        }
    }
    
    // Clases placeholder para tipado (en implementación real vendrían de Spring)
    public static class HttpRequest {
        public String getMethod() { return "GET"; }
        public String getHeader(String name) { return null; }
    }
    
    public static class HttpResponse {
        public void addHeader(String name, String value) {}
        public void setStatus(int status) {}
    }
}
