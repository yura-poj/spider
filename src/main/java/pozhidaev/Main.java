package pozhidaev;


import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Spider http://localhost:8080");
            return;
        }

        Spider spider = new Spider(args[0]);
        List<String> messages = spider.start();

        messages.forEach(System.out::println);
    }
}