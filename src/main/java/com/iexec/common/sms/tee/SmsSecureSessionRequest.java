package com.iexec.common.sms.tee;

import com.iexec.common.security.Signature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsSecureSessionRequest {

    private String taskId;
    private String workerAddress;
    private Signature coreSignature;
    private Signature workerSignature;
}