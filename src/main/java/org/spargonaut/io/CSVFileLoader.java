package org.spargonaut.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVFileLoader {

    public List<File> getFileNamesIn(String directoryName) {
        File directory = new File(directoryName);
        return getCSVFiles(Arrays.asList(directory.listFiles()));
    }

    private List<File> getCSVFiles(List<File> allFiles) {
        List<File> csvFiles = new ArrayList<>();
        for (File csvFile : allFiles) {
            if (isCSVFile(csvFile)) {
                csvFiles.add(csvFile);
            }
        }
        return csvFiles;
    }

    private boolean isCSVFile(File csvFile) {
        String csvFileName = csvFile.getName();
        int csvFileLength = csvFileName.length();
        int indexOfSuffix = csvFileName.lastIndexOf('.');
        String csvFileNameSuffix = csvFileName.substring(indexOfSuffix, csvFileLength);
        return ".csv".equalsIgnoreCase(csvFileNameSuffix);
    }
}
