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

import java.time.Instant;

@Value
@Builder
public class CreateAndWaitResponse {
    @Schema(example = "i-0123456789abcdef0")
    String instanceId;

    @Schema(example = "running")
    String state;

    @Schema(example = "t2.micro")
    String instanceType;

    @Schema(example = "ami-0abcdef1234567890")
    String imageId;

    @Schema(example = "ec2-203-0-113-25.compute-1.amazonaws.com")
    String publicDnsName;

    @Schema(example = "203.0.113.25")
    String publicIp;

    @Schema(example = "demo-ec2")
    String nameTag;

    @Schema(description = "UTC timestamp when the instance launched")
    Instant launchTime;
}
