package org.example.kb6spring.dto.forecast;

import lombok.Data;

@Data
public class Main{
	private double temp;
	private double tempMin;
	private int grndLevel;
	private double tempKf;
	private int humidity;
	private int pressure;
	private int seaLevel;
	private double feelsLike;
	private double tempMax;
}