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

package com.netz_ai.aws_controller.service.aws;

import com.netz_ai.aws_controller.controller.aws.PingResponse;
import com.netz_ai.aws_controller.properties.AwsProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsIamPingService {
    private final AwsProps awsProps;

    public String whoAmI() {
        log.debug("Inside AwsIamPingService.whoAmI(): {}", awsProps.getRegion());
        return awsProps.getRegion();
    }

    public PingResponse ping() {
        long start = System.nanoTime();
        log.debug("Inside AwsIamPingService.ping(): {}", awsProps.getRegion());
        // Resolve region: prefer AWS_REGION env/system; fallback to us-east-1
        Region region = Optional.ofNullable(awsProps.getRegion())
                .map(Region::of)
                .orElse(Region.EU_CENTRAL_2);
        log.info("Region: {}", region.toString());

        String providerHint = "Using DefaultCredentialsProvider";
        String err = null;
        String account = null, userId = null, arn = null;

        try (StsClient sts = StsClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            GetCallerIdentityResponse resp = sts.getCallerIdentity(GetCallerIdentityRequest.builder().build());
            account = resp.account();
            userId = resp.userId();
            arn = resp.arn();

        } catch (Exception ex) {
            err = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        long latencyMs = Math.round((System.nanoTime() - start) / 1_000_000.0);

        return PingResponse.builder()
                .status(err == null ? "OK" : "FAIL")
                .region(region.id())
                .account(account)
                .userId(userId)
                .arn(arn)
                .latencyMs(latencyMs)
                .timestamp(Instant.now().toString())
                .providerHint(providerHint)
                .error(err == null ? "" : err)
                .build();
    }
}
