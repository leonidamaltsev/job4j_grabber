package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class AlertRabbit {

    public static Properties rabbitProperties() {
        Properties config = new Properties();
        try (InputStream in = AlertRabbit.class
                .getClassLoader().getResourceAsStream("rabbit.properties")) {
            config.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    public static Connection init() throws ClassNotFoundException, SQLException {
        Class.forName(rabbitProperties().getProperty("driver"));
        String url = rabbitProperties().getProperty("url");
        String login = rabbitProperties().getProperty("login");
        String pass = rabbitProperties().getProperty("password");
        Connection connection = DriverManager.getConnection(url, login, pass);
        return connection;
    }

    public static void main (String[]args) {
        int interval = Integer.parseInt(rabbitProperties().getProperty("rabbit.interval"));
        try (Connection connection = init()) {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("connection", connection);
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(interval)
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    public static class Rabbit implements Job {

        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            context.getJobDetail().getJobDataMap().get("connection");
            try (PreparedStatement preparedStatement = init()
                    .prepareStatement("insert into rabbit(created_date) values (?);",
                    Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                preparedStatement.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}