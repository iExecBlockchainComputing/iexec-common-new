package io.iexec.common.chain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class DealParams {

    public static final String IPFS_RESULT_STORAGE_PROVIDER = "ipfs";
    public static final String DROPBOX_RESULT_STORAGE_PROVIDER = "dropbox";

    // Note to dev: the naming of the variables in the json file is important since it will be stored on-chain
    @JsonProperty("iexec_args")
    private String iexecArgs;

    @JsonProperty("iexec_input_files")
    private List<String> iexecInputFiles;

    @JsonProperty("iexec_developer_logger")
    private boolean iexecDeveloperLoggerEnabled;

    @JsonProperty("iexec_result_encryption")
    private boolean iexecResultEncryption;

    @JsonProperty("iexec_result_storage_provider")
    private String iexecResultStorageProvider;

    @JsonProperty("iexec_result_storage_proxy")
    private String iexecResultStorageProxy;

    //Should be set by SDK
    @JsonProperty("iexec_tee_post_compute_image")
    private String iexecTeePostComputeImage;

    //Should be set by SDK
    @JsonProperty("iexec_tee_post_compute_fingerprint")
    private String iexecTeePostComputeFingerprint;


}
