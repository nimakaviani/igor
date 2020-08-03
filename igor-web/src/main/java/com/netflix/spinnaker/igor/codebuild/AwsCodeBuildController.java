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

package com.netflix.spinnaker.igor.codebuild;

import com.amazonaws.services.codebuild.model.Build;
import com.amazonaws.services.codebuild.model.StartBuildRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.igor.accounts.CredentialsRepository;
import com.netflix.spinnaker.kork.artifacts.model.Artifact;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@ConditionalOnProperty("codebuild.enabled")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/codebuild")
public class AwsCodeBuildController {
  private final CredentialsRepository<AwsCodeBuildCredentials> awsCodeBuildAccountRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @RequestMapping(value = "/accounts", method = RequestMethod.GET)
  List<String> getAccounts() {
    return awsCodeBuildAccountRepository.getAll().stream().map(AwsCodeBuildCredentials::getName).collect(Collectors.toList());
  }

  @RequestMapping(value = "/projects/{account}", method = RequestMethod.GET)
  List<String> getProjects(@PathVariable String account) {
    return awsCodeBuildAccountRepository.getOne(account).getProjects();
  }

  @RequestMapping(
      value = "/builds/start/{account}",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  Build startBuild(@PathVariable String account, @RequestBody Map<String, Object> requestBody) {
    return awsCodeBuildAccountRepository
        .getOne(account)
        .startBuild(objectMapper.convertValue(requestBody, StartBuildRequest.class));
  }

  @RequestMapping(value = "/builds/{account}/{buildId}", method = RequestMethod.GET)
  Build getBuild(@PathVariable String account, @PathVariable String buildId) {
    return awsCodeBuildAccountRepository.getOne(account).getBuild(buildId);
  }

  @RequestMapping(value = "/builds/artifacts/{account}/{buildId}", method = RequestMethod.GET)
  List<Artifact> getArtifacts(@PathVariable String account, @PathVariable String buildId) {
    return awsCodeBuildAccountRepository.getOne(account).getArtifacts(buildId);
  }

  @RequestMapping(value = "/builds/stop/{account}/{buildId}", method = RequestMethod.POST)
  Build stopBuild(@PathVariable String account, @PathVariable String buildId) {
    return awsCodeBuildAccountRepository.getOne(account).stopBuild(buildId);
  }
}
