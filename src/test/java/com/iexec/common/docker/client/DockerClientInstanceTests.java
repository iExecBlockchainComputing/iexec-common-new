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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.iexec.common.docker.DockerLogs;
import com.iexec.common.docker.DockerRunRequest;
import com.iexec.common.utils.ArgsUtils;
import com.iexec.common.utils.FileHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("slow")
public class DockerClientInstanceTests {

    private static final String CHAIN_TASK_ID = "chainTaskId";
    //classic
    private static final String DOCKER_IO_CLASSIC_IMAGE = "docker.io/alpine/socat:latest";
    private static final String SHORT_CLASSIC_IMAGE = "alpine/socat:latest";
    //library
    private static final String DOCKER_IO_LIBRARY_IMAGE = "docker.io/library/alpine:latest";
    private static final String SHORT_LIBRARY_IMAGE = "library/alpine:latest";
    private static final String VERY_SHORT_LIBRARY_IMAGE = "alpine:latest";
    // deprecated
    private static final String DOCKER_COM_CLASSIC_IMAGE = "registry.hub.docker.com/alpine/socat:latest";
    // other
    private static final String ALPINE_LATEST = "alpine:latest";
    private static final String ALPINE_BLABLA = "alpine:blabla";
    private static final String BLABLA_LATEST = "blabla:latest";
    private static final String CMD = "cmd";
    private static final List<String> ENV = List.of("FOO=bar");
    private final static String DOCKERHUB_USERNAME_ENV_NAME = "dockerhubUsername";
    private final static String DOCKERHUB_PASSWORD_ENV_NAME = "dockerhubPassword";
    private final static String PRIVATE_IMAGE_NAME =
            "sconecuratedimages/iexec:runtime-scone-3.0.0-production";
    private final static String DOCKER_NETWORK = "dockerTestsNetwork";
    private static final String NULL_DEVICE = "/dev/null";
    private static final String SLASH_TMP = "/tmp";

    private static List<String> usedRandomNames = new ArrayList<>();

    @Spy
    private DockerClientInstance dockerClientInstance = DockerClientFactory.get();

    @Spy
    private DockerClient realClient = dockerClientInstance.getClient();

    private DockerClient corruptedClient = getCorruptedDockerClient();

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterAll
    public static void afterAll() {
        // clean containers
        usedRandomNames.forEach(name -> 
                DockerClientFactory.get().stopAndRemoveContainer(name));
        // clean networks
        usedRandomNames.forEach(name -> 
                DockerClientFactory.get().removeNetwork(name));
        DockerClientFactory.get().removeNetwork(DOCKER_NETWORK);
        // TODO clean docker images
    }

    public DockerRunRequest getDefaultDockerRunRequest(boolean isSgx) {
        return DockerRunRequest.builder()
                .containerName(getRandomString())
                .chainTaskId(CHAIN_TASK_ID)
                .imageUri(ALPINE_LATEST)
                .cmd(CMD)
                .env(ENV)
                .containerPort(1000)
                .binds(Collections.singletonList(FileHelper.SLASH_IEXEC_IN +
                        ":" + FileHelper.SLASH_IEXEC_OUT))
                // .isSgx(isSgx)
                .maxExecutionTime(500000)
                .dockerNetwork(DOCKER_NETWORK)
                .workingDir(SLASH_TMP)
                .build();
    }

    /**
     * docker volume
     */

    // createVolume

    @Test
    public void shouldCreateVolume() {
        String volumeName = getRandomString();
        assertThat(dockerClientInstance.isVolumePresent(volumeName)).isFalse();
        assertThat(dockerClientInstance.createVolume(volumeName)).isTrue();
        // cleaning
        dockerClientInstance.removeVolume(volumeName);
    }

    @Test
    public void shouldNotCreateVolumeSinceEmptyName() {
        assertThat(dockerClientInstance.createVolume("")).isFalse();
    }

    @Test
    public void shouldReturnTrueSinceVolumeAlreadyPresent() {
        String volumeName = getRandomString();
        assertThat(dockerClientInstance.createVolume(volumeName)).isTrue();
        assertThat(dockerClientInstance.createVolume(volumeName)).isTrue();
        // cleaning
        dockerClientInstance.removeVolume(volumeName);
    }

