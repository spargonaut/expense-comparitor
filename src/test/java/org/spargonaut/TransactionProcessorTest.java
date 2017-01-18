package org.spargonaut;

import org.joda.time.DateTime;
import org.junit.Test;
import org.spargonaut.datamodels.ActivityType;
import org.spargonaut.datamodels.CreditCardActivity;
import org.spargonaut.datamodels.Expense;
import org.spargonaut.datamodels.MatchedTransaction;
import org.spargonaut.datamodels.testbuilders.CreditCardActivityBuilder;
import org.spargonaut.datamodels.testbuilders.ExpenseBuilder;
import org.spargonaut.matchers.CloseDateMatcher;
import org.spargonaut.matchers.ExactMatcher;
import org.spargonaut.matchers.TransactionMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TransactionProcessorTest {

    private final int yearToMatchOn = 2016;
    private final int monthOfYearToMatchOn = 12;
    private final double amountToMatchOn = 3.38;
    private final String descriptionToMatchOn = "this is a description for a matched amount";
    private final double amountToMatchOnForCreditCardActivityOne = amountToMatchOn * -1;
    private final String merchantToMatch = "some merchant";

    @Test
    public void shouldCreateAnExactTransactionMatch_whenACreditCardActivityHasTheSameAmountAndTransactionDateAsAnExpenseEntry() {

        int dayOfMonthForTransactionDateToMatchOn = 31;
        DateTime transactionDateToMatchOn = getDateTimeForDay(dayOfMonthForTransactionDateToMatchOn);

        CreditCardActivity creditCardActivityToMatch = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(transactionDateToMatchOn)
                .setType(ActivityType.SALE)
                .build();

        CreditCardActivity creditCardActivityTwo = new CreditCardActivityBuilder().build();
        CreditCardActivity creditCardActivityThree = new CreditCardActivityBuilder().build();

        Expense expenseToMatch = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(transactionDateToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        List<CreditCardActivity> creditCardActivities = Arrays.asList(creditCardActivityTwo, creditCardActivityToMatch, creditCardActivityThree);
        List<Expense> expenses = Arrays.asList(expenseToMatch);

        TransactionProcessor transactionProcessor = new TransactionProcessor(creditCardActivities, expenses);
        TransactionMatcher exactMatcher = new ExactMatcher();
        List<TransactionMatcher> exactMatcherList = Arrays.asList(exactMatcher);
        transactionProcessor.processTransactions(exactMatcherList);

        Map<String, List<MatchedTransaction>> matchedTransactionsMap = transactionProcessor.getMatchedTransactions();
        List<MatchedTransaction> matchedTransactions = matchedTransactionsMap.get(exactMatcher.getType());


        assertThat(matchedTransactions.size(), is(1));

        CreditCardActivity expectedCreditCardActivityMatch = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setTransactionDate(getDateTimeForDay(dayOfMonthForTransactionDateToMatchOn))
                .setDescription(creditCardActivityToMatch.getDescription())
                .build();

        MatchedTransaction matchedTransaction = matchedTransactions.get(0);
        CreditCardActivity matchedCreditCardActivity = matchedTransaction.getMatchedCreditCardActivity();
        assertThat(matchedCreditCardActivity.equals(expectedCreditCardActivityMatch), is(true));

        Expense expectedExpenseMatch = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(transactionDateToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        Expense matchedExpense = matchedTransaction.getMatchedExpense();
        assertThat(matchedExpense.equals(expectedExpenseMatch), is(true));
    }

    @Test
    public void shouldCreateAListOfExpensesThatAreMatchedExactly() {
        int dayOfMonthForExpenseDateToMatchOn = 25;
        int dayOfMonthForTransactionDateToMatchOn = 25;
        int dayOfMonthForTransactionDateToNotMatchOn = 24;

        DateTime expenseDateToMatchOn = getDateTimeForDay(dayOfMonthForExpenseDateToMatchOn);
        DateTime transactionDateToMatchOn = getDateTimeForDay(dayOfMonthForTransactionDateToMatchOn);
        DateTime transactionDateAfterMatchDate = getDateTimeForDay(dayOfMonthForTransactionDateToNotMatchOn);

        CreditCardActivity creditCardActivityOne = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(transactionDateToMatchOn)
                .setType(ActivityType.SALE)
                .build();

        CreditCardActivity creditCardActivityTwo = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(transactionDateAfterMatchDate)
                .setType(ActivityType.SALE)
                .build();

        Expense expenseOne = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(expenseDateToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        List<CreditCardActivity> creditCardActivitiesForTesting = Arrays.asList(creditCardActivityOne, creditCardActivityTwo, new CreditCardActivityBuilder().build());
        List<Expense> expenses = Arrays.asList(expenseOne);

        TransactionProcessor transactionProcessor = new TransactionProcessor(creditCardActivitiesForTesting, expenses);
        TransactionMatcher exactMatcher = new ExactMatcher();
        List<TransactionMatcher> exactMatcherList = Arrays.asList(exactMatcher);
        transactionProcessor.processTransactions(exactMatcherList);

        Map<String, List<MatchedTransaction>> matchedTransactionsMap = transactionProcessor.getMatchedTransactions();
        List<MatchedTransaction> matchedTransactions = matchedTransactionsMap.get(exactMatcher.getType());

        assertThat(matchedTransactions.size(), is(1));

        CreditCardActivity expectedCreditCardActivityMatch = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(creditCardActivityOne.getTransactionDate())
                .build();

        MatchedTransaction matchedTransaction = matchedTransactions.get(0);
        CreditCardActivity matchedCreditCardActivity = matchedTransaction.getMatchedCreditCardActivity();
        assertThat(matchedCreditCardActivity.equals(expectedCreditCardActivityMatch), is(true));

        Expense expectedExpenseMatch = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(expenseDateToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        Expense matchedExpense = matchedTransaction.getMatchedExpense();
        assertThat(matchedExpense.equals(expectedExpenseMatch), is(true));
    }

    @Test
    public void shouldCreateAListOfCloseTransactionMatches_whenCreditCardActivityDateIsOneDayBeforeExpenseDate() {
        int dayOfMonthToMatchOn = 24;

        DateTime expenseDateToMatchOn = getDateTimeForDay(dayOfMonthToMatchOn);

        CreditCardActivity creditCardActivityOne = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(expenseDateToMatchOn.minusDays(1))
                .setType(ActivityType.SALE)
                .build();

        Expense expenseOne = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(expenseDateToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        List<CreditCardActivity> creditCardActivitiesForTesting = Arrays.asList(creditCardActivityOne, new CreditCardActivityBuilder().build(), new CreditCardActivityBuilder().build());
        List<Expense> expenses = Arrays.asList(expenseOne);

        TransactionProcessor transactionProcessor = new TransactionProcessor(creditCardActivitiesForTesting, expenses);
        CloseDateMatcher closeDateMatcher = new CloseDateMatcher();
        List<TransactionMatcher> exactMatcherList = Arrays.asList(closeDateMatcher);
        transactionProcessor.processTransactions(exactMatcherList);

        Map<String, List<MatchedTransaction>> matchedTransactionsMap = transactionProcessor.getMatchedTransactions();
        List<MatchedTransaction> matchedTransactions = matchedTransactionsMap.get(closeDateMatcher.getType());

        assertThat(matchedTransactions.size(), is(1));

        CreditCardActivity expectedCreditCardActivityMatch = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(expenseDateToMatchOn.minusDays(1))
                .build();

        MatchedTransaction matchedTransaction = matchedTransactions.get(0);
        CreditCardActivity matchedCreditCardActivity = matchedTransaction.getMatchedCreditCardActivity();
        assertThat(matchedCreditCardActivity.equals(expectedCreditCardActivityMatch), is(true));

        Expense expectedExpenseMatch = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(expenseDateToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        Expense matchedExpense = matchedTransaction.getMatchedExpense();
        assertThat(matchedExpense.equals(expectedExpenseMatch), is(true));
    }

    @Test
    public void shouldCreateAListOfCloseTransactionMatches_whenCreditCardActivityDateIsOneDayAfterTheExpenseDate() {
        int dayOfMonthForExpenseToMatchOn = 26;

        DateTime expenseDateToMatchOn = getDateTimeForDay(dayOfMonthForExpenseToMatchOn);

        CreditCardActivity creditCardActivityOne = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(expenseDateToMatchOn.plusDays(1))
                .setType(ActivityType.SALE)
                .build();

        Expense expenseOne = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(expenseDateToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        List<CreditCardActivity> creditCardActivitiesForTesting = Arrays.asList(creditCardActivityOne, new CreditCardActivityBuilder().build(), new CreditCardActivityBuilder().build());
        List<Expense> expenses = Arrays.asList(expenseOne);

        TransactionProcessor transactionProcessor = new TransactionProcessor(creditCardActivitiesForTesting, expenses);
        CloseDateMatcher closeDateMatcher = new CloseDateMatcher();
        List<TransactionMatcher> exactMatcherList = Arrays.asList(closeDateMatcher);
        transactionProcessor.processTransactions(exactMatcherList);

        Map<String, List<MatchedTransaction>> matchedTransactionsMap = transactionProcessor.getMatchedTransactions();
        List<MatchedTransaction> matchedTransactions = matchedTransactionsMap.get(closeDateMatcher.getType());

        assertThat(matchedTransactions.size(), is(1));

        CreditCardActivity expectedCreditCardActivityMatch = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(expenseDateToMatchOn.plusDays(1))
                .build();

        MatchedTransaction matchedTransaction = matchedTransactions.get(0);
        CreditCardActivity matchedCreditCardActivity = matchedTransaction.getMatchedCreditCardActivity();
        assertThat(matchedCreditCardActivity.equals(expectedCreditCardActivityMatch), is(true));

        Expense expectedExpenseMatch = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(expenseDateToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        Expense matchedExpense = matchedTransaction.getMatchedExpense();
        assertThat(matchedExpense.equals(expectedExpenseMatch), is(true));
    }

    @Test
    public void shouldCreateAListOfUnmatchedCreditCardActivities() {
        int dayOfMonthForExpenseDateToMatchOn = 26;
        DateTime dateForExpenseOneToMatchOn = getDateTimeForDay(dayOfMonthForExpenseDateToMatchOn);
        CreditCardActivity creditCardActivityToMatchExactly = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(dateForExpenseOneToMatchOn)
                .build();

        Expense expenseToMatchExactly = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(dateForExpenseOneToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        int dayOfMonthForExpenseDateCloseMatchBefore = 20;
        DateTime dateForExpenseDateCloseMatchOne = getDateTimeForDay(dayOfMonthForExpenseDateCloseMatchBefore);
        CreditCardActivity creditCardActivityToCloselyMatchDayBefore = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(dateForExpenseDateCloseMatchOne.minusDays(1))
                .build();

        Expense expenseToCloselyMatchDayBefore = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(dateForExpenseDateCloseMatchOne)
                .setAmount(amountToMatchOn)
                .build();

        int dayOfMonthForExpenseDateCloseMatchAfter = 15;
        DateTime dateForExpenseDateCloseMatchAfter = getDateTimeForDay(dayOfMonthForExpenseDateCloseMatchAfter);
        CreditCardActivity creditCardActivityToCloselyMatchDayAfter = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(dateForExpenseDateCloseMatchAfter.plusDays(1))
                .build();

        Expense expenseToCloselyMatchDayAfter = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(dateForExpenseDateCloseMatchAfter)
                .setAmount(amountToMatchOn)
                .build();

        int dayOfMonthForCreditCardActivityThatDoesntMatch = 10;
        DateTime dateForCreditCardActivityThatDoesntMatch = getDateTimeForDay(dayOfMonthForCreditCardActivityThatDoesntMatch);
        CreditCardActivity creditCardActivityThatDoesntMatch = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(dateForCreditCardActivityThatDoesntMatch)
                .build();

        List<CreditCardActivity> creditCardActivities = Arrays.asList(creditCardActivityToMatchExactly, creditCardActivityToCloselyMatchDayBefore, creditCardActivityToCloselyMatchDayAfter, creditCardActivityThatDoesntMatch);
        List<Expense> expenses = Arrays.asList(expenseToMatchExactly, expenseToCloselyMatchDayBefore, expenseToCloselyMatchDayAfter);

        TransactionProcessor transactionProcessor = new TransactionProcessor(creditCardActivities, expenses);
        List<TransactionMatcher> exactMatcherList = Arrays.asList(new ExactMatcher(), new CloseDateMatcher());
        transactionProcessor.processTransactions(exactMatcherList);

        List<CreditCardActivity> unmatchedCreditCardActivities = transactionProcessor.getUnmatchedCreditCardActivies();

        assertThat(unmatchedCreditCardActivities.size(), is(1));

    }

    @Test
    public void shouldCreateAListOfUnmatchedExpenses() {
        int dayOfMonthForExpenseDateToMatchOn = 26;
        DateTime dateForExpenseOneToMatchOn = getDateTimeForDay(dayOfMonthForExpenseDateToMatchOn);
        CreditCardActivity creditCardActivityToMatchExactly = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(dateForExpenseOneToMatchOn)
                .build();

        Expense expenseToMatchExactly = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(dateForExpenseOneToMatchOn)
                .setAmount(amountToMatchOn)
                .build();

        int dayOfMonthForExpenseDateCloseMatchBefore = 20;
        DateTime dateForExpenseDateCloseMatchOne = getDateTimeForDay(dayOfMonthForExpenseDateCloseMatchBefore);
        CreditCardActivity creditCardActivityToCloselyMatchDayBefore = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(dateForExpenseDateCloseMatchOne.minusDays(1))
                .build();

        Expense expenseToCloselyMatchDayBefore = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(dateForExpenseDateCloseMatchOne)
                .setAmount(amountToMatchOn)
                .build();

        int dayOfMonthForExpenseDateCloseMatchAfter = 15;
        DateTime dateForExpenseDateCloseMatchAfter = getDateTimeForDay(dayOfMonthForExpenseDateCloseMatchAfter);
        CreditCardActivity creditCardActivityToCloselyMatchDayAfter = new CreditCardActivityBuilder()
                .setAmount(amountToMatchOnForCreditCardActivityOne)
                .setDescription(descriptionToMatchOn)
                .setTransactionDate(dateForExpenseDateCloseMatchAfter.plusDays(1))
                .build();

        Expense expenseToCloselyMatchDayAfter = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(dateForExpenseDateCloseMatchAfter)
                .setAmount(amountToMatchOn)
                .build();

        int dayOfMonthForExpenseThatDoesntMatch = 10;
        DateTime dateForExpenseThatDoesntMatch = getDateTimeForDay(dayOfMonthForExpenseThatDoesntMatch);
        Expense expenseThatDoesNotMatch = new ExpenseBuilder()
                .setMerchant(merchantToMatch)
                .setTimestamp(dateForExpenseThatDoesntMatch)
                .setAmount(amountToMatchOn)
                .build();


        List<CreditCardActivity> creditCardActivities = Arrays.asList(creditCardActivityToMatchExactly, creditCardActivityToCloselyMatchDayBefore, creditCardActivityToCloselyMatchDayAfter);
        List<Expense> expenses = Arrays.asList(expenseToMatchExactly, expenseToCloselyMatchDayBefore, expenseToCloselyMatchDayAfter, expenseThatDoesNotMatch);

        TransactionProcessor transactionProcessor = new TransactionProcessor(creditCardActivities, expenses);
        List<TransactionMatcher> exactMatcherList = Arrays.asList(new ExactMatcher(), new CloseDateMatcher());
        transactionProcessor.processTransactions(exactMatcherList);

        List<Expense> unmatchedExpenses = transactionProcessor.getUnmatchedExpenses();
        assertThat(unmatchedExpenses.size(), is(1));

    }

    private DateTime getDateTimeForDay(int dayOfMonthForPostDate) {
        return new DateTime(yearToMatchOn, monthOfYearToMatchOn, dayOfMonthForPostDate, 0, 0);
    }
}