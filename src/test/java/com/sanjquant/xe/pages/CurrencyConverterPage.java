package com.sanjquant.xe.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CurrencyConverterPage {
    private static final Pattern RATE_VALUE_PATTERN = Pattern.compile("=\\s*([0-9.]+)");

    private final Page page;
    private final Locator amountTextBox;
    private final Locator fromCurrencyDropdown;
    private final Locator toCurrencyDropdown;
    private final Locator convertButton;

    public CurrencyConverterPage(Page page) {
        this.page = page;
        this.amountTextBox = page.getByLabel("Amount");
        this.fromCurrencyDropdown = page.locator("#midmarketFromCurrency").getByPlaceholder("Type to search...");
        this.toCurrencyDropdown = page.locator("#midmarketToCurrency").getByPlaceholder("Type to search...");
        this.convertButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Convert").setExact(true));
    }

    public void navigateToXeCurrencyConverter(String websiteUrl) {
        page.navigate(websiteUrl);
        page.waitForLoadState();
    }

    public void verifyPageTitle() {
        assertThat(page).hasTitle("Xe: Currency Exchange Rates and International Money Transfers");
    }

    public void acceptCookiesIfPresent() {
        Locator cookieButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Accept"));
        if (cookieButton.count() > 0) {
            try {
                cookieButton.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(3_000));
                cookieButton.first().click();
            } catch (PlaywrightException ignored) {
            }
        }
    }

    public void enterAmount(String amount) {
        amountTextBox.fill(amount);
    }

    public void fromCurrency(String fromCurrency) {
        selectCurrency(fromCurrency, fromCurrencyDropdown, 0, "#midmarketFromCurrency-listbox");
    }

    public void toCurrency(String toCurrency) {
        selectCurrency(toCurrency, toCurrencyDropdown, 1, "#midmarketToCurrency-listbox");
    }

    public void clickConvertButton() {
        convertButton.click();
    }

    public void acceptFirstAlertIfPresent() {
        Locator acceptButton = page.locator("button", new Page.LocatorOptions().setHasText("Accept"));
        try {
            if (acceptButton.count() > 0 && acceptButton.first().isVisible(new Locator.IsVisibleOptions().setTimeout(2_000))) {
                acceptButton.first().click();
            }
        } catch (PlaywrightException ignored) {
        }
    }

    public String getExchangeRate(String fromCurrency, String toCurrency) {
        Locator locator = page.locator("span")
                .filter(new Locator.FilterOptions().setHasText(fromCurrency))
                .filter(new Locator.FilterOptions().setHasText(toCurrency))
                .first();

        String text = locator.innerText();
        Matcher matcher = RATE_VALUE_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("Could not extract rate from text: " + text);
        }
        return matcher.group(1);
    }

    public String getFirstBigRateValue() {
        Locator values = page.locator("p[class*='result__BigRate']");
        if (values.count() == 0) {
            throw new IllegalStateException("No conversion result found on the page");
        }
        return values.first().innerText().trim();
    }

    public void waitForResultToRender() {
        page.locator("p[class*='result__BigRate']").first()
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(15_000));
    }

    private void selectCurrency(String currency, Locator dropdown, int searchBoxIndex, String listboxSelector) {
        String currencyCode = currency.trim().toLowerCase();

        dropdown.click();
        Locator searchBox = page.getByPlaceholder("Type to search...").nth(searchBoxIndex);
        assertThat(searchBox).isVisible();
        searchBox.click();
        searchBox.fill("");
        searchBox.pressSequentially(currencyCode, new Locator.PressSequentiallyOptions().setDelay(150));

        Locator option = page.locator(listboxSelector);
        Locator optionItem = option.locator(String.format("img[alt='%s']", currencyCode)).first();
        optionItem.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10_000));
        optionItem.scrollIntoViewIfNeeded();
        optionItem.click();
    }
}
