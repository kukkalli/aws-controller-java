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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netz_ai.aws_controller.dto.aws.CreateAndWaitResponse;
import com.netz_ai.aws_controller.dto.aws.CreateEc2Request;
import com.netz_ai.aws_controller.dto.openai.PromptRequest;
import com.netz_ai.aws_controller.service.aws.Ec2Service;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIAWSControllerService {
    private final OpenAIClient client;
    private final OpenAIResponsesService openAIResponsesService; // your existing OpenAI service that hits Responses API
    private final Ec2Service ec2Service;                         // your existing EC2 service
    private final ObjectMapper objectMapper;                     // Spring Boot auto-configured

    @Value("${openai.model:gpt-5}")
    private String defaultModel;

    @Value("${openai.temperature:1}")
    private Double defaultTemperature;

    public CreateAndWaitResponse respond(PromptRequest req) {

        String model = (req.getModel() == null || req.getModel().isBlank()) ? defaultModel : req.getModel();
        Double temperature = (req.getTemperature() == null) ? defaultTemperature : req.getTemperature();

        ChatCompletionCreateParams createParams = ChatCompletionCreateParams.builder()
                .model(model)
                .maxCompletionTokens(25000)
                .temperature(1)
                .addUserMessage(getPrompt(req.getPrompt()))
                .build();
        log.info("The final prompt:\n{}", getPrompt(req.getPrompt()));

        ChatCompletion response = client.chat().completions().create(createParams);

        Optional<String> content = response.choices().getFirst().message().content();
        response.usage().ifPresent(usage -> {
            log.info(usage.toString());
        });

        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI returned empty content");
        }
        log.info("JSON result:\n{}", content.get());

        final CreateEc2Request ec2Req;
        try {
            ec2Req = objectMapper.readValue(content.get(), CreateEc2Request.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "OpenAI content is not valid CreateEc2Request JSON: " + e.getOriginalMessage(), e
            );
        }
        log.info("CreateEc2Request object:\n{}", ec2Req.toString());

        // Optional: light sanity checks
        if (ec2Req.getKeyName() == null || ec2Req.getKeyName().isBlank()) {
            ec2Req.setKeyName("AWS-SAA-C003-RSA"); // default
        }
        if (ec2Req.getInstanceType() == null || ec2Req.getInstanceType().isBlank()) {
            ec2Req.setInstanceType("t2.micro"); // default
        }
        if (ec2Req.getUseAl2023() == null) {
            ec2Req.setUseAl2023(Boolean.TRUE);
        }
        if (ec2Req.getSecurityGroups() == null || ec2Req.getSecurityGroups().isEmpty()) {
            ec2Req.setSecurityGroups(new ArrayList<>());
            ec2Req.getSecurityGroups().add("sg-074b93f3fa5e149d4");
            ec2Req.getSecurityGroups().add("sg-03ab1f5cc977d5c85");
            ec2Req.getSecurityGroups().add("sg-064f4f6b368686377");
        }
        try {
            Instance instance = ec2Service.createAndWaitRunning(
                    Optional.ofNullable(ec2Req.getName()),
                    Optional.ofNullable(ec2Req.getKeyName()),
                    ec2Req.getUseAl2023() == null || ec2Req.getUseAl2023(),
                    Optional.ofNullable(ec2Req.getAmiId()),
                    Optional.ofNullable(ec2Req.getInstanceType()),
                    Optional.ofNullable(ec2Req.getUserData()),
                    Optional.of(ec2Req.getSecurityGroups()),
                    Duration.ofSeconds(300),
                    Duration.ofSeconds(5));
            String nameTag = instance.tags() == null ? null :
                    instance.tags().stream().filter(t -> "Name".equals(t.key()))
                            .findFirst().map(Tag::value).orElse(null);

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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OpenAI could not create instance");
        }
    }

    private String getPrompt(String query) {
        return String.format("Here’s a ready-to-use prompt template you can feed to your model. It wraps a user query (`{query}`) and forces a **single JSON** output matching your EC2 create API shape. It also instructs how to generate robust `userData` for Amazon Linux with **yum**, including special handling for **MySQL** or **MariaDB** requests.\n" +
                "\n" +
                "````text\n" +
                "You are an expert cloud/solution architect and DevOps engineer. Your job is to produce EXACTLY ONE JSON object that will be used by an API to create a single AWS EC2 instance. You MUST follow ALL rules below.\n" +
                "\n" +
                "OUTPUT FORMAT (MANDATORY)\n" +
                "- Output ONLY a JSON object. No prose, no markdown, no comments.\n" +
                "- The JSON MUST match this exact shape and include ALL keys, in this order:\n" +
                "{\n" +
                "  \"name\": \"<string, kebab-case, short>\",\n" +
                "  \"keyName\": \"<string or empty if not provided>\",\n" +
                "  \"useAl2023\": <true|false>,\n" +
                "  \"instanceType\": \"<string, e.g., t2.micro>\",\n" +
                "  \"securityGroups\": [\"<sg-id>\", \"...\"], \n" +
                "  \"userData\": \"<bash script as a single string with \\\\n line breaks>\"\n" +
                "}\n" +
                "- If the user did not provide a value, choose a sensible default as defined in the RULES section.\n" +
                "- `securityGroups` MUST be an array. If none are provided, return `[]` (empty array).\n" +
                "- `userData` MUST be a valid bash script for **Amazon Linux** and MUST use `yum` for all package operations. Embed it as a JSON string with `\\n` for newlines and escape quotes properly.\n" +
                "\n" +
                "RULES FOR VALUES\n" +
                "1) OS & AMI toggle\n" +
                "   - Always target Amazon Linux. If the user’s intent implies Amazon Linux 2023, set `\"useAl2023\": true`, otherwise default to true when unspecified.\n" +
                "   - If the user explicitly asks for Amazon Linux 2, set `\"useAl2023\": false`.\n" +
                "\n" +
                "2) Instance type\n" +
                "   - If the user specifies one, use it.\n" +
                "   - Otherwise default to `\"t2.micro\"` (free-tier eligible).\n" +
                "\n" +
                "3) Name\n" +
                "   - Derive a concise, readable kebab-case name from the user’s request, e.g., \"web-1\", \"mysql-db-1\", \"mariadb-db-1\".\n" +
                "   - Keep it alphanumeric and hyphenated.\n" +
                "\n" +
                "4) Key pair\n" +
                "   - If the user gives a key pair name, set `\"keyName\"` accordingly.\n" +
                "   - Otherwise set `\"keyName\": \"\"` (empty string).\n" +
                "\n" +
                "5) Security groups\n" +
                "   - If the user provides specific SG IDs (matching `^sg-[a-f0-9]{8,17}$`), include them in order.\n" +
                "   - If none are provided, return an empty array: `\"securityGroups\": []`.\n" +
                "   - Do NOT invent SG IDs.\n" +
                "\n" +
                "6) userData (critical)\n" +
                "   - Always start with a bash shebang, set `-euo pipefail`, update the system, and install only what’s needed.\n" +
                "   - All package operations MUST use `yum` (not dnf).\n" +
                "   - Use `systemctl enable --now <service>` to enable and start services.\n" +
                "   - If the request mentions a **web/http server** (e.g., \"web\", \"http\", \"apache\"), install and start **httpd**, and place a basic index.html.\n" +
                "   - If **MySQL** is requested:\n" +
                "       * Prepare Amazon Linux user data that installs **MySQL Community Server** with `yum`. \n" +
                "       * If a dedicated repo is needed, add it via `yum-config-manager` or the MySQL community repo RPM appropriate for Amazon Linux, then `yum install -y mysql-server`.\n" +
                "       * Enable and start `mysqld`.\n" +
                "       * If passwords or DB/user names are provided by the user, apply them; otherwise use secure placeholders like `StrongP@ssw0rd!` and `app_db`, `app_user`.\n" +
                "       * Perform a non-interactive hardening step (e.g., set root password, remove test DB/users if feasible) and create the application DB/user if requested.\n" +
                "   - If **MariaDB** is requested:\n" +
                "       * Install **mariadb-server** via `yum install -y mariadb-server`.\n" +
                "       * Enable and start `mariadb`.\n" +
                "       * Perform similar secure initialization and optional DB/user creation.\n" +
                "   - If both MySQL and MariaDB are mentioned, prefer the last explicitly requested one.\n" +
                "   - If neither DB is requested and no web server is requested, keep userData minimal: update packages and echo a health marker file.\n" +
                "   - Ensure every command is compatible with Amazon Linux and uses `yum` (`yum update -y`, `yum install -y <pkg>`, `yum remove -y <pkg>` when applicable).\n" +
                "\n" +
                "7) Safety & determinism\n" +
                "   - Do NOT include secrets pulled from nowhere. If the user does not provide passwords, use safe placeholders (e.g., `StrongP@ssw0rd!`) that the user must change later.\n" +
                "   - Keep scripts idempotent where reasonable (e.g., guard file creations with `|| true` where appropriate).\n" +
                "\n" +
                "8) No additional keys\n" +
                "   - Do NOT add any extra JSON keys beyond the specified structure.\n" +
                "\n" +
                "MAPPING THE USER REQUEST\n" +
                "- Read the user’s request between triple backticks as the only source of truth.\n" +
                "- Extract intent for name, instance type, key pair, and SG IDs if present.\n" +
                "- Detect whether the user wants MySQL or MariaDB; build the correct `userData` accordingly.\n" +
                "- Detect if they want a web server; install httpd and create a basic index page.\n" +
                "- If anything is missing, apply defaults from RULES.\n" +
                "\n" +
                "INPUT (user request)\n" +
                "```%s```\n" +
                "\n" +
                "NOW PRODUCE THE FINAL JSON OBJECT ONLY.\n" +
                "````\n", query);
    }
}
