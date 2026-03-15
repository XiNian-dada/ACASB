package com.leeinx.acasb.controller;

import com.leeinx.acasb.service.DatasetImageRecordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
public class MediaController {
    private final DatasetImageRecordService datasetImageRecordService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public MediaController(DatasetImageRecordService datasetImageRecordService) {
        this.datasetImageRecordService = datasetImageRecordService;
    }

    @GetMapping("/media/dataset/**")
    public ResponseEntity<Resource> getDatasetImage(HttpServletRequest request) {
        String relativeMediaPath = extractPathWithinHandler(request);
        if (relativeMediaPath == null || relativeMediaPath.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        int separatorIndex = relativeMediaPath.indexOf('/');
        if (separatorIndex <= 0 || separatorIndex >= relativeMediaPath.length() - 1) {
            return ResponseEntity.notFound().build();
        }

        String datasetName = relativeMediaPath.substring(0, separatorIndex);
        String relativePath = relativeMediaPath.substring(separatorIndex + 1);

        Path imagePath = datasetImageRecordService.resolveMediaPath(datasetName, relativePath);
        if (imagePath == null || !Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(imagePath.toUri());
            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String extractPathWithinHandler(HttpServletRequest request) {
        Object pathWithinMapping = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        Object bestPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (!(pathWithinMapping instanceof String mapping) || !(bestPattern instanceof String pattern)) {
            return null;
        }
        String extracted = pathMatcher.extractPathWithinPattern(pattern, mapping);
        if (extracted == null) {
            return null;
        }
        List<String> segments = java.util.Arrays.stream(extracted.split("/"))
                .filter(segment -> !segment.isBlank())
                .toList();
        return String.join("/", segments);
    }
}
