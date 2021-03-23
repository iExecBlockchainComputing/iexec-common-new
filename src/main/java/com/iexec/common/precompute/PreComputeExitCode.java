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

package com.iexec.common.precompute;

/**
 * To avoid confusion with exit codes of the chroot standard
 * (followed also by docker), we use exit codes between
 * 64 and 113 which is also conform with the C/C++ standard.
 * @see https://tldp.org/LDP/abs/html/exitcodes.html
 * @see https://docs.docker.com/engine/reference/run/#exit-status
 */
public enum PreComputeExitCode {

    EMPTY_REQUIRED_ENV_VAR(64),
    INPUT_FOLDER_NOT_FOUND(65),
    DATASET_FILE_NOT_FOUND(66),
    INVALID_DATASET_CHECKSUM(67),
    INVALID_DATASET_KEY(68),
    IO_ERROR(69),
    DATASET_DECRYPTION_ERROR(70);

    private final int value;

    private PreComputeExitCode(int n) {
        this.value = n;
    }

    public int getValue() {
        return value;
    }
}
