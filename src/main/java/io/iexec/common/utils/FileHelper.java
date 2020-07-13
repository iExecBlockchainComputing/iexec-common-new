package io.iexec.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.web3j.crypto.Hash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class FileHelper {

    //TODO move to IexecFileHelper.java
    public static final String SLASH_IEXEC_OUT = File.separator + "iexec_out";
    public static final String SLASH_IEXEC_IN = File.separator + "iexec_in";
    public static final String SLASH_OUTPUT = File.separator + "output";
    public static final String SLASH_INPUT = File.separator + "input";

    private FileHelper() {
        throw new UnsupportedOperationException();
    }

    public static String readFile(String filePath) {
        try {
            byte[] content = Files.readAllBytes(Paths.get(filePath));
            return new String(content);
        } catch (IOException e) {
            log.error("Failed to read file [filePath:{}]", filePath);
        }
        return "";
    }

    public static byte[] readFileBytes(String filePath) {
        String content = readFile(filePath);

        if (!content.isEmpty()) {
            return content.getBytes();
        }
        return null;
    }

    /*
     * TODO
     * 1 - Rename FileHelper.readFile --> FileHelper.readStringFile
     * 2 - Remove FileHelper.readFileBytes ?
     * */
    public static byte[] readAllBytes(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Failed to readAllBytes [filePath:{}]", filePath);
        }

        return null;
    }

    public static boolean writeFile(String filePath, byte[] data) {
        try {
            Files.write(Paths.get(filePath), data);
            return true;
        } catch (IOException e) {
            log.error("Failed to write file [filePath:{}, dataHash:{}]", filePath, Hash.sha3(data));
        }
        return false;
    }

    public static File createFileWithContent(String filePath, String data) {
        return data != null ? createFileWithContent(filePath, data.getBytes()) : null;
    }

    public static File createFileWithContent(String filePath, byte[] data) {
        String parentDirectoryPath = new File(filePath).getParent();
        boolean isParentFolderCreated = createFolder(parentDirectoryPath);

        if (!isParentFolderCreated) {
            log.error("Failed to create base directory [parentDirectoryPath:{}]", parentDirectoryPath);
            return null;
        }

        try {
            Files.write(Paths.get(filePath), data);
            log.debug("File created [filePath:{}]", filePath);
            return new File(filePath);
        } catch (IOException e) {
            log.error("Failed to create file [filePath:{}]", filePath);
            return null;
        }
    }

    public static boolean downloadFileInDirectory(String fileUri, String directoryPath) {
        if (!createFolder(directoryPath)) {
            log.error("Failed to create base directory [directoryPath:{}]", directoryPath);
            return false;
        }

        if (fileUri.isEmpty()) {
            log.error("FileUri shouldn't be empty [fileUri:{}]", fileUri);
            return false;
        }

        InputStream in;
        try {
            in = new URL(fileUri).openStream();//Not working with https resources yet
        } catch (IOException e) {
            log.error("Failed to download file [fileUri:{}, exception:{}]", fileUri, e.getCause());
            return false;
        }

        try {
            String fileName = Paths.get(fileUri).getFileName().toString();
            Files.copy(in, Paths.get(directoryPath + File.separator + fileName), StandardCopyOption.REPLACE_EXISTING);
            log.info("Downloaded data [fileUri:{}]", fileUri);
            return true;
        } catch (IOException e) {
            log.error("Failed to copy downloaded file to disk [directoryPath:{}, fileUri:{}]",
                    directoryPath, fileUri);
            return false;
        }
    }

    public static boolean createFolder(String folderPath) {
        File baseDirectory = new File(folderPath);
        return baseDirectory.exists() ? true : baseDirectory.mkdirs();
    }

    public static boolean deleteFile(String filePath) {
        try {
            Files.delete(Paths.get(filePath));
            log.info("File has been deleted [path:{}]", filePath);
            return true;
        } catch (IOException e) {
            log.error("Problem when trying to delete the file [path:{}]", filePath);
        }
        return false;
    }

    public static boolean deleteFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists()) {
            log.info("Folder doesn't exist so can't be deleted [path:{}]", folderPath);
            return false;
        }

        try {
            FileUtils.deleteDirectory(folder);
            log.info("Folder has been deleted [path:{}]", folderPath);
            return true;
        } catch (IOException e) {
            log.error("Problem when trying to delete the folder [path:{}]", folderPath);
        }
        return false;
    }

    public static File zipFolder(String folderPath) {
        String parentFolder = Paths.get(folderPath).getParent().toString();
        return zipFolder(folderPath, parentFolder);
    }

    public static File zipFolder(String folderPath, String saveIn) {
        String folderName = Paths.get(folderPath).getFileName().toString();
        String zipFilePath = Path.of(saveIn, folderName + ".zip").toAbsolutePath().toString();
        Path sourceFolderPath = Paths.get(folderPath);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(new File(zipFilePath)))) {
            Files.walkFileTree(sourceFolderPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    log.debug("Adding file to zip [file:{}, zip:{}]", file.toAbsolutePath().toString(), zipFilePath);
                    zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
                    if (!Files.isSymbolicLink(file)) {
                        Files.copy(file, zos);
                    }
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
            log.info("Folder zipped [path:{}]", zipFilePath);
            return new File(zipFilePath);

        } catch (Exception e) {
            log.error("Failed to zip folder [path:{}]", zipFilePath);
        }
        return null;
    }

    // TODO: Use same lib for zipping
    public static boolean unZipFile(String zipFilePath, String destDirPath) {
        if (zipFilePath == null || zipFilePath.isEmpty() || !new File(zipFilePath).exists()) {
            log.error("Failed to unZipFile (missing zipFile) [zipFilePath:{}, destDirPath:{}]", zipFilePath, destDirPath);
            return false;
        }

        if (destDirPath == null || destDirPath.isEmpty()) {
            log.error("Failed to unZipFile (missing destDirPath) [zipFilePath:{}, destDirPath:{}]", zipFilePath, destDirPath);
            return false;
        }

        try {
            new ZipFile(zipFilePath).extractAll(destDirPath);
            return true;
        } catch (ZipException e) {
            log.error("Failed to unZipFile (can't extract) [zipFilePath:{}, destDirPath:{}]" + zipFilePath + destDirPath);
        }
        return false;
    }

    /*
    * Will extract into a directory next to zip
    *
    * before
    * └── some-content.zip
    *
    * after
    * ├── some-content.zip
    * └── some-content/
    *     ├── file1
    *     └── file2
    *
    * returns extractDirPath
    * */
    public static String unZipFile(String zipPath){
        String extractDirPath = removeZipExtension(zipPath);
        if (extractDirPath.isEmpty()){
            log.error("unzip failed (removeZipExtension) [zipPath:{}]", zipPath);
            return "";
        }

        boolean isExtracted = FileHelper.unZipFile(zipPath, extractDirPath);
        if (!isExtracted){
            log.error("unzip failed (unZipFile) [zipPath:{}]", zipPath);
            return "";
        }
        return extractDirPath;
    }

    public static String removeZipExtension(String zipPath) {
        if (zipPath == null || !FilenameUtils.getExtension(zipPath).equals("zip")){
            return "";
        }
        return FilenameUtils.removeExtension(zipPath);
    }

    public static boolean replaceFile(String toBeReplaced, String replacer) {
        try {
            Files.delete(Paths.get(toBeReplaced));
            return new File(replacer).renameTo(new File(toBeReplaced));
        } catch (IOException e) {
            log.error("Problem when trying to replace file [toBeReplaced:{}, replacer:{}]",
                    toBeReplaced, replacer);
            e.printStackTrace();
            return false;
        }
    }

    public static boolean moveFile(String fromPath, String toPath) {
        boolean isMoved = new File(fromPath).renameTo(new File(toPath));
        if (!isMoved) {
            log.error("Failed to moveFile file [fromPath:{}, toPath:{}]", fromPath, toPath);
        }
        return isMoved;
    }

    public static boolean copyFolder(String source, String target) {
        try {
            FileUtils.copyDirectory(new File(source), new File(target));
            return true;
        } catch (IOException e) {
            log.error("Error copying folder [source:{}, target:{}, error:{}]",
                    source, target, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copyFile(String source, String target) {
        try {
            Files.copy(Path.of(source), Path.of(target));
            return true;
        } catch (IOException e) {
            log.error("Error copying file [source:{}, target:{}, error:{}]",
                    source, target, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static String getFilenameFromUri(String uri) {
        return Paths.get(uri).getFileName().toString();
    }

    public static String printDirectoryTree(File folder) {
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("folder is not a Directory");
        }
        int indent = 0;
        StringBuilder sb = new StringBuilder();
        printDirectoryTree(folder, indent, sb);
        return sb.toString();
    }

    private static void printDirectoryTree(File folder, int indent,
                                           StringBuilder sb) {
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("folder is not a Directory");
        }
        sb.append(getIndentString(indent));
        sb.append("├── ");
        sb.append(folder.getName());
        sb.append("/");
        sb.append("\n");
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                printDirectoryTree(file, indent + 1, sb);
            } else {
                printFile(file, indent + 1, sb);
            }
        }

    }

    private static void printFile(File file, int indent, StringBuilder sb) {
        sb.append(getIndentString(indent));
        sb.append("├── ");
        sb.append(file.getName());
        sb.append("\n");
    }

    private static String getIndentString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("│   ");
        }
        return sb.toString();
    }

}