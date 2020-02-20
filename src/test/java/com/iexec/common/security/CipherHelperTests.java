package com.iexec.common.security;

import com.iexec.common.utils.BytesUtils;
import org.junit.Test;
import org.web3j.crypto.Hash;

import java.security.KeyPair;
import java.util.Random;


import static com.iexec.common.security.CipherHelper.*;
import static org.junit.Assert.assertEquals;

public class CipherHelperTests {

    private static final int KB = 1000;
    private static final int MB = 1000 * KB;

    @Test
    public void shouldEncryptAndDecryptWithAes() {
        byte[] originalData = getRandomByteArray(1 * MB);

        byte[] aesKey = generateAesKey();

        byte[] encryptedOriginalData = aesEncrypt(originalData, aesKey);

        byte[] data = aesDecrypt(encryptedOriginalData, aesKey);

        assertEquals(
                BytesUtils.bytesToString(Hash.sha3(originalData)),
                BytesUtils.bytesToString(Hash.sha3(data))
        );
    }

    @Test
    public void shouldEncryptAndDecryptWithRsaKeys() {
        byte[] originalData = getRandomByteArray(128);

        KeyPair rsaKeys = generateRsaKeys();

        byte[] encryptedOriginalData = rsaEncrypt(originalData, rsaKeys.getPublic());

        byte[] data = rsaDecrypt(encryptedOriginalData, rsaKeys.getPrivate());

        assertEquals(
                BytesUtils.bytesToString(Hash.sha3(originalData)),
                BytesUtils.bytesToString(Hash.sha3(data))
        );
    }

    @Test
    public void shouldEncryptAndDecryptWithRsaKeysFile() {
        byte[] originalData = getRandomByteArray(128);

        String keyDirPath = "./src/test/resources";

        KeyPair rsaKeys = getRsaKeyPair(
                keyDirPath + "/test_rsa_key.pub",
                keyDirPath + "/test_rsa_key");

        byte[] encryptedOriginalData = rsaEncrypt(originalData, rsaKeys.getPublic());

        byte[] data = rsaDecrypt(encryptedOriginalData, rsaKeys.getPrivate());

        assertEquals(
                BytesUtils.bytesToString(Hash.sha3(originalData)),
                BytesUtils.bytesToString(Hash.sha3(data))
        );
    }

    private byte[] getRandomByteArray(int size) {
        byte[] randomByteArray = new byte[size];
        new Random().nextBytes(randomByteArray);
        return randomByteArray;
    }


}