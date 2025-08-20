package org.example;


import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TicketAnalyzer {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Пожалуйста, укажите путь к файлу tickets.json");
            return;
        }

        try {
            List<JSONObject> tickets = readTicketsFromFile(args[0]);
            List<JSONObject> vvoTlvTickets = filterTickets(tickets, "VVO", "TLV");

            if (vvoTlvTickets.isEmpty()) {
                System.out.println("Не найдено билетов между Владивостоком и Тель-Авивом");
                return;
            }

            for (JSONObject ticket : vvoTlvTickets) {
                int duration = calculateDuration(
                        ticket.getString("departure_date"),
                        ticket.getString("departure_time"),
                        ticket.getString("arrival_date"),
                        ticket.getString("arrival_time")
                );
                ticket.put("duration", duration);
            }

            Map<String, Integer> minFlightTimes = calculateMinFlightTimes(vvoTlvTickets);
            double priceDifference = calculatePriceDifference(vvoTlvTickets);

            printResults(minFlightTimes, priceDifference);

        } catch (Exception e) {
            System.out.println("Ошибка при обработке файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* В этом методе происходит парсинг JSON файла,
    из исходных данных при помощи Stream API получаем list из JSON объектов
     */
    private static List<JSONObject> readTicketsFromFile(String filePath) throws Exception {
        JSONTokener tokener = new JSONTokener(new FileReader(filePath));
        JSONObject json = new JSONObject(tokener);
        JSONArray ticketsArray = json.getJSONArray("tickets");

        return IntStream.range(0, ticketsArray.length())
                .mapToObj(ticketsArray::getJSONObject)
                .collect(Collectors.toList());
    }

    /*
    Здесь происходит фильтрация маршрутов, отбираются только те, которые сооветствуют входным параметрам
     */
    private static List<JSONObject> filterTickets(List<JSONObject> tickets,
                                                  String origin,
                                                  String destination) {
        return tickets.stream()
                .filter(t -> t.getString("origin").equals(origin) &&
                        t.getString("destination").equals(destination))
                .collect(Collectors.toList());
    }
// Вычисление времени полета
private static int calculateDuration(String departureDate, String departureTime,
                                     String arrivalDate, String arrivalTime) {
    try {
        String[] depParts = departureTime.split(":");
        String[] arrParts = arrivalTime.split(":");

        int depHour = Integer.parseInt(depParts[0]);
        int depMinute = Integer.parseInt(depParts[1]);
        int arrHour = Integer.parseInt(arrParts[0]);
        int arrMinute = Integer.parseInt(arrParts[1]);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        LocalDateTime departure = LocalDate.parse(departureDate, dateFormatter)
                .atTime(depHour, depMinute);
        LocalDateTime arrival = LocalDate.parse(arrivalDate, dateFormatter)
                .atTime(arrHour, arrMinute);

        return (int) Duration.between(departure, arrival).toMinutes();

    } catch (Exception e) {
        System.out.println("Ошибка парсинга времени: " + e.getMessage());
        return 0;
    }
}

    // Расчет минимального времени полета для каждого перевозчика
    private static Map<String, Integer> calculateMinFlightTimes(List<JSONObject> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getString("carrier"),
                        Collectors.collectingAndThen(
                                Collectors.mapping(
                                        t -> t.getInt("duration"),
                                        Collectors.minBy(Integer::compare)
                                ),
                                opt -> opt.orElse(0)
                        )
                ));
    }

    // Расчет разницы между средней ценой и медианой
    private static double calculatePriceDifference(List<JSONObject> tickets) {
        List<Integer> prices = tickets.stream()
                .map(t -> t.getInt("price"))
                .sorted()
                .collect(Collectors.toList());

        double average = prices.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        double median = calculateMedian(prices);

        return average - median;
    }

    // Операция, в которой высчитывается медиана,была вынесена в отдельную функцию
    private static double calculateMedian(List<Integer> sortedPrices) {
        int size = sortedPrices.size();
        if (size % 2 == 0) {
            return (sortedPrices.get(size/2 - 1) + sortedPrices.get(size/2)) / 2.0;
        } else {
            return sortedPrices.get(size/2);
        }
    }

    // Вывод результата в консоль
    private static void printResults(Map<String, Integer> minFlightTimes, double priceDifference) {
        System.out.println("Минимальное время полёта между Владивостоком и Тель-Авивом:");
        minFlightTimes.forEach((carrier, time) ->
                System.out.printf("%s: %d минут%n", carrier, time));

        System.out.printf("%nРазница между средней ценой и медианой: %.2f%n", priceDifference);
    }
}