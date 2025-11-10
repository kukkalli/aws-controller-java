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

package com.netz_ai.aws_controller.dto.aws;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TerminateEc2Response {
    @Schema(example = "i-0123456789abcdef0")
    String instanceId;

    @Schema(example = "running", description = "State before the terminate call (if known)")
    String previousState;

    @Schema(example = "shutting-down", description = "Immediate state after the terminate call (AWS response)")
    String currentState;

    @Schema(example = "terminated", description = "Final state if wait=true (else null)")
    String finalState;
}
