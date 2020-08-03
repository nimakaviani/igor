/*
 * Copyright 2020 Amazon.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.igor.config;

import com.amazonaws.auth.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.netflix.spinnaker.igor.accounts.AccountRepositoryDescriptor;
import com.netflix.spinnaker.igor.accounts.CredentialsRepository;
import com.netflix.spinnaker.igor.accounts.security.CredentialsProvider;
import com.netflix.spinnaker.igor.accounts.security.TypedAccountCredentialsProvider;
import com.netflix.spinnaker.igor.codebuild.AwsCodeBuildCredentials;

import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@ConditionalOnProperty("codebuild.enabled")
@EnableConfigurationProperties({AwsCodeBuildProperties.class})
public class AwsCodeBuildConfig {
  @Bean("awsCodeBuildAccountRepository")
  @DependsOn({"awsSecurityTokenServiceClient"})
  @ConditionalOnMissingBean(
    value = AwsCodeBuildCredentials.class,
    parameterizedContainer = CredentialsRepository.class
  )
  CredentialsRepository<AwsCodeBuildCredentials> awsCodeBuildAccountRepository(
      AwsCodeBuildProperties awsCodeBuildProperties,
      AWSSecurityTokenServiceClient awsSecurityTokenServiceClient,
      AWSCredentialsProvider awsCredentialsProvider) {
    return AccountRepositoryDescriptor
      .<AwsCodeBuildCredentials, AwsCodeBuildProperties.AwsCodeBuildAccount>builder()
      .type(AWSCredentialsProvider.class.getName())
      .springAccountSource(awsCodeBuildProperties::getAccounts)
      .parser(
        a -> {
          AwsCodeBuildCredentials account =
            new AwsCodeBuildCredentials(awsCredentialsProvider, a.getRegion(), a.getName());
          if (a.getAccountId() != null && a.getAssumeRole() != null) {
            STSAssumeRoleSessionCredentialsProvider stsAssumeRoleSessionCredentialsProvider =
              new STSAssumeRoleSessionCredentialsProvider.Builder(
                getRoleArn(a.getAccountId(), a.getAssumeRole()), "spinnaker-session")
                .withStsClient(awsSecurityTokenServiceClient)
                .build();
            account =
              new AwsCodeBuildCredentials(stsAssumeRoleSessionCredentialsProvider, a.getRegion(), a.getName());
          }
          return account;
        }
      )
      .build()
      .createRepository();

//    AwsCodeBuildAccountRepository accounts = new AwsCodeBuildAccountRepository();
//    awsCodeBuildProperties
//        .getAccounts()
//        .forEach(
//            a -> {
//              AwsCodeBuildAccount account =
//                  new AwsCodeBuildAccount(awsCredentialsProvider, a.getRegion());
//              if (a.getAccountId() != null && a.getAssumeRole() != null) {
//                STSAssumeRoleSessionCredentialsProvider stsAssumeRoleSessionCredentialsProvider =
//                    new STSAssumeRoleSessionCredentialsProvider.Builder(
//                            getRoleArn(a.getAccountId(), a.getAssumeRole()), "spinnaker-session")
//                        .withStsClient(awsSecurityTokenServiceClient)
//                        .build();
//                account =
//                    new AwsCodeBuildAccount(stsAssumeRoleSessionCredentialsProvider, a.getRegion());
//              }
//              accounts.addAccount(a.getName(), account);
//            });
//    return accounts;
  }

  @Bean("awsSecurityTokenServiceClient")
  @DependsOn({"awsCredentialsProvider"})
  AWSSecurityTokenServiceClient awsSecurityTokenServiceClient(
      AWSCredentialsProvider awsCredentialsProvider) {
    return (AWSSecurityTokenServiceClient)
        AWSSecurityTokenServiceClientBuilder.standard()
            .withCredentials(awsCredentialsProvider)
            .build();
  }

  @Bean("awsCredentialsProvider")
//  CredentialsProvider<AwsCodeBuildCredentials> awsCredentialsProvider(CredentialsRepository<AwsCodeBuildCredentials> repository) {
  AWSCredentialsProvider awsCredentialsProvider(AwsCodeBuildProperties awsCodeBuildProperties) {
    AWSCredentialsProvider credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
    if (awsCodeBuildProperties.getAccessKeyId() != null
        && !awsCodeBuildProperties.getAccessKeyId().isEmpty()
        && awsCodeBuildProperties.getSecretAccessKey() != null
        && !awsCodeBuildProperties.getSecretAccessKey().isEmpty()) {
      credentialsProvider =
          new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(
                  awsCodeBuildProperties.getAccessKeyId(),
                  awsCodeBuildProperties.getSecretAccessKey()));
    }
    return credentialsProvider;
  }

  private String getRoleArn(String accountId, String assumeRole) {
    String assumeRoleValue = Objects.requireNonNull(assumeRole, "assumeRole");
    if (!assumeRoleValue.startsWith("arn:")) {
      /**
       * GovCloud and China regions need to have the full arn passed because of differing formats
       * Govcloud: arn:aws-us-gov:iam China: arn:aws-cn:iam
       */
      assumeRoleValue =
          String.format(
              "arn:aws:iam::%s:%s",
              Objects.requireNonNull(accountId, "accountId"), assumeRoleValue);
    }
    return assumeRoleValue;
  }
}
