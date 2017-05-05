package com.suchdev.CyWoodsServer;

import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class AthleticsFetcher {
	
	// URLS
	public static final String FOOTBALL_VARSITY_URL = "https://www.rankonesport.com/Schedules/View_Schedule.aspx?P=0&D=5E3C64F6-EABB-401D-8901-2DA09A8500C8&S=1017&Sp=3&Tm=20709&L=1";
	public static final String BASKETBALL_VARSITY_MENS_URL = "https://www.rankonesport.com/Schedules/View_Schedule.aspx?P=0&D=5E3C64F6-EABB-401D-8901-2DA09A8500C8&S=1017&Sp=7&Tm=20703&L=1";
	public static final String BASKETBALL_VARSITY_WOMENS_URL = "https://www.rankonesport.com/Schedules/View_Schedule.aspx?P=0&D=5E3C64F6-EABB-401D-8901-2DA09A8500C8&S=1017&Sp=26&Tm=20732&L=1";
	public static final String SOCCER_VARSITY_MENS_URL = "";
	public static final String SOCCER_VARSITY_WOMENS_URL = "https://www.rankonesport.com/Schedules/View_Schedule.aspx?P=0&D=5E3C64F6-EABB-401D-8901-2DA09A8500C8&S=1017&Sp=25&Tm=20742&L=1";
	
	// News Items
	private ArrayList<AthleticGame> athleticGames;
	
	public AthleticsFetcher() {
		athleticGames = new ArrayList<AthleticGame>();
	}
	
	public ArrayList<AthleticGame> getAthleticGames() {
		return athleticGames;
	}
	
	public void fetchSport(String sport, Runnable function) throws Exception {
		athleticGames = new ArrayList<AthleticGame>();
		
		String url = "";
		switch (sport) {
		case "Football":
			url = FOOTBALL_VARSITY_URL;
			break;
		case "MBasketball":
			url = BASKETBALL_VARSITY_MENS_URL;
			break;
		case "WBasketball":
			url = BASKETBALL_VARSITY_WOMENS_URL;
			break;
		case "MSoccer":
			url = SOCCER_VARSITY_MENS_URL;
			break;
		case "WSoccer":
			url = SOCCER_VARSITY_WOMENS_URL;
			break;
		default:
			break;
		}
		
		if (url.isEmpty()) {
			athleticGames.add(new AthleticGame(0, "N/A", "N/A", "No Data", "Nowhere?", true, "0 - 0"));
			return;
		}
		
		Document athleticDocs = Jsoup.connect(url).get();
		ArrayList<String> columnTitles = new ArrayList<String>();
		for (Element columnTitle : athleticDocs.select("tr").get(0).select("div")) {
			String columnText = columnTitle.text().trim().replaceAll("[-]", "");
        	if (!columnText.isEmpty()) columnTitles.add(columnText);
		}
		columnTitles.remove(0);
        for (Element node : athleticDocs.select("td")) {
        	HashMap<String, String> gameInfo = new HashMap<String, String>();
        	int columnNumber = 0;
			for (Element infoNode : node.select("span")) {
				String infoText = infoNode.text().trim();
				if (!infoNode.text().contains("Summary") && !(infoNode.attr("id").contains("SpecialNote"))) {
					String columnTitle = "";
					do {
						columnTitle = columnTitles.get(columnNumber++);
					} while (columnTitle.contains("Location") && infoText.matches("[0-9]+ - [0-9]+"));
					gameInfo.put(columnTitle, infoText);
				}
			}
			if (gameInfo.get("Opponent").isEmpty()) gameInfo.put("Opponent", "TBD");
			gameInfo.put("Opponent", gameInfo.get("Opponent").replaceAll("Cy ", "Cy-").replaceAll("Cypress([- ])(.+)", "Cy-$2"));
			athleticGames.add(new AthleticGame(athleticGames.size(), gameInfo.get("Date"), gameInfo.get("Time"), gameInfo.get("Opponent"), gameInfo.get("Location/Map"), gameInfo.get("Type").toUpperCase().startsWith("H"), gameInfo.get("Score")));
        }
        function.run();
	}
}