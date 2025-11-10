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

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.waiters.Ec2Waiter;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Ec2Service {
    private final Ec2Client ec2;
    private final SsmClient ssm;

    @Value("${aws.al2Param}")
    private String al2Param;

    @Value("${aws.al2023Param}")
    private String al2023Param;

    public RunInstancesResponse createEc2Instance(
            Optional<String> nameOpt,
            Optional<String> keyNameOpt,
            boolean useAl2023,
            Optional<String> overrideAmi,
            Optional<String> instanceTypeStr,
            Optional<String> userDataPlain,
            Optional<List<String>> securityGroupIdsOpt) {

        String imageId = overrideAmi.orElseGet(() -> fetchLatestAmazonLinuxAmi(useAl2023));
        // Resolve instance type (default t2.micro) free tier
        InstanceType instanceType = InstanceType.T2_MICRO;
        if (instanceTypeStr.isPresent() && !instanceTypeStr.get().isBlank()) {
            try {
                // AWS SDK v2 enum expects exact value (e.g., "t3.micro")
                instanceType = InstanceType.fromValue(instanceTypeStr.get());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported instanceType: " + instanceTypeStr.get());
            }
        }

        RunInstancesRequest.Builder req = RunInstancesRequest.builder()
                .imageId(imageId)
                .instanceType(instanceType)
                .minCount(1)
                .maxCount(1);

        keyNameOpt.filter(s -> !s.isBlank()).ifPresent(req::keyName);

        // Security group IDs (VPC). If omitted -> default security group is used.
        securityGroupIdsOpt
                .filter(list -> !list.isEmpty())
                .ifPresent(req::securityGroupIds);

        // User data: EC2 API expects base64-encoded content; accept plain text and encode here.
        userDataPlain.filter(s -> !s.isBlank()).ifPresent(s -> {
            String b64 = Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
            req.userData(b64);
        });

        RunInstancesResponse run = ec2.runInstances(req.build());

        String instanceId = run.instances().getFirst().instanceId();

        // Name tag (optional)
        nameOpt.filter(s -> !s.isBlank()).ifPresent(name ->
                ec2.createTags(CreateTagsRequest.builder()
                        .resources(instanceId)
                        .tags(Tag.builder().key("Name").value(name).build())
                        .build())
        );

        return run;
    }

    public Instance createAndWaitRunning(
            Optional<String> nameOpt,
            Optional<String> keyNameOpt,
            boolean useAl2023,
            Optional<String> overrideAmi,
            Optional<String> instanceTypeStr,
            Optional<String> userDataPlain,
            Optional<List<String>> securityGroupIdsOpt,
            Duration timeout,
            Duration pollInterval) {

        var run = createEc2Instance(
                nameOpt, keyNameOpt, useAl2023, overrideAmi,
                instanceTypeStr, userDataPlain, securityGroupIdsOpt);

        String instanceId = run.instances().getFirst().instanceId();

        // Block until 'running'
        return waitUntilRunning(instanceId, timeout, pollInterval);
    }

    private String fetchLatestAmazonLinuxAmi(boolean useAl2023) {
        String param = useAl2023 ? al2023Param : al2Param;
        GetParameterResponse resp = ssm.getParameter(GetParameterRequest.builder()
                .name(param)
                .build());
        return resp.parameter().value(); // ami-xxxx
    }

    public String getInstanceStateName(String instanceId) {
        Instance i = describeInstance(instanceId);
        return i.state().nameAsString();
    }

    /**
     * Wait until the instance becomes 'running' (or timeout).
     * @param instanceId EC2 instance id
     * @param timeout total time to wait
     * @param pollInterval delay between polls
     * @return the latest Instance description when waiter finishes
     * @throws ResponseStatusException 408 if timed out, 404 if not found
     */
    public Instance waitUntilRunning(String instanceId, Duration timeout, Duration pollInterval) {
        DescribeInstancesRequest req = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        Ec2Waiter waiter = Ec2Waiter.builder()
                .client(ec2)
                .overrideConfiguration(WaiterOverrideConfiguration.builder()
                        .waitTimeout(timeout)
                        .backoffStrategy(FixedDelayBackoffStrategy.create(pollInterval))
                        .build())
                .build();

        WaiterResponse<DescribeInstancesResponse> response =
                waiter.waitUntilInstanceRunning(req);

        if (response.matched().exception().isPresent()) {
            // timed out or failed
            throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT,
                    "Timed out waiting for instance to be running");
        }

        return describeInstance(instanceId);
    }

    public Instance describeInstance(String instanceId) {
        DescribeInstancesResponse resp = ec2.describeInstances(DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build());
        return resp.reservations().getFirst().instances().getFirst();
    }

    public TerminateInstancesResponse terminateInstance(String instanceId) {
        TerminateInstancesRequest req = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        return ec2.terminateInstances(req);
    }

    public Instance waitUntilTerminated(String instanceId, Duration timeout, Duration pollInterval) {
        Ec2Waiter waiter = Ec2Waiter.builder()
                .client(ec2)
                .overrideConfiguration(WaiterOverrideConfiguration.builder()
                        .waitTimeout(timeout)
                        .backoffStrategy(FixedDelayBackoffStrategy.create(pollInterval))
                        .build())
                .build();

        DescribeInstancesRequest req = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

        WaiterResponse<DescribeInstancesResponse> wr = waiter.waitUntilInstanceTerminated(req);
        if (wr.matched().exception().isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.REQUEST_TIMEOUT, "Timed out waiting for instance to terminate");
        }
        return describeInstance(instanceId); // final description (state should be 'terminated')
    }
}
