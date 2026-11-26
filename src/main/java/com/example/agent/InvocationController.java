package com.example.agent;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@CrossOrigin(origins = "*")
public class InvocationController {

    private final ChatService chatService;

    // Constructor injection fixes the uninitialized final field error.
    public InvocationController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(
            value = "/load",
            consumes = MediaType.TEXT_PLAIN_VALUE
    )
    public void loadDocument(@RequestBody String content) {
        chatService.loadDocument(content);
    }

    @PostMapping(
            value = "/invocations",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Flux<String> handleInvocation(
            @RequestBody InvocationRequest request,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        String username = (auth != null && !auth.isBlank())
                ? auth
                : "default";

        return chatService.chat(request.prompt(), username);
    }
}