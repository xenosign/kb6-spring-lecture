package org.example.kb6spring.dto.forecast;

import lombok.Data;

import java.util.List;

@Data
public class ForecastDto {
	private City city;
	private int cnt;
	private String cod;
	private int message;
	private List<ListItem> list;
}