package ru.thesn.app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Main {

    private static final String HEADHUNTER_URL_TEMPLATE = "https://m.hh.ru/vacancies?text=java&page=";

    private static int IO_ERRORS_COUNT = 0;

    private static final Pattern DESCRIPTION_SPLIT_PATTERN = Pattern.compile("[^A-Za-z0-9 ]|( and )|( or )");
    private static final Pattern DESCRIPTION_REPLACE_PATTERN = Pattern.compile("</?.*?>");

    public static void main(String[] args) {


        IntStream
                .rangeClosed(0, 99)
                .boxed()
                .map(i -> HEADHUNTER_URL_TEMPLATE + i)
                .map(Main::getDocument)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(d -> d.getElementsByClass("vacancy-list-item-link"))
                .flatMap(Collection::stream)
                .map(d -> d.attr("href"))
                .distinct()
                .map(Main::getDocument)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Main::getEntryUrl)
                .filter(Main::isRussianText)
                .map(DESCRIPTION_SPLIT_PATTERN::split)
                .flatMap(Arrays::stream)
                .map(String::trim)
                .filter(s -> s.length() > 1)
                .map(String::toLowerCase)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .limit(2000)
                .forEach(e -> System.out.printf("%s - %s%n", e.getKey(), e.getValue()));


        System.out.println("\nIO errors: " + IO_ERRORS_COUNT);
    }

    private static Optional<Document> getDocument(String url){
        Optional<Document> document = Optional.empty();
        try {
            document = Optional.of(Jsoup.connect(url).get());
        } catch (IOException e) {
            IO_ERRORS_COUNT++;
        }
        return document;
    }

    private static boolean isRussianText(String text) {
        long rusLettersCount = text
                .chars()
                .filter(i -> i >= 'а' && i <= 'я')
                .count();
        return (text.length() - rusLettersCount) / (text.length() * 1.) < 0.5;
    }

    private static String getEntryUrl(Document document) {
        return DESCRIPTION_REPLACE_PATTERN.matcher(
                document.getElementsByClass("vacancy__description").first().html()
        ).replaceAll("");
    }

}
