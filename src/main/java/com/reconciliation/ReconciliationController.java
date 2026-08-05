package com.reconciliation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.io.IOException;

@RestController
public class ReconciliationController {

        @GetMapping("/reconcile")
            public List<Break> reconcile ()  throws IOException {
                // read both files (same as current Main)
                ReadResult resultA = CSVReader.read("ledger_a.csv");
                ReadResult resultB = CSVReader.read("ledger_b.csv");


                ReconciliationEngine engine = new ReconciliationEngine();
                return engine.reconcile(resultA.getTransactions(), resultB.getTransactions());
            }
        }


