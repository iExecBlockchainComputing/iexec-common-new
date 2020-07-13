package io.iexec.common.replicate;

import io.iexec.common.chain.ChainReceipt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplicateStatusDetails {

    public static final int MAX_STDOUT_LENGTH = 100000;

    private ChainReceipt chainReceipt;
    private String resultLink;
    private String chainCallbackData;
    private String errorMessage;
    private ReplicateStatusCause cause;
    private String stdout;

    public ReplicateStatusDetails(ReplicateStatusDetails details) {
        chainReceipt = details.getChainReceipt();
        resultLink = details.getResultLink();
        chainCallbackData = details.getChainCallbackData();
        errorMessage = details.getErrorMessage();
        cause = details.getCause();
        stdout = details.getStdout();
    }

    public ReplicateStatusDetails(long blockNumber) {
        this.chainReceipt = ChainReceipt.builder().blockNumber(blockNumber).build();
    }

    public ReplicateStatusDetails(ReplicateStatusCause cause) {
        this.cause = cause;
    }

    public ReplicateStatusDetails tailStdout() {
        if (stdout != null && stdout.length() > MAX_STDOUT_LENGTH) {
            stdout = stdout.substring(stdout.length() - MAX_STDOUT_LENGTH, stdout.length());
        }
        return this;
    }
}
