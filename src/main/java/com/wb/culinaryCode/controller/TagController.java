package com.wb.culinaryCode.controller;

import com.wb.culinaryCode.model.recipe.rest.TagDTO;
import com.wb.culinaryCode.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<List<TagDTO>> listTags() {
        return ResponseEntity.ok(tagService.listTags());
    }
}
