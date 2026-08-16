package com.wb.culinaryCode.config;

import com.wb.culinaryCode.model.recipe.RecipeIngredient;
import com.wb.culinaryCode.model.recipe.rest.IngredientsDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CulinaryCodeConfig {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
            }
        };
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        PropertyMap<RecipeIngredient, IngredientsDTO> propertyMap = new PropertyMap<>() {
            @Override
            protected void configure() {
                map().setId(source.getId());
                map().setName(source.getIngredient().getName());
                map().setUnit(source.getUnit());
                map().setNotes(source.getNotes());
                map().setQuantity(source.getQuantity());
            }
        };

        modelMapper.addMappings(propertyMap);

        return modelMapper;
    }
}
