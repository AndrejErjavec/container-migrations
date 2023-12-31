package main.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.opencsv.CSVWriter;


public class CsvUtils {
    public static void initCsv(String[] headers, String filePath) {
        File file = new File(filePath);

        try {
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);

            writer.writeNext(headers);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeLine(String[] line, String filePath) {
        File file = new File(filePath);

        try {
            FileWriter outputFile = new FileWriter(file, true);
            CSVWriter writer = new CSVWriter(outputFile);

            writer.writeNext(line);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
