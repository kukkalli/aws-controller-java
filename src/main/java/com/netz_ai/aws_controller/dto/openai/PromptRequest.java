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

package com.netz_ai.aws_controller.dto.openai;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PromptRequest {
    @Schema(description = "Optional system instruction to steer behavior",
            example = "You are a concise, expert cloud architect.")
    private String system;

    @NotBlank
    @Schema(description = "User prompt", example = "List 3 ways to harden an EC2 instance.")
    private String prompt;

    @Schema(description = "OpenAI model to use; defaults from config",
            example = "gpt-5")
    private String model;

    @Schema(description = "Sampling temperature (0..2). Default 0.7", example = "0.7")
    private Double temperature;

    @Positive
    @Schema(description = "Max tokens in the completion (optional)", example = "512")
    private Integer maxTokens;
}
