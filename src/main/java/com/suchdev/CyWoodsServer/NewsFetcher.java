package com.suchdev.CyWoodsServer;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NewsFetcher {
	
	// URLS
	public static final String SCHOOL_URL = "http://cywoods.cfisd.net/en/";
	public static final String SCHOOL_NEWS_URL = "http://cywoods.cfisd.net/en/news/school-news/";
	public static final String CRIMSON_CONNECTION_URL = "http://www.thecrimsonconnection.com/category/news/cy-woods/";
	public static final String APP_NEWS_PATH = "AppNews.txt";
	
	// News Items
	private LinkedHashSet<SchoolNewsItem> schoolNews;
	private LinkedHashSet<CrimsonConnectionNewsItem> crimsonConnectionNews;
	private LinkedHashSet<AppNewsItem> appNews;
	
	public NewsFetcher() {
		schoolNews = new LinkedHashSet<SchoolNewsItem>();
		crimsonConnectionNews = new LinkedHashSet<CrimsonConnectionNewsItem>();
		appNews = new LinkedHashSet<AppNewsItem>();
	}
	
	public LinkedHashSet<SchoolNewsItem> getSchoolNews() {
		return schoolNews;
	}
	
	public LinkedHashSet<CrimsonConnectionNewsItem> getCrimsonConnectionNews() {
		return crimsonConnectionNews;
	}
	
	public LinkedHashSet<AppNewsItem> getAppNews() {
		return appNews;
	}
	
	public void fetchSchoolNewsFeed(Runnable function) throws Exception {
		schoolNews = new LinkedHashSet<SchoolNewsItem>();
		for (int page = 0; page <= 5; page++) {
			Document newsDoc = Jsoup.connect(SCHOOL_NEWS_URL)
									.data("ccm_paging_p_b1401", page + "")
									.method(Method.POST)
									.execute()
									.parse();  
	        Elements nodes = newsDoc.select(".index-item");
	        for (Element node : nodes) {
				Elements element = node.select("div");
				if (element.size() == 0) continue;
				else {
					String title = element.select(".item-title").text();
					String date = element.select(".item-date").text();
					String url = SCHOOL_URL.replace("/en/", "") + element.select("a").attr("href");
					schoolNews.add(new SchoolNewsItem(title, date, url));
				}
	        }
		}
        function.run();
	}
	
	public void fetchCrimsonConnectionFeed(Runnable function) throws Exception {
		crimsonConnectionNews = new LinkedHashSet<CrimsonConnectionNewsItem>();
        Document newsDoc = Jsoup.connect(CRIMSON_CONNECTION_URL).get();
        Elements nodes = newsDoc.select(".sno-animate");
        for (Element node : nodes) {
        	String title = node.select(".searchheadline").text();
        	String date = node.select(".categorydate").text();
        	String url = node.select("a").attr("href");
        	try {
	        	CrimsonConnectionNewsItem newsItem = new CrimsonConnectionNewsItem(title, date, url);
	        	if (!crimsonConnectionNews.contains(newsItem))
	        		crimsonConnectionNews.add(newsItem);
        	} catch (ParseException e) {
        		continue;
        	}
        }
        function.run();
	}
	
	public void fetchAppNewsFeed(Runnable function) throws Exception {
		appNews = new LinkedHashSet<AppNewsItem>();
		File appNewsFile = new File(APP_NEWS_PATH);
        Scanner in = new Scanner(appNewsFile);
        String[] keys = in.nextLine().replaceAll("[\"\"]", "").split("[= ]");
        String newKeyParams = "";
        if (keys.length == 4 && keys[0].equals("oldKey") && keys[2].equals("key") && (new BigInteger(keys[1]).shiftLeft(3).add(new BigInteger("2"))).toString().substring(0, 20).equals(keys[3])) {
        	String newKey = (new BigInteger(keys[3]).shiftLeft(3).add(new BigInteger("2"))).toString().substring(0, 20);
        	newKeyParams = "oldKey=\"" + keys[3] + "\" key=\"" + newKey + "\"";
        	
	        List<String> lines = new ArrayList<String>();
	        while (in.hasNext()) {
	        	String title = in.nextLine();
	        	lines.add(title);
	        	String date = in.nextLine();
	        	lines.add(date);
	        	
	        	AppNewsItem newsItem = new AppNewsItem(title, date);
	        	appNews.add(newsItem);
	        }
	        
	        FileWriter writer = new FileWriter(appNewsFile);
	        writer.write(newKeyParams + "\n");
	        for (String line : lines) writer.write(line + "\n");
	        in.close();
	        writer.close();
        } else in.close();
        
        function.run();
	}
}