package org.example.kb6spring.dto.forecast;

import lombok.Data;

import java.util.List;

@Data
public class ListItem{
	private int dt;
	private double pop;
	private Rain rain;
	private int visibility;
	private String dtTxt;
	private List<WeatherItem> weather;
	private Main main;
	private Clouds clouds;
	private Sys sys;
	private Wind wind;
}