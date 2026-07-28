package com.reconciliation;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class CSVReader {
    public static ReadResult read(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        List<String> badRows = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(filePath));

        for (int i = 1; i < lines.size() ; i++) {
            String line = lines.get(i);
            try {
                String[] columns = line.split(",");
                String id = columns[0];
                LocalDate date = LocalDate.parse(columns[1]);
                BigDecimal amount = new BigDecimal(columns[2]);
                transactions.add(new Transaction(id, amount, date));
            } catch (Exception e) {
                badRows.add("Lines " + (i + 1) + ": " + line);
            }
        }
        return new ReadResult(transactions, badRows);
    }
}
