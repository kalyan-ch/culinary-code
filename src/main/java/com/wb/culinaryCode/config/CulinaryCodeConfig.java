package com.wb.culinaryCode.config;

import com.wb.culinaryCode.model.RecipeIngredient;
import com.wb.culinaryCode.model.rest.IngredientsDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CulinaryCodeConfig {

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
