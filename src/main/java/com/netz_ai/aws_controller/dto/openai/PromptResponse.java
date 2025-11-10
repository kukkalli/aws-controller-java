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
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PromptResponse {
    @Schema(example = "gpt-4")
    String model;

    @Schema(example = "stop")
    String finishReason;

    @Schema(description = "Assistant text output")
    String content;

    @Schema(example = "123")
    Integer promptTokens;

    @Schema(example = "456")
    Integer completionTokens;

    @Schema(example = "579")
    Integer totalTokens;
}
