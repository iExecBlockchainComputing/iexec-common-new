package com.iexec.common.chain;

import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import static com.iexec.common.chain.ChainUtils.weiToEth;
import static com.iexec.common.contract.generated.IexecHubABILegacy.*;

@Slf4j
public abstract class Web3jAbstractService {

    private final static long GAS_LIMIT_CAP = 500000;
    private final Web3j web3j;
    private final float gasPriceMultiplier;
    private final long gasPriceCap;

    public Web3jAbstractService(String chainNodeAddress,
                                float gasPriceMultiplier,
                                long gasPriceCap) {
        this.web3j = getWeb3j(chainNodeAddress);
        this.gasPriceMultiplier = gasPriceMultiplier;
        this.gasPriceCap = gasPriceCap;
    }

    public static BigInteger getMaxTxCost(long gasPriceCap) {
        return BigInteger.valueOf(GAS_LIMIT_CAP * gasPriceCap);
    }

    private Web3j getWeb3j(String chainNodeAddress) {
        Web3j web3j = Web3j.build(new HttpService(chainNodeAddress));
        ExceptionInInitializerError exceptionInInitializerError = new ExceptionInInitializerError("Failed to connect to ethereum node " + chainNodeAddress);
        try {
            log.info("Connected to Ethereum node [address:{}, version:{}]", chainNodeAddress, web3j.web3ClientVersion().send().getWeb3ClientVersion());
            if (web3j.web3ClientVersion().send().getWeb3ClientVersion() == null) {
                throw exceptionInInitializerError;
            }
        } catch (IOException e) {
            throw exceptionInInitializerError;
        }
        return web3j;
    }

    public Web3j getWeb3j() {
        return web3j;
    }

    private EthBlock.Block getLatestBlock() throws IOException {
        return web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().getBlock();
    }

    private long getLatestBlockNumber() throws IOException {
        return getLatestBlock().getNumber().longValue();
    }

    private EthBlock.Block getBlock(long blockNumber) throws IOException {
        return web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNumber)),
                false).send().getBlock();
    }

    // check if the blockNumber is already available for the scheduler
    // blockNumber is different than 0 only for status the require a check on the blockchain, so the scheduler should
    // already have this block, otherwise it should wait for a maximum of 10 blocks.
    public boolean isBlockAvailable(long blockNumber) {
        try {
            long maxBlockNumber = blockNumber + 10;
            long currentBlockNumber = getLatestBlockNumber();
            while (currentBlockNumber <= maxBlockNumber) {
                if (blockNumber <= currentBlockNumber) {
                    return true;
                } else {
                    log.warn("Chain is NOT synchronized yet [blockNumber:{}, currentBlockNumber:{}]", blockNumber, currentBlockNumber);
                    Thread.sleep(500);
                }
                currentBlockNumber = getLatestBlockNumber();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error in checking the latest block number [execption:{}]", e.getMessage());
        }
        return false;
    }

    public long getMaxWaitingTimeWhenPendingReceipt() {
        long maxWaitingTime = 2 * 60 * 1000L; // 2min

        // max waiting Time should be roughly the time of 10 blocks
        try {
            EthBlock.Block latestBlock = getLatestBlock();

            long latestBlockNumber = latestBlock.getNumber().longValue();

            BigInteger latestBlockTimestamp = latestBlock.getTimestamp();
            BigInteger tenBlocksAgoTimestamp = getBlock(latestBlockNumber -10).getTimestamp();

            maxWaitingTime = (latestBlockTimestamp.longValue() - tenBlocksAgoTimestamp.longValue()) * 1000;

            log.info(" [latestBlockTimestamp:{}, tenBlocksAgoTimestamp:{}, maxWaitingTime:{}]",
                    latestBlockTimestamp, tenBlocksAgoTimestamp, maxWaitingTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return maxWaitingTime;
    }

    public boolean hasEnoughGas(String address) {
        Optional<BigInteger> optionalBalance = getBalance(address);
        if (!optionalBalance.isPresent()) {
            return false;
        }

        BigInteger weiBalance = optionalBalance.get();
        BigInteger estimateTxNb = weiBalance.divide(getMaxTxCost(gasPriceCap));
        BigDecimal balanceToShow = weiToEth(weiBalance);

        if (estimateTxNb.compareTo(BigInteger.ONE) < 0) {
            log.error("ETH balance is empty, please refill gas now [balance:{}, estimateTxNb:{}]", balanceToShow, estimateTxNb);
            return false;
        } else if (estimateTxNb.compareTo(BigInteger.TEN) < 0) {
            log.warn("ETH balance very low, should refill gas now [balance:{}, estimateTxNb:{}]", balanceToShow, estimateTxNb);
        } else {
            log.info("ETH balance is fine [balance:{}, estimateTxNb:{}]", balanceToShow, estimateTxNb);
        }
        return true;
    }

    public Optional<BigInteger> getBalance(String address) {
        try {
            return Optional.of(web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().getBalance());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Optional<BigInteger> getNetworkGasPrice() {
        try {
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            return Optional.of(gasPrice);
        } catch (IOException e) {
            log.error("getNetworkGasPrice failed");
            return Optional.empty();
        }
    }

    public BigInteger getUserGasPrice(float gasPriceMultiplier, long gasPriceCap) {
        Optional<BigInteger> networkGasPrice = getNetworkGasPrice();
        if (!networkGasPrice.isPresent()) {
            return BigInteger.valueOf(gasPriceCap);
        }
        long wishedGasPrice = (long) (networkGasPrice.get().floatValue() * gasPriceMultiplier);

        return BigInteger.valueOf(Math.min(wishedGasPrice, gasPriceCap));
    }

    /*
     * This is just a dummy stub for contract reader:
     * gas price & gas limit is not required when querying (read) an eth node
     *
     */
    public ContractGasProvider getReadingContractGasProvider() {
        return new ContractGasProvider() {
            @Override
            public BigInteger getGasPrice(String contractFunc) {
                return null;
            }

            @Override
            public BigInteger getGasPrice() {
                return null;
            }

            @Override
            public BigInteger getGasLimit(String contractFunc) {
                return null;
            }

            @Override
            public BigInteger getGasLimit() {
                return null;
            }
        };
    }

    public ContractGasProvider getWritingContractGasProvider() {
        return new ContractGasProvider() {

            @Override
            public BigInteger getGasPrice(String s) {
                return getUserGasPrice(gasPriceMultiplier, gasPriceCap);
            }

            @Override
            public BigInteger getGasPrice() {
                return getUserGasPrice(gasPriceMultiplier, gasPriceCap);
            }

            @Override
            public BigInteger getGasLimit(String functionName) {
                long gasLimit;
                switch (functionName) {
                    case FUNC_INITIALIZE:
                        gasLimit = 300000;//seen 176340
                        break;
                    case FUNC_CONTRIBUTE:
                        gasLimit = 500000;//seen 333541
                        break;
                    case FUNC_REVEAL:
                        gasLimit = 100000;//seen 56333
                        break;
                    case FUNC_FINALIZE:
                        gasLimit = 3000000;//seen 175369 (242641 in reopen case)
                        break;
                    case FUNC_REOPEN:
                        gasLimit = 500000;//seen 43721
                        break;
                    default:
                        gasLimit = GAS_LIMIT_CAP;
                }
                return BigInteger.valueOf(gasLimit);
            }

            @Override
            public BigInteger getGasLimit() {
                return BigInteger.valueOf(GAS_LIMIT_CAP);
            }
        };
    }


}