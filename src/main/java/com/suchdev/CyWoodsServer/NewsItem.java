package com.suchdev.CyWoodsServer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NewsItem implements Comparable<NewsItem> {
	public String Source;
	
	public String Title;
	public String Date;
	
	public Integer Priority;
	
	public String URL;
	
	public NewsItem(String source, String title, Date date) {
		this.Source = source;
		this.Title = title;
		this.Date = new SimpleDateFormat("MMM d, y").format(date);
	}
	
	public NewsItem(String source, String title, Date date, String url) {
		this(source, title, date);
		this.URL = url;
	}

	@Override
	public int compareTo(NewsItem other) {
		SimpleDateFormat format = new SimpleDateFormat("MMM d, y");
		try {
			int compare = format.parse(other.Date).compareTo(format.parse(Date));
			if (compare == 0) compare = Title.compareTo(other.Title);
			return compare;
		} catch (Exception e) {
			return -1;
		}
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof NewsItem && compareTo((NewsItem)other) == 0;
	}
}
