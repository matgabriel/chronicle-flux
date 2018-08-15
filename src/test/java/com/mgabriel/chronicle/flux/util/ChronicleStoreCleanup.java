package com.mgabriel.chronicle.flux.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class ChronicleStoreCleanup {

    public static void deleteStoreIfItExists(String path) {
        File directory = new File(".");
        try {
            deleteStore(path, directory);
        } catch (IOException e) {
            //try again
            try {
                Thread.sleep(500);
                deleteStore(path, directory);
            } catch (Exception exception) {
                System.err.println("Error while deleting store "+exception);
            }

        }
    }

    private static void deleteStore(String path, File directory) throws IOException {
        String FULL_PATH = directory.getCanonicalPath() + File.separator + path;
        File storePath = new File(FULL_PATH);
        if (storePath.exists()) {
            FileUtils.deleteDirectory(storePath);
            System.out.println("Deleted existing store");
        }
    }
}
