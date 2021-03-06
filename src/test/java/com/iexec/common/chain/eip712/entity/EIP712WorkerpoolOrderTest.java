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

package com.iexec.common.chain.eip712.entity;

import com.iexec.common.chain.eip712.EIP712Domain;
import com.iexec.common.sdk.order.payload.WorkerpoolOrder;
import com.iexec.common.utils.BytesUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;

import java.io.IOException;
import java.math.BigInteger;

public class EIP712WorkerpoolOrderTest {

    public static final EIP712Domain DOMAIN = new EIP712Domain(133, "0x3eca1B216A7DF1C7689aEb259fFB83ADFB894E7f");
    public static final String POOL_ADDRESS = "0x53Ef1328a96E40E125bca15b9a4da045C5e63E1A";

    /**
     * Expected signature string could also be found with:
     * <p>
     * iexec order sign --workerpool --chain 133 \
     * --keystoredir /home/$USER/iexecdev/iexec-common/src/test/resources/ \
     * --wallet-file wallet.json --password whatever
     * <p>
     * Note: Don't forget to update salt
     */
    @Test
    public void signWorkerpoolOrderEIP712() {
        EIP712WorkerpoolOrder eip712WorkerpoolOrder = new EIP712WorkerpoolOrder(
                DOMAIN,
                WorkerpoolOrder.builder()
                        .workerpool(POOL_ADDRESS)
                        .price(BigInteger.ZERO)
                        .volume(BigInteger.ONE)
                        .category(BigInteger.ZERO)
                        .trust(BigInteger.ZERO)
                        .tag("0x0000000000000000000000000000000000000000000000000000000000000000")
                        .datasetrestrict(BytesUtils.EMPTY_ADDRESS)
                        .apprestrict(BytesUtils.EMPTY_ADDRESS)
                        .requesterrestrict(BytesUtils.EMPTY_ADDRESS)
                        .salt("0x40af1a4975ca6ca7285d7738e51c8da91a9daee4a23fb45d105068be56f85e56")
                        .build()
        );

        String signatureString = eip712WorkerpoolOrder.signMessage(getWallet());
        Assertions.assertThat(signatureString)
                .isEqualTo("0x5d7c625e34c1dbfa76c6f1b953910f21d83fb51499748e2ccf15f9d357142f1c4f8b24dc583914b02c868d5a1d751409bbe83c753152cfd88fdd3ac65b39e9fe1c");
    }

    private ECKeyPair getWallet() {
        try {
            return WalletUtils.loadCredentials(
                    "whatever",
                    "./src/test/resources/wallet.json")
                    .getEcKeyPair();
        } catch (IOException | CipherException e) {
            e.printStackTrace();
        }
        return null;
    }

}