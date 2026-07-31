package app.mealdeck.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
/**
 * Configures development CORS access and serving of uploaded meal photos.
 */
public class WebConfig implements WebMvcConfigurer {
    private final String uploadDirectory;

    /**
     * Creates web configuration for the selected upload directory.
     *
     * @param uploadDirectory filesystem directory containing uploaded images
     */
    public WebConfig(@Value("${mealdeck.upload-dir:./data/uploads}") String uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    @Override
    /**
     * Allows local Expo clients to call the backend API.
     *
     * @param registry MVC CORS mapping registry
     */
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "exp://*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
    /**
     * Maps stored upload files onto the public {@code /uploads/**} path.
     *
     * @param registry MVC static-resource registry
     */
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(path.toUri().toString());
    }
}
