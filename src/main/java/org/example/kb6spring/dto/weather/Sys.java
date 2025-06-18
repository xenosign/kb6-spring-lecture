package org.example.kb6spring.dto.weather;

public class Sys{
	private String country;
	private int sunrise;
	private int sunset;
	private int id;
	private int type;

	public String getCountry(){
		return country;
	}

	public int getSunrise(){
		return sunrise;
	}

	public int getSunset(){
		return sunset;
	}

	public int getId(){
		return id;
	}

	public int getType(){
		return type;
	}
}
