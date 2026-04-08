package com.sanjquant.xe.steps;

import com.sanjquant.xe.context.TestContext;
import com.sanjquant.xe.pages.CurrencyConverterPage;
import com.sanjquant.xe.utils.CsvDataLoader;
import com.sanjquant.xe.utils.NumberFormatter;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CurrencyConversionSteps {
    private List<Map<String, String>> records;

    @Given("the XE Currency Converter site is opened")
    public void theXeCurrencyConverterSiteIsOpened() {
        String baseUrl = System.getProperty("base.url", "https://www.xe.com/");
        CurrencyConverterPage page = new CurrencyConverterPage(TestContext.getPage());
        page.navigateToXeCurrencyConverter(baseUrl);
        page.verifyPageTitle();
        page.acceptCookiesIfPresent();
    }

    @When("I load the currency conversion records from {string}")
    public void iLoadTheCurrencyConversionRecordsFrom(String csvResource) {
        records = CsvDataLoader.load(csvResource);
        assertThat(records).isNotEmpty();
    }

    @Then("each record should produce the expected converted value")
    public void eachRecordShouldProduceTheExpectedConvertedValue() {
        assertThat(records).isNotNull().isNotEmpty();
        CurrencyConverterPage page = new CurrencyConverterPage(TestContext.getPage());

        for (Map<String, String> record : records) {
            String amount = record.get("Amount");
            String fromCurrency = record.get("FromCurrency");
            String toCurrency = record.get("ToCurrency");

            page.enterAmount(amount);
            page.fromCurrency(fromCurrency);
            page.toCurrency(toCurrency);
            page.clickConvertButton();
            page.acceptFirstAlertIfPresent();
            page.waitForResultToRender();

            String rate = page.getExchangeRate(fromCurrency, toCurrency);
            String expectedResult = NumberFormatter.multiplyRateWithAmount(rate, Double.parseDouble(amount));
            String actualText = page.getFirstBigRateValue();
            String actualResult = NumberFormatter.extractAndFormatNumber(actualText);

            assertThat(actualResult)
                    .as("Converted value for %s -> %s", fromCurrency, toCurrency)
                    .isEqualTo(expectedResult);
        }
    }
}
