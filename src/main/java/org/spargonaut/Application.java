package org.spargonaut;

import org.spargonaut.datamodels.CreditCardActivity;
import org.spargonaut.datamodels.Expense;
import org.spargonaut.datamodels.MatchedTransaction;
import org.spargonaut.io.CSVFileLoader;
import org.spargonaut.io.CSVFileReader;
import org.spargonaut.io.DataLoader;
import org.spargonaut.io.parser.ChargeParser;
import org.spargonaut.io.parser.ExpenseParser;
import org.spargonaut.io.printer.SummaryPrinter;
import org.spargonaut.matchers.CloseDateMatcher;
import org.spargonaut.matchers.ExactMatcher;
import org.spargonaut.matchers.FuzzyMerchantExactAmountMatcher;
import org.spargonaut.matchers.TransactionMatcher;

import java.util.*;

public class Application {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        Application application = new Application();
//        application.run();
    }

    public void run() {

        String chargeDirectoryName = "./data/credit_card_files";
        String expenseDirectoryName = "./data/expense_files";
        String manualIgnoreDirectoryName = "./data/manual_ignores";

        CSVFileReader csvFileReader = new CSVFileReader();

        DataLoader<CreditCardActivity> creditCardactivityDataLoader = new DataLoader<>(new CSVFileLoader());
        creditCardactivityDataLoader.load(chargeDirectoryName, new ChargeParser(csvFileReader));
        creditCardactivityDataLoader.ignore(manualIgnoreDirectoryName, new ChargeParser(csvFileReader));
        Set<CreditCardActivity> creditCardActivities = new HashSet<>(creditCardactivityDataLoader.getLoadedFiles());

        DataLoader<Expense> expenseDataLoader = new DataLoader<>(new CSVFileLoader());
        expenseDataLoader.load(expenseDirectoryName, new ExpenseParser(csvFileReader));
        Set<Expense> expenses = new HashSet<>(expenseDataLoader.getLoadedFiles());

        TransactionProcessor transactionProcessor = new TransactionProcessor(creditCardActivities, expenses);

        TransactionMatcher exactMatcher = new ExactMatcher();
        TransactionMatcher closeDateMatcher = new CloseDateMatcher();
        TransactionMatcher fuzzyMatcher = new FuzzyMerchantExactAmountMatcher();
        List<TransactionMatcher> matchers = Arrays.asList(exactMatcher, closeDateMatcher, fuzzyMatcher);
        transactionProcessor.processTransactions(matchers);

        Map<String, List<MatchedTransaction>> matchedTransactionsMap = transactionProcessor.getMatchedTransactions();
        Set<CreditCardActivity> unmatchedCreditCardActivity = transactionProcessor.getUnmatchedCreditCardActivies();
        Set<Expense> unmatchedExpenses = transactionProcessor.getUnmatchedExpenses();

        SummaryPrinter.printSummary(matchedTransactionsMap,
                                    creditCardActivities,
                                    creditCardactivityDataLoader.getIgnoredData(),
                                    expenses,
                                    unmatchedCreditCardActivity,
                                    unmatchedExpenses);
    }
}
