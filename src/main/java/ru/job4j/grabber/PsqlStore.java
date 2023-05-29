package ru.job4j.grabber;

import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Метод save() - сохраняет объявление в базе
 * Метод getAll() - извлекает объявления из базы.
 * Метод findById(int id) - извлекает объявление из базы по id.
 */

public class PsqlStore implements Store, AutoCloseable {

    private final Connection cnn;

    public PsqlStore(Properties cfg) throws SQLException {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        cnn = DriverManager.getConnection(
                cfg.getProperty("url"),
                cfg.getProperty("username"),
                cfg.getProperty("password")
        );
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement prs = cnn.prepareStatement(
                "insert into post(name, link, text, created)"
                        + "values (?, ?, ?, ?) on conflict (link) do nothing;",
                Statement.RETURN_GENERATED_KEYS)) {
            prs.setString(1, post.getTitle());
            prs.setString(2, post.getLink());
            prs.setString(3, post.getDescription());
            prs.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            prs.execute();
            try (ResultSet generatedKey = prs.getGeneratedKeys()) {
                if (generatedKey.next()) {
                    post.setId(generatedKey.getInt(1));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> allPost = new ArrayList<>();
        try (PreparedStatement prs = cnn.prepareStatement("select * from post")) {
            try (ResultSet resultSet = prs.executeQuery()) {
                while (resultSet.next()) {
                    allPost.add(createPost(resultSet));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allPost;
    }

    @Override
    public Post findById(int id) {
        Post idPost = new Post();
        try (PreparedStatement prs = cnn.prepareStatement("select * from post where id = ?")) {
            prs.setInt(1, id);
            try (ResultSet resultSet = prs.executeQuery()) {
                if (resultSet.next()) {
                    idPost = createPost(resultSet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idPost;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    private static Post createPost(ResultSet resultSet) throws SQLException {
        return new Post(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("link"),
                resultSet.getString("text"),
                resultSet.getTimestamp("created").toLocalDateTime());
    }


    public static void main(String[] args) {
        Properties cfg = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader()
                .getResourceAsStream("PsqlStore.properties")) {
            cfg.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (PsqlStore store = new PsqlStore(cfg)) {
            HabrCareerParse parser = new HabrCareerParse(new HabrCareerDateTimeParser());
            String sourceLink = "https://career.habr.com";
            String pageLink =
                    String.format("%s/vacancies/java_developer?page=1", sourceLink);
            List<Post> download = parser.list(pageLink);
            store.save(download.get(1));
            store.save(download.get(2));
            List<Post> allPost = store.getAll();
            Post idPost = store.findById(2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
