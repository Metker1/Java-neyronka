import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.PrintWriter;
import java.time.Duration;

public class Main1 {

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        // options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        WebDriver driver = new ChromeDriver(options);

        // ⏱️ Устанавливаем таймауты для драйвера
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

        try {
            System.out.println("🔄 Открываем сайт (макс. 10 сек на загрузку)...");

            // 🔥 Попытка загрузить страницу с таймаутом 10 секунд
            try {
                driver.get("https://chat.qwen.ai");
            } catch (org.openqa.selenium.TimeoutException e) {
                System.out.println("⚠️ Страница не загрузилась за 10 сек, продолжаем...");
            }

            // Даём ещё 2 секунды на инициализацию JS
            Thread.sleep(2000);

            System.out.println("✅ Страница открыта, ждём 10 секунд перед отправкой...");

            // 🔥 ГЛАВНОЕ: Ждём 10 секунд перед отправкой
            Thread.sleep(10000);

            System.out.println("✅ Отправляем сообщение...");

            // 🔍 Поиск поля ввода
            WebElement input = null;
            String[] inputSelectors = {
                    "textarea[placeholder*='Message']",
                    "textarea[placeholder*='сообщение']",
                    "textarea[aria-label]",
                    "div[contenteditable='true']",
                    "textarea"
            };

            for (String selector : inputSelectors) {
                try {
                    input = driver.findElement(By.cssSelector(selector));
                    if (input != null && input.isDisplayed()) break;
                } catch (Exception e) {}
            }

            if (input != null) {
                input.click();
                Thread.sleep(500);
                input.sendKeys("Напиши 5 матча на теннис сегодняшних матчей с прогнозом на ставку строго в таком формате по этому примеру и ничего больше лишнего не пиши - Офнер С.;Бранкаччио Р.;П1  друг под другом П1 ИЛИ П2  пиши в зависимости от твоего прогноза и анализа,на русском языке");
                Thread.sleep(500);
                input.sendKeys(Keys.RETURN);
                System.out.println("✉️ Сообщение отправлено!");

                // ⏳ Ждём ответ от нейросети — 40 секунд
                System.out.println("⏳ Ждём ответ от нейросети (до 60 сек)...");

                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
                WebElement answerElement = null;

                try {
                    // Вариант 1: Точный селектор с двумя классами
                    answerElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.qwen-markdown.qwen-markdown-loose")
                    ));
                } catch (Exception e1) {
                    try {
                        // Вариант 2: Только первый класс
                        answerElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("div.qwen-markdown")
                        ));
                    } catch (Exception e2) {
                        try {
                            // Вариант 3: XPath по классу
                            answerElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.xpath("//div[contains(@class, 'qwen-markdown')]")
                            ));
                        } catch (Exception e3) {
                            System.out.println("⚠️ Элемент ответа не найден по селекторам");
                        }
                    }
                }

                // Если нашли элемент — ждём окончания генерации текста
                if (answerElement != null) {
                    // Ждём, пока текст перестанет меняться (конец стриминга)
                    waitForTextStable(answerElement, 15);

                    String answer = answerElement.getText().trim();

                    if (!answer.isEmpty()) {
                        System.out.println("\n✅ ОТВЕТ ПОЛУЧЕН:");
                        System.out.println("=".repeat(60));
                        System.out.println(answer);
                        System.out.println("=".repeat(60));

                        // 🔥 Сохраняем в matches.txt (с ПЕРЕЗАПИСЬЮ)
                        saveToMatchesFile(answer);

                    } else {
                        System.out.println("⚠️ Элемент найден, но текст пустой");
                    }
                } else {
                    System.out.println("❌ Ответ не найден за 40 секунд");
                }

            } else {
                System.out.println("❌ Поле ввода не найдено");
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { Thread.sleep(1000); } catch (Exception e) {}
            driver.quit();
            System.out.println("✅ Завершено");
        }
    }

    // ⏳ Ждём, пока текст в элементе перестанет меняться (конец стриминга)
    private static void waitForTextStable(WebElement element, int maxSeconds) {
        try {
            String lastText = element.getText();
            int unchangedCount = 0;

            for (int i = 0; i < maxSeconds * 2; i++) {
                Thread.sleep(500);
                String currentText = element.getText();

                if (currentText.equals(lastText) && !currentText.isEmpty()) {
                    unchangedCount++;
                    if (unchangedCount >= 3) {
                        break;
                    }
                } else {
                    unchangedCount = 0;
                    lastText = currentText;
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки ожидания
        }
    }

    // 💾 Сохранение ответа в matches.txt (с ПЕРЕЗАПИСЬЮ)
    private static void saveToMatchesFile(String content) {
        try (PrintWriter writer = new PrintWriter("matches.txt", "UTF-8")) {
            writer.print(content);
            System.out.println("💾 Ответ сохранён в файл: matches.txt (перезаписан)");
        } catch (Exception e) {
            System.err.println("❌ Ошибка сохранения в файл: " + e.getMessage());
        }
    }
}