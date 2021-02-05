/*
 * Copyright 2020 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.common.docker.client;

public abstract class DockerClientFactory {

    private static DockerClientInstance defaultClient;
    private static DockerClientInstance authenticatedClient;

    public static synchronized DockerClientInstance get() {
        if (defaultClient == null) {
            defaultClient = new DockerClientInstance();
        }
        return defaultClient;
    }

    public static synchronized DockerClientInstance get(String username, String password) {
        if (authenticatedClient == null) {
            authenticatedClient = new DockerClientInstance(username, password);
        }
        return defaultClient;
    }
}
