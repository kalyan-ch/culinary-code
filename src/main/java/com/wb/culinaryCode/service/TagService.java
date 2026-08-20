package com.wb.culinaryCode.service;

import com.wb.culinaryCode.dao.TagRepository;
import com.wb.culinaryCode.model.recipe.rest.TagDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final ModelMapper modelMapper;

    public List<TagDTO> listTags(UUID viewerId) {
        return tagRepository.findVisibleTo(viewerId).stream()
                .map(tag -> modelMapper.map(tag, TagDTO.class))
                .sorted(Comparator.comparing(TagDTO::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
