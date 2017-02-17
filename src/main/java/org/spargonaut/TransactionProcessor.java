package org.spargonaut;

import org.spargonaut.datamodels.CreditCardActivity;
import org.spargonaut.datamodels.Expense;
import org.spargonaut.datamodels.MatchedTransaction;
import org.spargonaut.matchers.TransactionMatcher;

import java.util.*;
import java.util.function.Predicate;

public class TransactionProcessor {

    class CreditCardPredicate implements Predicate<CreditCardActivity> {

        TransactionMatcher transactionMatcher;
        private final Expense expense;
        private final List<MatchedTransaction> matchedTransactions;

        public CreditCardPredicate(TransactionMatcher transactionMatcher, Expense expense, List<MatchedTransaction> matchedTransactions) {
            this.transactionMatcher = transactionMatcher;
            this.expense = expense;
            this.matchedTransactions = matchedTransactions;
        }

        @Override
        public boolean test(CreditCardActivity creditCardActivity) {
            boolean isMatched = transactionMatcher.isMatch(this.expense, creditCardActivity);
            boolean expense = expenseIsNotPreviouslyMatched(this.expense, matchedTransactions);
            boolean creditCardActivityIsNotPreviouslyMatched = creditCardActivityIsNotPreviouslyMatched(creditCardActivity, matchedTransactions);
            return isMatched && expense && creditCardActivityIsNotPreviouslyMatched;
        }
    }


    private final List<Expense> expenses;
    private List<CreditCardActivity> creditCardActivities;
    private Map<String, List<MatchedTransaction>> matchedTransactions;

    public TransactionProcessor(List<CreditCardActivity> creditCardActivities, List<Expense> expenses) {
        this.creditCardActivities = creditCardActivities;
        this.expenses = expenses;
        this.matchedTransactions = new HashMap<>();
    }

    public List<CreditCardActivity> getUnmatchedCreditCardActivies() {
        return collectUnmatchedCreditCardActivities(this.creditCardActivities, this.matchedTransactions.values());
    }

    public List<Expense> getUnmatchedExpenses() {
        return collectUnmatchedExpenses(this.expenses, this.matchedTransactions.values());
    }

    public Map<String, List<MatchedTransaction>> getMatchedTransactions() {
        return this.matchedTransactions;
    }

    public void processTransactions(List<TransactionMatcher> matchers) {
        for (TransactionMatcher matcher : matchers) {
            List<Expense> unmatchedExpenses = collectUnmatchedExpenses(this.expenses, this.matchedTransactions.values());
            List<CreditCardActivity> unmatchedCreditCardActivities = collectUnmatchedCreditCardActivities(this.creditCardActivities, this.matchedTransactions.values());
            List<MatchedTransaction> matchedTransactionList = createMatchedTransactions(unmatchedCreditCardActivities, unmatchedExpenses, matcher);
            this.matchedTransactions.put(matcher.getType(), matchedTransactionList);
        }
    }

    private List<MatchedTransaction> createMatchedTransactions(List<CreditCardActivity> creditCardActivities, List<Expense> expenses, TransactionMatcher matcher) {
        List<MatchedTransaction> matchedTransactions = new ArrayList<>();
        for (Expense expense : expenses) {
            for (CreditCardActivity creditCardActivity : creditCardActivities) {
                if(matcher.isMatch(expense, creditCardActivity) &&
                        expenseIsNotPreviouslyMatched(expense, matchedTransactions) &&
                        creditCardActivityIsNotPreviouslyMatched(creditCardActivity, matchedTransactions)) {
                    matchedTransactions.add(new MatchedTransaction(creditCardActivity, expense));
                }
            }
        }
        return matchedTransactions;
    }

    private boolean expenseIsNotPreviouslyMatched(Expense expense, List<MatchedTransaction> matchedTransactions) {
        boolean isMatched = false;
        for (MatchedTransaction matchedTransaction : matchedTransactions) {
            isMatched = matchedTransaction.containsExpense(expense);
        }
        return !isMatched;
    }

    private boolean creditCardActivityIsNotPreviouslyMatched(CreditCardActivity creditCardActivity, List<MatchedTransaction> matchedTransactions) {
        boolean isMatched = false;
        for (MatchedTransaction matchedTransaction : matchedTransactions) {
            isMatched = matchedTransaction.containsCreditCardActivity(creditCardActivity);
        }
        return !isMatched;
    }

    private List<CreditCardActivity> collectUnmatchedCreditCardActivities(List<CreditCardActivity> creditCardActivities, Collection<List<MatchedTransaction>> matchedTransactionList) {
        List<CreditCardActivity> unmatchedCreditCardActivities = new ArrayList<>(creditCardActivities);
        for (List<MatchedTransaction> matchedTransactions : matchedTransactionList) {
            for (MatchedTransaction matchedTransaction : matchedTransactions) {
                CreditCardActivity matchedCreditCardActivity = matchedTransaction.getMatchedCreditCardActivity();
                if (unmatchedCreditCardActivities.contains(matchedCreditCardActivity)) {
                    unmatchedCreditCardActivities.remove(matchedCreditCardActivity);
                }
            }
        }
        return unmatchedCreditCardActivities;
    }

    private List<Expense> collectUnmatchedExpenses(List<Expense> expenses, Collection<List<MatchedTransaction>> matchedTransactionLists) {
        List<Expense> unmatchedExpenses = new ArrayList<>(expenses);
        for (List<MatchedTransaction> matchedTransactions : matchedTransactionLists) {
            for (MatchedTransaction matchedTransaction : matchedTransactions) {
                Expense matchedExpense = matchedTransaction.getMatchedExpense();
                if (unmatchedExpenses.contains(matchedExpense)) {
                    unmatchedExpenses.remove(matchedExpense);
                }
            }
        }
        return unmatchedExpenses;
    }
}