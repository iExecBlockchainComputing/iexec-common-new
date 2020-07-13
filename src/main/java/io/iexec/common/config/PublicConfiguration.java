package io.iexec.common.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicConfiguration {

    // blockchain
    private Integer chainId;
    private String blockchainURL;
    private String iexecHubAddress;
    private String workerPoolAddress;
    private String schedulerPublicAddress;
    private boolean isSidechain;

    // repos URLs
    private String resultRepositoryURL;

    // address of the SMS
    private String smsURL;
    private String sconeCasURL;

    private long askForReplicatePeriod;
    private String requiredWorkerVersion;
}

