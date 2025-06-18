package org.example.kb6spring.dto.forecast;

import lombok.Data;

@Data
public class City{
	private String country;
	private Coord coord;
	private int sunrise;
	private int timezone;
	private int sunset;
	private String name;
	private int id;
	private int population;
}