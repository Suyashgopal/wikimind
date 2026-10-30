package com.wikimind.controller;

import com.wikimind.dto.AddRequest;
import com.wikimind.dto.AddResponse;
import com.wikimind.dto.AskRequest;
import com.wikimind.dto.AskResponse;
import com.wikimind.security.AuthUser;
import com.wikimind.service.RagService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/add")
    public AddResponse add(@Valid @RequestBody AddRequest request, @AuthenticationPrincipal AuthUser caller) {
        int chunks = ragService.ingest(request.title(), caller.orgId(), request.content());
        return new AddResponse(request.title(), chunks);
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request, @AuthenticationPrincipal AuthUser caller) {
        return new AskResponse(ragService.answer(request.question(), caller.orgId()));
    }
}
