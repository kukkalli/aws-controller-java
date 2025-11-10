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
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class CreateEc2Request {
    @Schema(description = "Optional: Name tag for the instance", example = "demo-ec2")
    private String name;

    @Schema(description = "Optional: existing EC2 key pair name (for SSH). If omitted, instance will have no key.", example = "my-keypair")
    private String keyName;

    @Schema(description = "Optional: use Amazon Linux 2023 (true) or Amazon Linux 2 (false). Default true.")
    private Boolean useAl2023 = Boolean.TRUE;

    @Schema(description = "Optional: specific AMI id to override SSM lookup", example = "ami-0abcdef1234567890")
    @Pattern(regexp = "ami-[a-f0-9]{8,17}", message = "Invalid AMI id")
    private String amiId;


    @Schema(description = "Optional: EC2 instance type. Defaults to t2.micro if not provided.", example = "t2.micro")
    @Pattern(regexp = "^[a-z0-9]+\\.[a-z0-9]+$", message = "Invalid instance type format (e.g., t2.micro)")
    private String instanceType;

    @Schema(description = "Optional: user data script (plain text). Will be base64-encoded for EC2. Example shown is a simple bash script.",
            example = "#!/bin/bash\nyum update -y\nyum install -y httpd\nsystemctl enable --now httpd")
    private String userData;

    @Schema(description = "Optional: list of security group IDs (VPC). If omitted, default security group is used.",
            example = "[\"sg-0123456789abcdef0\", \"sg-0fedcba9876543210\"]")
    private List<@Pattern(regexp = "sg-[a-f0-9]{8,17}", message = "Invalid security group id") String> securityGroups;
}