    @Test
    public void shouldNotCreateVolumeSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.createVolume(getRandomString())).isFalse();
    }

    // isVolumePresent

    @Test
    public void ShouldFindVolumePresent() {
        String volumeName = getRandomString();
        dockerClientInstance.createVolume(volumeName);
        assertThat(dockerClientInstance.isVolumePresent(volumeName)).isTrue();
        // cleaning
        dockerClientInstance.removeVolume(volumeName);
    }

    @Test
    public void shouldNotFindVolumePresentSinceEmptyName() {
        assertThat(dockerClientInstance.isVolumePresent("")).isFalse();
    }

    @Test
    public void shouldNotFindVolumePresentSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.isVolumePresent(getRandomString())).isFalse();
    }

    // removeVolume

    @Test
    public void shouldRemoveVolume() {
        String volumeName = getRandomString();
        dockerClientInstance.createVolume(volumeName);
        assertThat(dockerClientInstance.removeVolume(volumeName)).isTrue();
        assertThat(dockerClientInstance.isVolumePresent(volumeName)).isFalse();
    }

    @Test
    public void shouldNotRemoveVolumeSinceEmptyName() {
        assertThat(dockerClientInstance.removeVolume("")).isFalse();
    }

    @Test
    public void shouldNotRemoveVolumeSinceDockerCmdException() {
        // create volume so isVolumePresent returns true
        String volumeName = getRandomString();
        dockerClientInstance.createVolume(volumeName);
        // useCorruptedDockerClient();

        when(realClient.removeVolumeCmd(volumeName)).thenThrow(newDockerException());
        assertThat(dockerClientInstance.removeVolume(getRandomString())).isFalse();
        
        when(realClient.removeVolumeCmd(volumeName)).thenCallRealMethod();
        dockerClientInstance.removeVolume(volumeName);
    }

    /**
     * docker network
     */

    // createNetwork

    @Test
    public void shouldCreateNetwork() {
        String networkName = getRandomString();
        String networkId = dockerClientInstance.createNetwork(networkName);
        assertThat(networkId).isNotEmpty();
        assertThat(dockerClientInstance.isNetworkPresent(networkName)).isTrue();
        // cleaning
        dockerClientInstance.removeNetwork(networkId);
    }

    @Test
    public void shouldNotCreateNetworkSinceEmptyName() {
        assertThat(dockerClientInstance.createNetwork("")).isEmpty();
    }

    @Test
    public void shouldReturnExistingNetworkIdWenNetworkIsAlreadyPresent() {
        String networkName = getRandomString();
        String networkId = dockerClientInstance.createNetwork(networkName);
        assertThat(dockerClientInstance.createNetwork(networkName)).isNotEmpty();
        assertThat(dockerClientInstance.isNetworkPresent(networkName)).isTrue();
        // cleaning
        dockerClientInstance.removeNetwork(networkId);
    }

    @Test
    public void shouldNotCreateNetworkSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.createNetwork(getRandomString())).isEmpty();
    }

    // getNetworkId

    @Test
    public void shouldGetNetworkId() {
        String networkName = getRandomString();
        String networkId = dockerClientInstance.createNetwork(networkName);
        assertThat(dockerClientInstance.getNetworkId(networkName)).isEqualTo(networkId);
        // cleaning
        dockerClientInstance.removeNetwork(networkId);
    }

    @Test
    public void shouldNotGetNetworkIdSinceEmptyName() {
        assertThat(dockerClientInstance.getNetworkId("")).isEmpty();
    }

    @Test
    public void shouldNotGetNetworkIdSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.getNetworkId(getRandomString())).isEmpty();
    }

    // isNetworkPresent

    @Test
    public void shouldFindNetworkPresent() {
        String networkName = getRandomString();
        dockerClientInstance.createNetwork(networkName);
        assertThat(dockerClientInstance.isNetworkPresent(networkName)).isTrue();
        dockerClientInstance.removeNetwork(networkName);
    }

    @Test
    public void shouldNotFineNetworkPresentSinceEmptyName() {
        assertThat(dockerClientInstance.isNetworkPresent("")).isFalse();
        
    }

    @Test
    public void shouldNotFineNetworkPresentSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.isNetworkPresent(getRandomString())).isFalse();
        
    }

    // removeNetwork

    @Test
    public void shouldRemoveNetwork() {
        String networkName = getRandomString();
        dockerClientInstance.createNetwork(networkName);
        assertThat(dockerClientInstance.isNetworkPresent(networkName)).isTrue();
        assertThat(dockerClientInstance.removeNetwork(networkName)).isTrue();
        assertThat(dockerClientInstance.isNetworkPresent(networkName)).isFalse();
    }

    @Test
    public void shouldNotRemoveNetworkSinceEmptyId() {
        assertThat(dockerClientInstance.removeNetwork("")).isFalse();
    }

    @Test
    public void shouldNotRemoveNetworkSinceDockerCmdException() {
        String networkName = getRandomString();
        dockerClientInstance.createNetwork(networkName);
        assertThat(dockerClientInstance.isNetworkPresent(networkName)).isTrue();
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.removeNetwork(networkName)).isFalse();
    }

    /**
     * docker image
     */

    // isImagePresent

    @Test
    public void shouldFindImagePresent() {
        dockerClientInstance.pullImage(ALPINE_LATEST);
        assertThat(dockerClientInstance.isImagePresent(ALPINE_LATEST)).isTrue();
        dockerClientInstance.removeImage(ALPINE_LATEST);
    }

    @Test
    public void shouldNotFindImagePresent() {
        assertThat(dockerClientInstance.isImagePresent(getRandomString())).isFalse();
    }

    @Test
    public void shouldNotFindImagePresentSinceEmptyName() {
        assertThat(dockerClientInstance.isImagePresent("")).isFalse();
    }


    @Test
    public void shouldNotFindImagePresentSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.isImagePresent("")).isFalse();
    }

    // pull image

    @Test
    public void shouldPullImage() {
        assertThat(dockerClientInstance.pullImage(ALPINE_LATEST)).isTrue();
        assertThat(dockerClientInstance.isImagePresent(ALPINE_LATEST)).isTrue();
        dockerClientInstance.removeImage(ALPINE_LATEST);
    }

    @Test
    public void shouldNotPullImageSinceNoTag() {
        assertThat(dockerClientInstance.pullImage("alpine")).isFalse();
    }

    @Test
    public void shouldNotPullImageSinceEmptyImageName() {
        assertThat(dockerClientInstance.pullImage("")).isFalse();
    }

    @Test
    public void shouldNotPullImageSinceEmptyNameButPresentTag() {
        assertThat(dockerClientInstance.pullImage(":latest")).isFalse();
    }

    @Test
    public void shouldNotPullImageSincePresentNameButEmptyTag() {
        assertThat(dockerClientInstance.pullImage("blabla:")).isFalse();
    }

    @Test
    public void shouldNotPullImageSinceWrongName() {
        assertThat(dockerClientInstance.pullImage(BLABLA_LATEST)).isFalse();
    }

    @Test
    public void shouldNotPullImageSinceWrongTag() {
        assertThat(dockerClientInstance.pullImage(ALPINE_BLABLA)).isFalse();
    }

    @Test
    public void shouldNotPullImageSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.pullImage(getRandomString())).isFalse();
    }

    /**
     * Following test will only occur if dockerhubPassword envvar is present
     */
    @Test
    public void shouldPullPrivateImage() {
        String username = getEnvValue(DOCKERHUB_USERNAME_ENV_NAME);
        String password = getEnvValue(DOCKERHUB_PASSWORD_ENV_NAME);
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            System.out.println("No dockerhub username or password found, will" +
                    " abort shouldPullPrivateImage test");
            return;
        }
        // Get an authenticated docker client
        DockerClientInstance authClientInstance = DockerClientFactory.get(username, password);
        // clean to avoid previous tests collisions
        authClientInstance.removeImage(PRIVATE_IMAGE_NAME);
        // pull image and check
        assertThat(dockerClientInstance.pullImage(PRIVATE_IMAGE_NAME)).isTrue();
        // clean
        dockerClientInstance.removeImage(PRIVATE_IMAGE_NAME);
    }

    private String getEnvValue(String envVarName) {
        return System.getenv(envVarName) != null ?
                //Intellij envvar injection
                System.getenv(envVarName) :
                //gradle test -DdockerhubPassword=xxx
                System.getProperty(envVarName);
    }

    @Test
    public void shouldFailToPullPrivateImageWithWrongCredentials() {
        // Get an authenticated docker client
        DockerClientInstance authClientInstance = DockerClientFactory
                .get("dummyUsername", "dummyPassword");
        assertThat(authClientInstance.pullImage(PRIVATE_IMAGE_NAME)).isFalse();
    }

    // getImageId

    @Test
    public void shouldGetImageId() {
        dockerClientInstance.pullImage(ALPINE_LATEST);
        assertThat(dockerClientInstance.getImageId(ALPINE_LATEST)).isNotEmpty();
    }

    @Test
    public void shouldGetImageIdWithDockerIoClassicImage() {
        String image = DOCKER_IO_CLASSIC_IMAGE;
        dockerClientInstance.pullImage(image);
        assertThat(dockerClientInstance.getImageId(image)).isNotEmpty();
    }

    @Test
    public void shouldGetImageIdWithShortClassicImage() {
        String image = SHORT_CLASSIC_IMAGE;
        dockerClientInstance.pullImage(image);
        assertThat(dockerClientInstance.getImageId(image)).isNotEmpty();
    }

    @Test
    public void shouldGetImageIdWithDockerIoLibraryImage() {
        String image = DOCKER_IO_LIBRARY_IMAGE;
        dockerClientInstance.pullImage(image);
        assertThat(dockerClientInstance.getImageId(image)).isNotEmpty();
    }

    @Test
    public void shouldGetImageIdWithShortLibraryImage() {
        String image = SHORT_LIBRARY_IMAGE;
        dockerClientInstance.pullImage(image);
        assertThat(dockerClientInstance.getImageId(image)).isNotEmpty();
    }

    @Test
    public void shouldGetImageIdWithVeryShortLibraryImage() {
        String image = VERY_SHORT_LIBRARY_IMAGE;
        dockerClientInstance.pullImage(image);
        assertThat(dockerClientInstance.getImageId(image)).isNotEmpty();
    }

    @Test
    public void shouldGetImageIdWithClassicDockerComImage() {
        String image = DOCKER_COM_CLASSIC_IMAGE;
        dockerClientInstance.pullImage(image);
        String imageId = dockerClientInstance.getImageId(image);
        assertThat(imageId).isNotEmpty();
    }

    @Test
    public void shouldNotGetImageIdSinceEmptyName() {
        assertThat(dockerClientInstance.getImageId("")).isEmpty();
    }

    @Test
    public void shouldNotGetImageId() {
        assertThat(dockerClientInstance.getImageId(BLABLA_LATEST)).isEmpty();
    }

    @Test
    public void shouldNotGetImageIdSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.getImageId(getRandomString())).isEmpty();
    }

    // sanitizeImageName

    @Test
    public void shouldGetSanitizedImageWithDockerIoClassicImage() {
        assertThat(dockerClientInstance.sanitizeImageName(DOCKER_IO_CLASSIC_IMAGE))
                .isEqualTo("alpine/socat:latest");
    }

    @Test
    public void shouldGetSanitizedImageWithDockerComClassicImage() {
        assertThat(dockerClientInstance.sanitizeImageName(DOCKER_COM_CLASSIC_IMAGE))
                .isEqualTo("alpine/socat:latest");
    }

    @Test
    public void shouldGetSanitizedImageWithDockerIoLibraryImage() {
        assertThat(dockerClientInstance.sanitizeImageName(DOCKER_IO_LIBRARY_IMAGE))
                .isEqualTo("alpine:latest");
    }

    @Test
    public void shouldGetSanitizedImageWithShortLibraryImage() {
        assertThat(dockerClientInstance.sanitizeImageName(SHORT_LIBRARY_IMAGE))
                .isEqualTo("alpine:latest");
    }

    @Test
    public void shouldGetSanitizedImageIfShortNameLibraryName() {
        assertThat(dockerClientInstance.sanitizeImageName(VERY_SHORT_LIBRARY_IMAGE))
                .isEqualTo("alpine:latest");
    }

    @Test
    public void shouldDoNothingForSanitizedImage() {
        String image = "nexus.iex.ec/some-app:latest";
        assertThat(dockerClientInstance.sanitizeImageName(image))
                .isEqualTo(image);
    }

    // Remove image

    @Test
    public void shouldRemoveImage() {
        dockerClientInstance.pullImage(DOCKER_IO_CLASSIC_IMAGE);
        assertThat(dockerClientInstance.removeImage(DOCKER_IO_CLASSIC_IMAGE)).isTrue();
    }

    @Test
    public void shouldRemoveImageByIdSinceEmptyName() {
        assertThat(dockerClientInstance.removeImage("")).isFalse();
    }

    @Test
    public void shouldNotRemoveImageByIdSinceDockerCmdException() {
        dockerClientInstance.pullImage(ALPINE_LATEST);
        dockerClientInstance.getImageId(ALPINE_LATEST);

        useCorruptedDockerClient();
        assertThat(dockerClientInstance.removeImage(ALPINE_LATEST)).isFalse();

        // cleaning
        dockerClientInstance.removeImage(ALPINE_LATEST);
    }

    /**
     * docker container
     */

    // docker run
    // TODO

    // createContainer

    @Test
    public void shouldCreateContainer() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        dockerClientInstance.pullImage(request.getImageUri());
        String containerId = dockerClientInstance.createContainer(request);
        assertThat(containerId).isNotEmpty();
        // cleaning
        dockerClientInstance.removeContainer(request.getContainerName());
        dockerClientInstance.removeImage(request.getImageUri());
    }

    @Test
    public void shouldNotCreateContainerSinceNoRequest() {
        assertThat(dockerClientInstance.createContainer(null)).isEmpty();
    }

    @Test
    public void shouldNotCreateContainerSinceEmptyContainerName() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        request.setContainerName("");
        assertThat(dockerClientInstance.createContainer(request)).isEmpty();
    }

    @Test
    public void shouldNotCreateContainerSinceEmptyImageUri() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        request.setImageUri("");
        assertThat(dockerClientInstance.createContainer(request)).isEmpty();
    }

    @Test
    public void shouldNotCreateContainerSinceDockerCmdException() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.createContainer(request)).isEmpty();
    }

    @Test
    public void shouldCreateContainerAndRemoveExistingDuplicate() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        dockerClientInstance.pullImage(request.getImageUri());
        // create first container
        String container1Id = dockerClientInstance.createContainer(request);
        // create second container with same name (should replace previous one)
        String container2Id = dockerClientInstance.createContainer(request);
        assertThat(container2Id).isNotEmpty();
        assertThat(container2Id).isNotEqualTo(container1Id);
        // cleaning
        dockerClientInstance.removeContainer(container2Id);
        dockerClientInstance.removeImage(request.getImageUri());
    }

    @Test
    public void shouldNotCreateContainerSinceDuplicateIsPresent() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        dockerClientInstance.pullImage(request.getImageUri());
        // create first container
        String container1Id = dockerClientInstance.createContainer(request);
        // create second container with same name (should not replace previous one)
        String container2Id = dockerClientInstance.createContainer(request, false);
        assertThat(container1Id).isNotEmpty();
        assertThat(container2Id).isEmpty();
        // cleaning
        dockerClientInstance.removeContainer(container1Id);
        dockerClientInstance.removeImage(request.getImageUri());
    }

    // buildHostConfigFromRunRequest

    @Test
    public void shouldBuildHostConfigFromRunRequest() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);

        HostConfig hostConfig =
                dockerClientInstance.buildHostConfigFromRunRequest(request);
        assertThat(hostConfig.getNetworkMode())
                .isEqualTo(DOCKER_NETWORK);
        assertThat((hostConfig.getBinds()[0].getPath()))
                .isEqualTo(FileHelper.SLASH_IEXEC_IN);
        assertThat((hostConfig.getBinds()[0].getVolume().getPath()))
                .isEqualTo(FileHelper.SLASH_IEXEC_OUT);
        assertThat(hostConfig.getDevices()).isNull();
    }

    @Test
    public void shouldBuildHostConfigWithDeviceFromRunRequest() {
        DockerRunRequest request = getDefaultDockerRunRequest(true);
        String device = NULL_DEVICE + ":" + NULL_DEVICE;
        request.setDevices(new ArrayList<>());
        request.getDevices().add(device);

        HostConfig hostConfig =
                dockerClientInstance.buildHostConfigFromRunRequest(request);
        assertThat(hostConfig.getNetworkMode())
                .isEqualTo(DOCKER_NETWORK);
        assertThat((hostConfig.getBinds()[0].getPath()))
                .isEqualTo(FileHelper.SLASH_IEXEC_IN);
        assertThat((hostConfig.getBinds()[0].getVolume().getPath()))
                .isEqualTo(FileHelper.SLASH_IEXEC_OUT);
        assertThat(hostConfig.getDevices()).isNotNull();
        assertThat(hostConfig.getDevices()[0].getPathInContainer())
                .isEqualTo(NULL_DEVICE);
        assertThat(hostConfig.getDevices()[0].getPathOnHost())
                .isEqualTo(NULL_DEVICE);
    }

    @Test
    public void shouldNotbuildHostConfigFromRunRequestSinceNoRequest() {
        HostConfig hostConfig =
                dockerClientInstance.buildHostConfigFromRunRequest(null);
        assertThat(hostConfig).isNull();
    }

    @Test
    public void shouldbuildCreateContainerCmdFromRunRequest() {
        CreateContainerCmd createContainerCmd = dockerClientInstance.getClient()
                .createContainerCmd("repo/image:tag");
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        request.setCmd("");
        request.setEnv(null);
        request.setContainerPort(0);

        Optional<CreateContainerCmd> oActualCreateContainerCmd =
        dockerClientInstance.buildCreateContainerCmdFromRunRequest(request,
                createContainerCmd);
        assertThat(oActualCreateContainerCmd).isPresent();
        CreateContainerCmd actualCreateContainerCmd = oActualCreateContainerCmd.get();
        assertThat(actualCreateContainerCmd.getName())
                .isEqualTo(request.getContainerName());
        assertThat(actualCreateContainerCmd.getHostConfig())
                .isEqualTo(dockerClientInstance.buildHostConfigFromRunRequest(request));
        assertThat(actualCreateContainerCmd.getCmd()).isNull();
        assertThat(actualCreateContainerCmd.getEnv()).isNull();
        assertThat(actualCreateContainerCmd.getExposedPorts()).isEmpty();
    }

    @Test
    public void shouldbuildCreateContainerCmdFromRunRequestWithFullParams() {
        CreateContainerCmd createContainerCmd = dockerClientInstance.getClient()
                .createContainerCmd("repo/image:tag");
        DockerRunRequest request = getDefaultDockerRunRequest(false);

        Optional<CreateContainerCmd> oActualCreateContainerCmd =
                dockerClientInstance.buildCreateContainerCmdFromRunRequest(request,
                        createContainerCmd);
        assertThat(oActualCreateContainerCmd).isPresent();
        CreateContainerCmd actualCreateContainerCmd = oActualCreateContainerCmd.get();
        assertThat(actualCreateContainerCmd.getName())
                .isEqualTo(request.getContainerName());
        assertThat(actualCreateContainerCmd.getHostConfig())
                .isEqualTo(dockerClientInstance.buildHostConfigFromRunRequest(request));
        assertThat(actualCreateContainerCmd.getCmd())
                .isEqualTo(ArgsUtils.stringArgsToArrayArgs(request.getCmd()));
        assertThat(actualCreateContainerCmd.getEnv()).isNotNull();
        assertThat(Arrays.asList(actualCreateContainerCmd.getEnv()))
                .isEqualTo(request.getEnv());
        assertThat(actualCreateContainerCmd.getExposedPorts()).isNotNull();
        assertThat(actualCreateContainerCmd.getExposedPorts()[0].getPort())
                .isEqualTo(1000);
        assertThat(actualCreateContainerCmd.getWorkingDir()).isEqualTo(SLASH_TMP);        
    }

    @Test
    public void shouldNotbuildCreateContainerCmdFromRunRequestSinceNoRequest() {
        Optional<CreateContainerCmd> actualCreateContainerCmd =
                dockerClientInstance.buildCreateContainerCmdFromRunRequest(
                        getDefaultDockerRunRequest(false),
                        null);
        assertThat(actualCreateContainerCmd).isEmpty();
    }

    @Test
    public void shouldNotbuildCreateContainerCmdFromRunRequestSinceNoCreateContainerCmd() {
        Optional<CreateContainerCmd> actualCreateContainerCmd =
                dockerClientInstance.buildCreateContainerCmdFromRunRequest(
                        null,
                        dockerClientInstance.getClient()
                                .createContainerCmd("repo/image:tag")
                        );
        assertThat(actualCreateContainerCmd).isEmpty();
    }

    // getContainerName

    @Test
    public void shouldGetContainerName() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerId = dockerClientInstance.createContainer(request);

        assertThat(dockerClientInstance.getContainerName(containerId))
                .isEqualTo(request.getContainerName());

        // cleaning
        dockerClientInstance.removeContainer(request.getContainerName());
    }

    @Test
    public void shouldNotGetContainerNameSinceEmptyId() {
        assertThat(dockerClientInstance.getContainerName("")).isEmpty();
    }

    @Test
    public void shouldNotGetContainerNameSinceNoContainer() {
        assertThat(dockerClientInstance.getContainerName(getRandomString())).isEmpty();
    }

    @Test
    public void shouldNotGetContainerNameSinceDockerCmdException() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerId = dockerClientInstance.createContainer(request);

        useCorruptedDockerClient();
        assertThat(dockerClientInstance.getContainerName(containerId)).isEmpty();

        // cleaning
        useRealDockerClient();
        dockerClientInstance.removeContainer(request.getContainerName());
    }

    // getContainerId

    @Test
    public void shouldGetContainerId() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        pullImageIfNecessary();
        String expectedId = dockerClientInstance.createContainer(request);

        String containerId =
                dockerClientInstance.getContainerId(request.getContainerName());
        assertThat(containerId).isNotEmpty();
        assertThat(containerId).isEqualTo(expectedId);

        // cleaning
        dockerClientInstance.removeContainer(request.getContainerName());
    }

    @Test
    public void shouldNotGetContainerIdSinceEmptyId() {
        assertThat(dockerClientInstance.getContainerId("")).isEmpty();
    }

    @Test
    public void shouldNotGetContainerIdSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.getContainerId(getRandomString())).isEmpty();
    }

    // getContainerStatus

    @Test
    public void shouldGetContainerStatus() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        pullImageIfNecessary();
        dockerClientInstance.createContainer(request);

        assertThat(dockerClientInstance.getContainerStatus(request.getContainerName()))
                .isEqualTo(DockerClientInstance.CREATED_STATUS);

        // cleaning
        dockerClientInstance.removeContainer(request.getContainerName());
    }

    @Test
    public void shouldNotGetContainerStatusSinceEmptyId() {
        assertThat(dockerClientInstance.getContainerStatus("")).isEmpty();
    }

    @Test
    public void shouldNotGetContainerStatusSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.getContainerStatus(getRandomString())).isEmpty();
    }

    // start container

    @Test
    public void shouldStartContainer() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerName = request.getContainerName();
        request.setCmd("sh -c 'sleep 1 && echo Hello from Docker alpine!'");
        pullImageIfNecessary();
        dockerClientInstance.createContainer(request);

        assertThat(dockerClientInstance.startContainer(containerName)).isTrue();
        assertThat(dockerClientInstance.getContainerStatus(containerName))
                .isEqualTo(DockerClientInstance.RUNNING_STATUS);

        // cleaning
        dockerClientInstance.stopContainer(containerName);
        dockerClientInstance.removeContainer(containerName);
    }

    @Test
    public void shouldNotStartContainerNameSinceEmptyId() {
        assertThat(dockerClientInstance.startContainer("")).isFalse();
    }

    @Test
    public void shouldNotStartContainerSinceDockerCmdException() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerName = request.getContainerName();
        request.setCmd("sh -c 'sleep 1 && echo Hello from Docker alpine!'");
        dockerClientInstance.createContainer(request);

        useCorruptedDockerClient();
        assertThat(dockerClientInstance.startContainer(containerName)).isFalse();

        // cleaning
        useRealDockerClient();
        dockerClientInstance.stopContainer(containerName);
        dockerClientInstance.removeContainer(containerName);
    }

    // waitContainerUntilExitOrTimeout
    @Test
    public void shouldTimeoutAfterWaitContainerUntilExitOrTimeout() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerName = request.getContainerName();
        request.setCmd("sh -c 'sleep 30 && echo Hello from Docker alpine!'");
        pullImageIfNecessary();
        dockerClientInstance.createContainer(request);
        dockerClientInstance.startContainer(containerName);
        assertThat(dockerClientInstance.getContainerStatus(containerName))
                .isEqualTo(DockerClientInstance.RUNNING_STATUS);
        Date before = new Date();
        Long exitCode = dockerClientInstance.waitContainerUntilExitOrTimeout(containerName,
                Instant.now().plusSeconds(5));
        assertThat(dockerClientInstance.getContainerStatus(containerName))
                .isEqualTo(DockerClientInstance.RUNNING_STATUS);
        assertThat(exitCode).isNull();
        assertThat(new Date().getTime() - before.getTime()).isGreaterThan(1000);
        // cleaning
        dockerClientInstance.stopContainer(containerName);
        dockerClientInstance.removeContainer(containerName);
    }

    @Test
    public void shouldWaitContainerUntilExitOrTimeoutSinceExited() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerName = request.getContainerName();
        request.setCmd("sh -c 'sleep 1 && echo Hello from Docker alpine!'");
        dockerClientInstance.createContainer(request);
        dockerClientInstance.startContainer(containerName);
        assertThat(dockerClientInstance.getContainerStatus(containerName))
                .isEqualTo(DockerClientInstance.RUNNING_STATUS);
        dockerClientInstance.waitContainerUntilExitOrTimeout(containerName,
                Instant.now().plusMillis(3000));
        assertThat(dockerClientInstance.getContainerStatus(containerName))
                .isEqualTo(DockerClientInstance.EXITED_STATUS);

        // cleaning
        dockerClientInstance.stopContainer(containerName);
        dockerClientInstance.removeContainer(containerName);
    }

    // getContainerLogs
    @Test
    public void shouldGetContainerLogsSinceStdout() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        request.setCmd("sh -c 'echo Hello from Docker alpine!'");
        pullImageIfNecessary();
        dockerClientInstance.createContainer(request);
        dockerClientInstance.startContainer(request.getContainerName());

        Optional<DockerLogs> containerLogs =
                dockerClientInstance.getContainerLogs(request.getContainerName());
        assertThat(containerLogs).isPresent();
        assertThat(containerLogs.get().getStdout()).contains("Hello from " +
                "Docker alpine!");
        assertThat(containerLogs.get().getStderr()).isEmpty();

        // cleaning
        dockerClientInstance.stopContainer(request.getContainerName());
        dockerClientInstance.removeContainer(request.getContainerName());
    }

    @Test
    public void shouldGetContainerLogsSinceStderr() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        request.setCmd("sh -c 'echo Hello from Docker alpine! >&2'");
        pullImageIfNecessary();
        dockerClientInstance.createContainer(request);
        dockerClientInstance.startContainer(request.getContainerName());

        Optional<DockerLogs> containerLogs =
                dockerClientInstance.getContainerLogs(request.getContainerName());
        assertThat(containerLogs).isPresent();
        assertThat(containerLogs.get().getStdout()).isEmpty();
        assertThat(containerLogs.get().getStderr()).contains("Hello from " +
                "Docker alpine!");

        // cleaning
        dockerClientInstance.stopContainer(request.getContainerName());
        dockerClientInstance.removeContainer(request.getContainerName());
    }

    @Test
    public void shouldNotGetContainerLogsSinceEmptyId() {
        assertThat(dockerClientInstance.getContainerLogs("")).isEmpty();
    }

    @Test
    public void shouldNotGetContainerLogsSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.getContainerLogs(getRandomString())).isEmpty();
    }

    // stopContainer
    @Test
    public void shouldStopContainer() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerName = request.getContainerName();
        request.setCmd("sh -c 'sleep 1 && echo Hello from Docker alpine!'");
        pullImageIfNecessary();
        dockerClientInstance.createContainer(request);
        dockerClientInstance.startContainer(containerName);

        assertThat(dockerClientInstance.getContainerStatus(containerName))
                .isEqualTo(DockerClientInstance.RUNNING_STATUS);
        assertThat(dockerClientInstance.stopContainer(containerName)).isTrue();
        assertThat(dockerClientInstance.getContainerStatus(containerName))
                .isEqualTo(DockerClientInstance.EXITED_STATUS);

        // cleaning
        dockerClientInstance.removeContainer(containerName);
    }

    @Test
    public void shouldNotStopContainerSinceEmptyId() {
        assertThat(dockerClientInstance.stopContainer("")).isFalse();
    }

    // removeContainer

    @Test
    public void shouldRemoveContainer() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerName = request.getContainerName();
        request.setCmd("sh -c 'sleep 1 && echo Hello from Docker alpine!'");
        dockerClientInstance.createContainer(request);
        dockerClientInstance.startContainer(containerName);
        dockerClientInstance.stopContainer(containerName);

        assertThat(dockerClientInstance.removeContainer(containerName)).isTrue();
    }

    @Test
    public void shouldNotRemoveContainerSinceEmptyId() {
        assertThat(dockerClientInstance.removeContainer("")).isFalse();
    }

    @Test
    public void shouldNotRemoveContainerSinceRunning() {
        DockerRunRequest request = getDefaultDockerRunRequest(false);
        String containerName = request.getContainerName();
        request.setCmd("sh -c 'sleep 5 && echo Hello from Docker alpine!'");
        pullImageIfNecessary();
        dockerClientInstance.createContainer(request);
        dockerClientInstance.startContainer(containerName);

        assertThat(dockerClientInstance.getContainerStatus(containerName))
                .isEqualTo(DockerClientInstance.RUNNING_STATUS);
        assertThat(dockerClientInstance.removeContainer(containerName)).isFalse();

        // cleaning
        dockerClientInstance.waitContainerUntilExitOrTimeout(containerName,
                Instant.now().plusMillis(15000));
        dockerClientInstance.removeContainer(containerName);
    }

    @Test
    public void shouldNotRemoveContainerSinceDockerCmdException() {
        useCorruptedDockerClient();
        assertThat(dockerClientInstance.removeContainer(getRandomString())).isFalse();
    }

    private String getRandomString() {
        String random = RandomStringUtils.randomAlphanumeric(20);
        usedRandomNames.add(random);
        return random;
    }

    private void useRealDockerClient() {
        when(dockerClientInstance.getClient()).thenCallRealMethod();
    }

    private void useCorruptedDockerClient() {
        when(dockerClientInstance.getClient()).thenReturn(corruptedClient);
    }

    private DockerClient getCorruptedDockerClient() {
        DockerClientConfig config =
                DefaultDockerClientConfig.createDefaultConfigBuilder()
                        .withDockerHost("tcp://localhost:11111")
                        .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    private void pullImageIfNecessary() {
        if (dockerClientInstance.getImageId(ALPINE_LATEST).isEmpty()){
            dockerClientInstance.pullImage(ALPINE_LATEST);
        }
    }

    private DockerException newDockerException() {
        return new DockerException("Test exception", -1);
    }

}