package org.example.kb6spring.dto.weather;

public class WeatherItem{
	private String icon;
	private String description;
	private String main;
	private int id;

	public String getIcon(){
		return icon;
	}

	public String getDescription(){
		return description;
	}

	public String getMain(){
		return main;
	}

	public int getId(){
		return id;
	}
}
