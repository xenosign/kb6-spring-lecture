package org.example.kb6spring.dto.forecast;

import lombok.Data;

@Data
public class WeatherItem{
	private String icon;
	private String description;
	private String main;
	private int id;
}