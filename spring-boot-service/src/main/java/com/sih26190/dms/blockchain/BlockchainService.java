package com.sih26190.dms.blockchain;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;

import jakarta.annotation.PostConstruct;

/**
 * Deploys the DocumentRegistry contract (if no address is already
 * configured) and calls storeDocumentHash / getDocumentRecord on it,
 * using Web3j's low level ABI encode/decode APIs directly rather than
 * a generated wrapper class. web3j-maven-plugin's code-generation step
 * currently fails against a change in the remote solc version list it
 * fetches at build time (an upstream data format issue, not fixable
 * from this project), so this hand-written version avoids that broken
 * tooling entirely.
 */
@Service
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final RawTransactionManager transactionManager;

    private String contractAddress;

    public BlockchainService(Web3j web3j, Credentials credentials, ContractGasProvider gasProvider,
                              @Value("${blockchain.contract-address:}") String configuredAddress) throws Exception {
        this.web3j = web3j;
        this.credentials = credentials;
        this.gasProvider = gasProvider;
        long chainId = web3j.ethChainId().send().getChainId().longValue();
        this.transactionManager = new RawTransactionManager(web3j, credentials, chainId);
        this.contractAddress = configuredAddress;
    }

    @PostConstruct
    public void init() throws Exception {
        if (contractAddress == null || contractAddress.isBlank()) {
            contractAddress = deployContract();
            System.out.println("DocumentRegistry deployed at: " + contractAddress);
            System.out.println("Set blockchain.contract-address=" + contractAddress
                    + " in application.properties to reuse this deployment next time.");
        }
    }

    private String deployContract() throws Exception {
        String data = Numeric.prependHexPrefix(readContractBinary());

        EthSendTransaction sendResponse = transactionManager.sendTransaction(
                gasProvider.getGasPrice(), gasProvider.getGasLimit(), null, data, BigInteger.ZERO);

        if (sendResponse.hasError()) {
            throw new RuntimeException("Contract deployment failed: " + sendResponse.getError().getMessage());
        }

        TransactionReceipt receipt = new PollingTransactionReceiptProcessor(web3j, 1000, 40)
                .waitForTransactionReceipt(sendResponse.getTransactionHash());

        return receipt.getContractAddress();
    }

    private String readContractBinary() throws Exception {
        try (InputStream is = new ClassPathResource("solidity/DocumentRegistry/DocumentRegistry.bin").getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    public String storeDocumentHash(Long documentId, byte[] fileBytes, String caseId) throws Exception {
        byte[] hash = Hash.sha3(fileBytes);

        Function function = new Function(
                "storeDocumentHash",
                Arrays.asList(new Uint256(BigInteger.valueOf(documentId)), new Bytes32(hash), new Utf8String(caseId)),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthSendTransaction sendResponse = transactionManager.sendTransaction(
                gasProvider.getGasPrice(), gasProvider.getGasLimit(), contractAddress, encodedFunction, BigInteger.ZERO);

        if (sendResponse.hasError()) {
            throw new RuntimeException("storeDocumentHash failed: " + sendResponse.getError().getMessage());
        }

        TransactionReceipt receipt = new PollingTransactionReceiptProcessor(web3j, 1000, 40)
                .waitForTransactionReceipt(sendResponse.getTransactionHash());

        return receipt.getTransactionHash();
    }

    public boolean verifyDocumentHash(Long documentId, byte[] currentFileBytes) throws Exception {
        byte[] onChainHash = getStoredHash(documentId);
        byte[] currentHash = Hash.sha3(currentFileBytes);
        return Arrays.equals(onChainHash, currentHash);
    }

    private byte[] getStoredHash(Long documentId) throws Exception {
        Function function = new Function(
                "getDocumentRecord",
                Collections.singletonList(new Uint256(BigInteger.valueOf(documentId))),
                Arrays.asList(
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Utf8String>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {}
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);
        Transaction ethCallTransaction = Transaction.createEthCallTransaction(
                credentials.getAddress(), contractAddress, encodedFunction);

        EthCall response = web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).send();

        if (response.hasError()) {
            throw new RuntimeException("getDocumentRecord failed: " + response.getError().getMessage());
        }

        List<Type> results = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return ((Bytes32) results.get(0)).getValue();
    }

}
