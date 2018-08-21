package ch.streamly.chronicle.flux.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class ChronicleStoreCleanup {

    public static void deleteStoreIfItExists(String path) {
        File directory = new File(".");
        boolean done = false;
        long startTime = System.currentTimeMillis();
        Exception exception = null;
        while(!done && System.currentTimeMillis() - startTime < 5000){
            try {
                deleteStore(path, directory);
                done = true;
            } catch (IOException e) {
                exception = e;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interrupted) {
                    System.out.println("store deletion interrupted "+interrupted);
                }
            }
        }
        if(!done){
            System.err.println("Error while deleting store "+exception);
        }
    }

    private static void deleteStore(String path, File directory) throws IOException {
        String FULL_PATH = directory.getCanonicalPath() + File.separator + path;
        File storePath = new File(FULL_PATH);
        if (storePath.exists()) {
            FileUtils.deleteDirectory(storePath);
            System.out.println("Deleted existing store");
        }else{
            System.out.println("Path does not exists "+path);
        }
    }
}
