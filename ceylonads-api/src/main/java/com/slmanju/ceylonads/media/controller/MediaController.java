package com.slmanju.ceylonads.media.controller;

import com.slmanju.ceylonads.media.dto.MediaResponse;
import com.slmanju.ceylonads.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/ads/{adId}/media")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MODERATOR')")
    @Operation(summary = "Upload an image to one of my ads")
    MediaResponse upload(
            @PathVariable Long adId,
            Authentication authentication,
            @RequestPart("file") MultipartFile file) throws IOException {
        return mediaService.upload(adId, authentication.getName(), file);
    }
}
