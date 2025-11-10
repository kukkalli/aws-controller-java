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

package com.netz_ai.aws_controller.controller.aws;

import com.netz_ai.aws_controller.service.aws.AwsIamPingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.regions.Region;

import java.util.Optional;

@Slf4j
@Tag(name = "AWS", description = "AWS IAM/STSes utilities")
@RestController
@RequestMapping(AwsIamPingController.URI_BASE_PATH)
@RequiredArgsConstructor
public class AwsIamPingController {
    public static final String URI_BASE_PATH = "/api/v1/aws";
    private final AwsIamPingService service;

    @Operation(
            summary = "Ping AWS using STS GetCallerIdentity",
            description = "Validates your AWS credentials by calling STS. Returns account, arn, and userId."
    )
    @GetMapping("/ping")
    public ResponseEntity<PingResponse> ping() {
        // Resolve region: prefer AWS_REGION env/system; fallback to us-east-1
        log.info("Why no region?: {}", System.getenv("AWS_REGION"));
        Region region = Optional.ofNullable(System.getenv("AWS_REGION"))
                .map(Region::of)
                .orElse(Region.EU_CENTRAL_2);
        log.info("Region: {}", region.toString());
        log.info("Ping AWS using STS GetCallerIdentity");
        return ResponseEntity.ok(service.ping());
    }

    @GetMapping("/home")
    public ResponseEntity<PingResponse> getPing() {
        return ResponseEntity.ok(service.ping());
    }
}
