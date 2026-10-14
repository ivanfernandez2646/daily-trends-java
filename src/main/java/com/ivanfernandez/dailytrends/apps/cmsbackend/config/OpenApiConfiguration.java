package com.ivanfernandez.dailytrends.apps.cmsbackend.config;

import com.ivanfernandez.dailytrends.apps.cmsbackend.errors.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI openApi(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties build = buildProperties.getIfAvailable();
        // build-info only exists after a Maven build; an IDE run without it still starts.
        String version = build != null && build.getVersion() != null ? build.getVersion() : "unknown";
        return new OpenAPI().info(new Info().title("daily-trends API").version(version));
    }

    /** Every route can fail with the generic 500 body. */
    @Bean
    OpenApiCustomizer internalServerErrorResponses() {
        return openApi -> {
            Components components = openApi.getComponents() != null ? openApi.getComponents() : new Components();
            ModelConverters.getInstance().read(ErrorResponse.class).forEach(components::addSchemas);
            openApi.setComponents(components);
            var internalServerError = new ApiResponse()
                    .description("Internal server error")
                    .content(new Content()
                            .addMediaType(
                                    org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().stream()
                        .flatMap(path -> path.readOperations().stream())
                        .forEach(operation -> operation.getResponses().addApiResponse("500", internalServerError));
            }
        };
    }
}
