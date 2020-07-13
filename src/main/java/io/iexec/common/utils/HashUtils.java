package io.iexec.common.utils;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HashUtils {

    private HashUtils() {
        throw new UnsupportedOperationException();
    }

    public static String concatenateAndHash(String... hexaString) {

        // convert
        byte[] res = new byte[0];
        for (String str : hexaString) {
            res = org.bouncycastle.util.Arrays.concatenate(res, BytesUtils.stringToBytes(str));
        }

        // Hash the result and convert to String
        return Numeric.toHexString(Hash.sha3(res));
    }

    public static String sha256(String utf8Input) {
        byte[] input = utf8Input.getBytes(StandardCharsets.UTF_8);
        byte[] hexHash = Hash.sha256(input);
        return BytesUtils.bytesToString(hexHash);
    }

    public static String getFileTreeSha256(String fileTreePath) {
        File fileTree = new File(fileTreePath);

        if (!fileTree.exists()){
            return "";
        }

        //fileTree is a leaf, a single file
        if (!fileTree.isDirectory()){
            return getFileSha256(fileTreePath);
        }

        //fileTree is a tree, with multiple files
        File[] files = fileTree.listFiles();
        if (files != null){
            List<String> hashes = new ArrayList<>();
            java.util.Arrays.sort(files); // /!\ files MUST be sorted to ensure final concatenateAndHash(..) is always the same (order matters)
            for (File file : files) {
                hashes.add(getFileSha256(file.getAbsolutePath()));
            }
            return HashUtils.concatenateAndHash(hashes.toArray(new String[0]));
        }

        return "";
    }

    public static String getFileSha256(String filePath) {
        if (!new File((filePath)).exists()){
            return "";
        }
        byte[] input = FileHelper.readAllBytes(filePath);
        if (input == null){
            return "";
        }
        return BytesUtils.bytesToString(Hash.sha256(input));
    }
}
