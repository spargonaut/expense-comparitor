package org.spargonaut;

import org.spargonaut.datamodels.CreditCardActivity;
import org.spargonaut.datamodels.Expense;
import org.spargonaut.datamodels.MatchedTransaction;
import org.spargonaut.matchers.PreviousMatchDetector;
import org.spargonaut.matchers.TransactionMatcher;
import org.spargonaut.matchers.UnmatchedCollector;

import java.util.*;

public class TransactionProcessor {

    private final List<Expense> expenses;
    private List<CreditCardActivity> creditCardActivities;
    private Map<String, List<MatchedTransaction>> matchedTransactions;

    public TransactionProcessor(Set<CreditCardActivity> creditCardActivities, Set<Expense> expenses) {
        this.creditCardActivities = new ArrayList<>(creditCardActivities);
        this.expenses = new ArrayList<>(expenses);
        this.matchedTransactions = new HashMap<>();
    }

    public Set<CreditCardActivity> getUnmatchedCreditCardActivies() {
        UnmatchedCollector<CreditCardActivity> unmatchedCollector = new UnmatchedCollector<>();
        return unmatchedCollector.collect(this.creditCardActivities, this.matchedTransactions.values());
    }

    public Set<Expense> getUnmatchedExpenses() {
        UnmatchedCollector<Expense> unmatchedCollector = new UnmatchedCollector<>();
        return unmatchedCollector.collect(this.expenses, this.matchedTransactions.values());
    }

    public Map<String, List<MatchedTransaction>> getMatchedTransactions() {
        return this.matchedTransactions;
    }

    public void processTransactions(List<TransactionMatcher> matchers) {
        for (TransactionMatcher matcher : matchers) {
            List<MatchedTransaction> matchedTransactionList = createMatchedTransactions(matcher);
            this.matchedTransactions.put(matcher.getType(), matchedTransactionList);
        }
    }

    private List<MatchedTransaction> createMatchedTransactions(TransactionMatcher matcher) {
        List<MatchedTransaction> matchedTransactions = new ArrayList<>();
        PreviousMatchDetector previousMatchDetector = new PreviousMatchDetector();

        for (Expense expense : this.getUnmatchedExpenses()) {
            for (CreditCardActivity creditCardActivity : this.getUnmatchedCreditCardActivies()) {
                boolean isMatch = matcher.isMatch(expense, creditCardActivity);
                boolean creditCardActivityIsPreviouslyMatched = previousMatchDetector.isPreviouslyMatched(creditCardActivity, new HashSet<>(matchedTransactions));
                boolean expenseIsPreviouslyMatched = previousMatchDetector.isPreviouslyMatched(expense, new HashSet<>(matchedTransactions));

                if (isMatch && !creditCardActivityIsPreviouslyMatched && !expenseIsPreviouslyMatched) {
                    matchedTransactions.add(new MatchedTransaction(creditCardActivity, expense));
                }
            }
        }
        return matchedTransactions;
    }
}