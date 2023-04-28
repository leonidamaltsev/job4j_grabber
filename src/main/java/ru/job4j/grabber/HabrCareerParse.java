package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer?page=", SOURCE_LINK);

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    private static String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Element vacancyDescription = document.select(".style-ugc").first();
        return String.format("%s %s%n", "Описание вакансии :", vacancyDescription.text());
    }

    @Override
    public List<Post> list(String link) throws IOException {
        List<Post> listPosts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String pageLink = String.format("%s%d", PAGE_LINK, i);
            Connection connection = Jsoup.connect(pageLink);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                String title = titleElement.text();
                String linkVacancy = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                String description = null;
                try {
                    description = retrieveDescription(linkVacancy);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String date = row.select(".vacancy-card__date").first().child(0).attr("datetime");
                LocalDateTime dateVacancy = new HabrCareerDateTimeParser().parse(date);
                listPosts.add(new Post(title, linkVacancy, description, dateVacancy));
            });
        }
        return listPosts;
    }

    public static void main(String[] args) throws IOException {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        System.out.println(habrCareerParse.list(PAGE_LINK));
    }
}