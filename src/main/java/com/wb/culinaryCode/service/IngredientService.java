package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.IngredientRepository;
import com.wb.culinaryCode.model.recipe.rest.IngredientOptionDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final ModelMapper modelMapper;

    public List<IngredientOptionDTO> listIngredients(UUID viewerId) {
        return ingredientRepository.findVisibleTo(viewerId).stream()
                .map(ingredient -> modelMapper.map(ingredient, IngredientOptionDTO.class))
                .sorted(Comparator.comparing(IngredientOptionDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
