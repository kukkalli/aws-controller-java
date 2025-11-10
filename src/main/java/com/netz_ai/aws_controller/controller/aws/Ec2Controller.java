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

import com.netz_ai.aws_controller.dto.aws.*;
import com.netz_ai.aws_controller.service.aws.Ec2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Duration;
import java.util.Optional;

import static com.netz_ai.aws_controller.constants.AWSConstants.BASE_URL_EC2;


@RestController
@RequestMapping(BASE_URL_EC2)
@RequiredArgsConstructor
@Tag(name = "EC2", description = "Endpoints to manage EC2 instances")
public class Ec2Controller {
    private final Ec2Service ec2Service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a free-tier EC2 instance",
            description = """
                Launches a single Amazon Linux instance (t2.micro) in the default VPC.
                AMI is resolved automatically via SSM unless you override it.
                Key pair is optional. If omitted, the instance launches without an SSH key.
                """)
    public CreateEc2Response create(@Valid @RequestBody CreateEc2Request req) {
        RunInstancesResponse run = ec2Service.createEc2Instance(
                Optional.ofNullable(req.getName()),
                Optional.ofNullable(req.getKeyName()),
                req.getUseAl2023() == null || req.getUseAl2023(),
                Optional.ofNullable(req.getAmiId()),
                Optional.ofNullable(req.getInstanceType()),
                Optional.ofNullable(req.getUserData()),
                Optional.ofNullable(req.getSecurityGroups())
        );

        String instanceId = run.instances().getFirst().instanceId();
        Instance instance = ec2Service.describeInstance(instanceId);

        return CreateEc2Response.builder()
                .instanceId(instanceId)
                .instanceType(instance.instanceTypeAsString())
                .imageId(instance.imageId())
                .state(instance.state().nameAsString())
                .build();
    }

    @PostMapping("/wait-running")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an EC2 instance and wait until it is running",
            description = """
                Launches a single Amazon Linux instance, then blocks until the instance reaches 'running' or the timeout elapses.
                - Accepts the same fields as the standard create endpoint (instanceType, userData, securityGroups, etc.).
                - Parameters:
                  * timeoutSeconds (default 300) - total time to wait
                  * pollSeconds (default 5) - polling interval
                """)
    public CreateAndWaitResponse createAndWait(
            @Valid @RequestBody CreateEc2Request req,
            @RequestParam(defaultValue = "300") @Min(1) int timeoutSeconds,
            @RequestParam(defaultValue = "5")   @Min(1) int pollSeconds) {

        try {
            Instance instance = ec2Service.createAndWaitRunning(
                    Optional.ofNullable(req.getName()),
                    Optional.ofNullable(req.getKeyName()),
                    req.getUseAl2023() == null || req.getUseAl2023(),
                    Optional.ofNullable(req.getAmiId()),
                    Optional.ofNullable(req.getInstanceType()),
                    Optional.ofNullable(req.getUserData()),
                    Optional.of(req.getSecurityGroups()),
                    Duration.ofSeconds(timeoutSeconds),
                    Duration.ofSeconds(pollSeconds)
            );

            String nameTag = instance.tags() == null ? null :
                    instance.tags().stream()
                            .filter(t -> "Name".equals(t.key()))
                            .findFirst().map(t -> t.value()).orElse(null);

            return CreateAndWaitResponse.builder()
                    .instanceId(instance.instanceId())
                    .state(instance.state().nameAsString())
                    .instanceType(instance.instanceTypeAsString())
                    .imageId(instance.imageId())
                    .publicDnsName(instance.publicDnsName())
                    .publicIp(instance.publicIpAddress())
                    .nameTag(nameTag)
                    .launchTime(instance.launchTime())
                    .build();

        } catch (Ec2Exception e) {
            throw e; // your global handler (or let Spring return a 4xx/5xx)
        }
    }

    @GetMapping("/{instanceId}/state")
    @Operation(summary = "Get current EC2 state", description = "Returns the current lifecycle state of the instance.")
    public InstanceStateResponse getState(@PathVariable String instanceId) {
        try {
            Instance i = ec2Service.describeInstance(instanceId);
            return InstanceStateResponse.builder()
                    .instanceId(instanceId)
                    .state(i.state().nameAsString())
                    .instanceType(i.instanceTypeAsString())
                    .imageId(i.imageId())
                    .publicDnsName(i.publicDnsName())
                    .publicIp(i.publicIpAddress())
                    .build();
        } catch (Ec2Exception e) {
            if (e.awsErrorDetails() != null && "InvalidInstanceID.NotFound"
                    .equals(e.awsErrorDetails().errorCode())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Instance not found");
            }
            throw e;
        }
    }

    @GetMapping("/{instanceId}/wait-running")
    @Operation(summary = "Wait until instance is running",
            description = "Blocks until the instance reaches 'running' or until the timeout elapses.")
    public InstanceStateResponse waitUntilRunning(
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "300") @Min(1) int timeoutSeconds,
            @RequestParam(defaultValue = "5")  @Min(1) int pollSeconds) {

        try {
            Instance i = ec2Service.waitUntilRunning(
                    instanceId,
                    Duration.ofSeconds(timeoutSeconds),
                    Duration.ofSeconds(pollSeconds));

            return InstanceStateResponse.builder()
                    .instanceId(instanceId)
                    .state(i.state().nameAsString())
                    .instanceType(i.instanceTypeAsString())
                    .imageId(i.imageId())
                    .publicDnsName(i.publicDnsName())
                    .publicIp(i.publicIpAddress())
                    .build();
        } catch (Ec2Exception e) {
            if (e.awsErrorDetails() != null && "InvalidInstanceID.NotFound"
                    .equals(e.awsErrorDetails().errorCode())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Instance not found");
            }
            throw e;
        }
    }

    @DeleteMapping("/{instanceId}")
    @Operation(summary = "Terminate an EC2 instance",
            description = """
                Terminates the instance. If wait=true, the call blocks until the instance reaches 'terminated' or the timeout elapses.
                """)
    public TerminateEc2Response delete(
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "false") boolean wait,
            @RequestParam(defaultValue = "300") @Min(1) int timeoutSeconds,
            @RequestParam(defaultValue = "5")   @Min(1) int pollSeconds) {

        try {
            TerminateInstancesResponse resp = ec2Service.terminateInstance(instanceId);
            InstanceStateChange change = resp.terminatingInstances().getFirst();

            String prev = change.previousState() != null ? change.previousState().nameAsString() : null;
            String curr = change.currentState()  != null ? change.currentState().nameAsString()  : null;

            String finalState = null;
            if (wait) {
                Instance finalDesc = ec2Service.waitUntilTerminated(
                        instanceId,
                        Duration.ofSeconds(timeoutSeconds),
                        Duration.ofSeconds(pollSeconds));
                finalState = finalDesc.state().nameAsString();
            }

            return TerminateEc2Response.builder()
                    .instanceId(instanceId)
                    .previousState(prev)
                    .currentState(curr)
                    .finalState(finalState)
                    .build();

        } catch (Ec2Exception e) {
            if (e.awsErrorDetails() != null && "InvalidInstanceID.NotFound"
                    .equals(e.awsErrorDetails().errorCode())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Instance not found");
            }
            throw e;
        }
    }
}
