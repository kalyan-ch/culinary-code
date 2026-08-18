package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.TagRepository;
import com.wb.culinaryCode.model.recipe.rest.TagDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final ModelMapper modelMapper;

    public List<TagDTO> listTags() {
        return tagRepository.findAll().stream()
                .map(tag -> modelMapper.map(tag, TagDTO.class))
                .sorted(Comparator.comparing(TagDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
