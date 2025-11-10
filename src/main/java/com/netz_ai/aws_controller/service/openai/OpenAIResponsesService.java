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

package com.netz_ai.aws_controller.service.openai;

import com.netz_ai.aws_controller.dto.openai.PromptRequest;
import com.netz_ai.aws_controller.dto.openai.PromptResponse;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIResponsesService {
    private final OpenAIClient client;

    @Value("${openai.model:gpt-5}")
    private String defaultModel;

    @Value("${openai.temperature:1}")
    private Double defaultTemperature;

    public PromptResponse respond(PromptRequest req) {

        String model = (req.getModel() == null || req.getModel().isBlank()) ? defaultModel : req.getModel();
        Double temperature = (req.getTemperature() == null) ? defaultTemperature : req.getTemperature();

        ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder()
                .model(model)
                .maxCompletionTokens(25000)
                .temperature(1)
                .addUserMessage(req.getPrompt()).build();

        ChatCompletion response = client.chat().completions().create(createParams);

        Optional<String> content = response.choices().getFirst().message().content();
        response.usage().ifPresent(usage -> {
            log.info(usage.toString());
        });

        return PromptResponse.builder()
                .content(content.get())
                .build();
    }
}
