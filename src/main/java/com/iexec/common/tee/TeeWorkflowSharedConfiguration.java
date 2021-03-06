/*
 * Copyright 2021 IEXEC BLOCKCHAIN TECH
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

package com.iexec.common.tee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration of TEE workflow provided from the sms to the worker.
 * It contains:
 * - las image;
 * - pre-compute image, heap size and entrypoint;
 * - post-compute image, heap size and entrypoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeeWorkflowSharedConfiguration {

    private String lasImage;
    private String preComputeImage;
    private long preComputeHeapSize;
    private String preComputeEntrypoint;
    private String postComputeImage;
    private long postComputeHeapSize;
    private String postComputeEntrypoint;
}
