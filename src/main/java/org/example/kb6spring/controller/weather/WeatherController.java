package org.example.kb6spring.controller.weather;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.dto.forecast.ForecastDto;
import org.example.kb6spring.dto.weather.WeatherDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Api(tags = "날씨 컨트롤러")
@RestController
@Slf4j
@RequiredArgsConstructor
@CrossOrigin("*")
@RequestMapping("/weather")
public class WeatherController {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String URI = "https://api.openweathermap.org/data/2.5/weather";
    private final String API_KEY = "590c223343799397b190b4fa8894ebf2";
    private final String UNITS = "metric";
    private final String LANG = "kr";

    @ApiOperation(value = "도시의 현재 날씨", notes = "전달 받은 도시의 현재 날씨 데이터 API")
    @GetMapping("/{city}")
    public ResponseEntity<WeatherDto> getWeather(
            @ApiParam(value = "도시명", required = true, example = "seoul")
            @PathVariable String city
    ) {
        String url = UriComponentsBuilder.fromHttpUrl(URI)
                .queryParam("q", city)
                .queryParam("units", UNITS)
                .queryParam("APPID", API_KEY)
                .queryParam("lang", LANG)
                .toUriString();

        WeatherDto weather = restTemplate.getForObject(url, WeatherDto.class);
        return ResponseEntity.ok(weather);
    }

    private final String FORECAST_URI = "http://api.openweathermap.org/data/2.5/forecast";

    @ApiOperation(value = "도시의 날씨 예보", notes = "전달 받은 도시의 5일간의 날씨 예보 데이터 API")
    @GetMapping("/forecast/{city}")
    public ResponseEntity<ForecastDto> getForecast(
            @ApiParam(value = "도시명", required = true, example = "seoul")
            @PathVariable String city
    ) {
        String url = UriComponentsBuilder.fromHttpUrl(FORECAST_URI)
                .queryParam("q", city)
                .queryParam("units", UNITS)
                .queryParam("APPID", API_KEY)
                .queryParam("lang", LANG)
                .toUriString();

        ForecastDto forecast = restTemplate.getForObject(url, ForecastDto.class);
        return ResponseEntity.ok(forecast);
    }
}
