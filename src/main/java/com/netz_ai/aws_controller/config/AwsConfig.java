///*
// * Copyright (c) 2025. Netz AI GmbH <https://netz-ai.com>
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// * either express or implied. See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.netz_ai.aws_controller.config;
//
//import com.netz_ai.aws_controller.properties.AwsProps;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
//import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.sts.StsClient;
//
//
//@Slf4j
//@Configuration
//@EnableConfigurationProperties(AwsProps.class)
//public class AwsConfig {
//    @Bean
//    public AwsCredentialsProvider awsCredentialsProvider(AwsProps p) {
//        if (p.region() != null && !p.region().isBlank()) {
//            return StaticCredentialsProvider.create(
//                    AwsSessionCredentials.create(p.accessKeyId(), p.secretAccessKey(), p.region()));
//        }
//        return StaticCredentialsProvider.create(
//                AwsBasicCredentials.create(p.accessKeyId(), p.secretAccessKey()));
//    }
//
//    @Bean
//    public StsClient stsClient(AwsProps p, AwsCredentialsProvider creds) {
//        Region region = (p.region() != null && !p.region().isBlank())
//                ? Region.of(p.region())
//                : Region.US_EAST_1;
//        log.info("Using AWS region: {}", region);
//        return StsClient.builder()
//                .region(region)
//                .credentialsProvider(creds)
//                .build();
//    }
//}
