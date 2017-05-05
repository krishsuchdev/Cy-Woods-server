package com.suchdev.CyWoodsServer;

import java.text.SimpleDateFormat;

public class SchoolNewsItem extends NewsItem {
	public SchoolNewsItem(String title, String date, String url) throws Exception {
		super("School News", title, new SimpleDateFormat("MMM d y").parse(date.replaceAll("[\\.,]", "")), url);
	}
}
