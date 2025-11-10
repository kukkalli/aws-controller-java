/*
 * Copyright (c) 2025. Netz AI GmbH <https://netz-ai.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netz_ai.aws_controller.controller.openai;

import com.netz_ai.aws_controller.dto.aws.CreateAndWaitResponse;
import com.netz_ai.aws_controller.dto.openai.PromptRequest;
import com.netz_ai.aws_controller.dto.openai.PromptResponse;
import com.netz_ai.aws_controller.service.openai.OpenAIAWSControllerService;
import com.netz_ai.aws_controller.service.openai.OpenAIResponsesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/openai")
@RequiredArgsConstructor
@Tag(name = "OpenAI", description = "Prompt → text response via Responses API")
public class OpenAIController {
    private final OpenAIResponsesService service;
    private final OpenAIAWSControllerService openAIAWSControllerService;

    @PostMapping("/prompt")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Send a prompt and get a detailed response")
    public PromptResponse prompt(@Valid @RequestBody PromptRequest req) {
        return service.respond(req);
    }

    @PostMapping("/aws-controller")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Send a prompt and get a detailed response")
    public CreateAndWaitResponse awsController(@Valid @RequestBody PromptRequest req) {
        return openAIAWSControllerService.respond(req);
    }

}
