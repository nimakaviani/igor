/*
 * Copyright 2020 Armory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package com.netflix.spinnaker.igor.accounts.dynamic;

import com.netflix.spinnaker.igor.accounts.ReloadableCredentialsRepository;
import com.netflix.spinnaker.igor.accounts.Reloader;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty("accounts.dynamic.enabled")
@RequiredArgsConstructor
public class ReloadAccountRepositories {
  private final ReloadAccountRepositoryConfig config;
  private final List<ReloadableCredentialsRepository<?>> repositories;
  private final AccountSynchronizer accountSynchronizer;

  @PostConstruct
  public void setup() {
    repositories.stream()
        .filter(r -> config.getRepositories().containsKey(r.getType()))
        .forEach(
            r ->
                accountSynchronizer.schedule(
                    new Reloader<>(
                        r, config.getRepositories().get(r.getType()).getReloadFrequencyMs())));
  }
}
