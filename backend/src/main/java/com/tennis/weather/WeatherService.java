package com.tennis.weather;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

    /*
    private final WebClient.Builder webClientBuilder;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url}")
    private String url;

    public String getCurrentWeather(String city) {
        System.out.println("weather api : "+apiKey+" url : "+url);

        Map response = webClientBuilder.build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.openweathermap.org")
                        .path("/data/2.5/weather")
                        .queryParam("q", city)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .build()
                )
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map weather = ((java.util.List<Map>) response.get("weather")).get(0);

        return (String) weather.get("main");
        // ex: Rain, Clear, Clouds
    }

     */


        private final WebClient webClient = WebClient.create();

        public Map<String, Map<String, Object>> getHourlyWeather(double lat, double lon) {

            String url = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + lat
                    + "&longitude=" + lon
                    + "&hourly=temperature_2m,precipitation_probability,wind_speed_10m"
                    + "&forecast_days=2"
                    + "&timezone=Africa/Tunis";

            Map response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return buildHourlyMap(response);
        }

        private Map<String, Map<String, Object>> buildHourlyMap(Map response) {

            Map hourly = (Map) response.get("hourly");

            List<String> time = (List<String>) hourly.get("time");
            List<Double> temp = (List<Double>) hourly.get("temperature_2m");
            List<Double> rain = (List<Double>) hourly.get("precipitation_probability");
            List<Double> wind = (List<Double>) hourly.get("wind_speed_10m");

            Map<String, Map<String, Object>> result = new HashMap<>();

            for (int i = 0; i < time.size(); i++) {

                String t = time.get(i); // "2026-06-03T08:00"
                String hourStr = t.substring(11, 13); // "08:00"
                int hour = Integer.parseInt(hourStr.substring(0, 2));

                if (hour < 8 || hour > 21) {
                    continue;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("temp", temp.get(i));
                data.put("rain", rain.get(i));
                data.put("wind", wind.get(i));

                result.put(hourStr, data);
            }

            return result;
        }
    
}
