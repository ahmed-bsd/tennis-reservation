package com.tennis.weather;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeatherService {

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
}
