package com.iexec.common.chain;

import com.iexec.common.utils.BytesUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Arrays;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.crypto.Hash;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ChainUtils {

    private ChainUtils() {
        throw new UnsupportedOperationException();
    }

    public static String generateChainTaskId(String dealId, BigInteger taskIndex) {
        byte[] dealIdBytes32 = BytesUtils.stringToBytes(dealId);
        if (dealIdBytes32.length != 32) {
            return null;
        }
        byte[] taskIndexBytes32 = Numeric.toBytesPadded(taskIndex, 32);
        if (taskIndexBytes32.length != 32) {
            return null;
        }
        //concatenate bytes with same size only
        byte[] concatenate = Arrays.concatenate(dealIdBytes32, taskIndexBytes32);
        return Hash.sha3(BytesUtils.bytesToString(concatenate));
    }


    public static BigDecimal weiToEth(BigInteger weiAmount) {
        return Convert.fromWei(weiAmount.toString(), Convert.Unit.ETHER);
    }

    public static ChainReceipt buildChainReceipt(Log chainResponseLog, String chainTaskId, long lastBlock) {
        if (chainResponseLog == null) {
            log.error("Transaction log received but was null [chainTaskId:{}]", chainTaskId);
            return null;
        }

        BigInteger txBlockNumber = chainResponseLog.getBlockNumber();
        String txHash = chainResponseLog.getTransactionHash();

        ChainReceipt.ChainReceiptBuilder builder = ChainReceipt.builder();
        // it seems response.log.getBlockNumber() could be null (issue in https://github.com/web3j/web3j should be opened)
        if (txHash != null) {
            builder.txHash(txHash);
        }

        if (txBlockNumber != null) {
            builder.blockNumber(txBlockNumber.longValue());
        } else {
            log.warn("Transaction log received but blockNumber is null inside (lastBlock will be used instead) "
                    + "[chainTaskId:{}, receiptLog:{}, lastBlock:{}]", chainTaskId, chainResponseLog.toString(), lastBlock);
            builder.blockNumber(lastBlock);
        }

        return builder.build();
    }

    public static DynamicArray<Address> fromListString2DynamicArrayAddress(List<String> contributors) {
        return new DynamicArray<Address>(
                Address.class,
                org.web3j.abi.Utils.typeMap(contributors, Address.class));
    }

    public static List<String> fromListAddress2ListString(List<Address> addresses) {
        List<String> strings = new ArrayList<>();
        for (Address address: addresses){
            strings.add(address.getValue());
        }
        return strings;
    }

}
