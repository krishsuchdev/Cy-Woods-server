package com.suchdev.CyWoodsServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.gson.Gson;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference.CompletionListener;

@SuppressWarnings({ "serial", "unused" })
public class MainServlet extends HttpServlet {
	
	FirebaseDatabase database;
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		NewsFetcher newsFetcher = new NewsFetcher();
		GradeFetcher gradeFetcher = new GradeFetcher();
		AthleticsFetcher athleticsFetcher = new AthleticsFetcher();
		
		Gson gson;
		String jsonInString;
		
		User currentUser = gradeFetcher.getUser();
		Date currentDate = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
		
		String action = req.getParameter("Action");
		switch (action) {
		case "HNE": // Home - News
			// Authorize Key (needs to be changed every once in a while)
			//if (!Utility.authorize(req.getParameter("Key"))) return;
			
			try {
				newsFetcher.fetchSchoolNewsFeed(null);
				newsFetcher.fetchAppNewsFeed(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			Map<String, Object> newsDictionary = new HashMap<String, Object>();
			newsDictionary.put("school news", newsFetcher.getSchoolNews());
			
			gson = new Gson();
			jsonInString = gson.toJson(newsDictionary);
			resp.getWriter().println(jsonInString);
			break;
		case "GGP": // Grades - Grading Period
			
			// Authorize Key (needs to be changed every once in a while)
			if (!Utility.authorize(req.getParameter("Key"))) return;
			
			try {
				gradeFetcher.login(req.getParameter("Username"), req.getParameter("Password"));
				gradeFetcher.fetchGradingPeriodGrades();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			currentUser = gradeFetcher.getUser();
			
			Map<String, Object> nineWeeksDictionary = new HashMap<String, Object>();
			nineWeeksDictionary.put("username", currentUser.username);
			nineWeeksDictionary.put("name", currentUser.name);
			nineWeeksDictionary.put("nine weeks grades", currentUser.classes);
			
			currentDate = new Date();
			nineWeeksDictionary.put("last updated", format.format(currentDate));
			
			gson = new Gson();
			jsonInString = gson.toJson(nineWeeksDictionary);
			resp.getWriter().println(jsonInString);
			break;
		case "GSM": // Grades - Semester Grades
			// Authorize Key (needs to be changed every once in a while)
			if (!Utility.authorize(req.getParameter("Key"))) return;
			
			try {
				gradeFetcher.login(req.getParameter("Username"), req.getParameter("Password"));
				gradeFetcher.fetchSemesterGrades();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			currentUser = gradeFetcher.getUser();
			
			Map<String, Object> reportCardDictionary = new HashMap<String, Object>();
			reportCardDictionary.put("username", currentUser.username);
			reportCardDictionary.put("report card", currentUser.reportCard);
			
			currentDate = new Date();
			reportCardDictionary.put("last updated", format.format(currentDate));
			
			gson = new Gson();
			jsonInString = gson.toJson(reportCardDictionary);
			resp.getWriter().println(jsonInString);
			break;
		case "GAT": // Grades - Attendance
			
			// Authorize Key (needs to be changed every once in a while)
			if (!Utility.authorize(req.getParameter("Key"))) return;
			
			try {
				gradeFetcher.login(req.getParameter("Username"), req.getParameter("Password"));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			Map<String, Object> attendanceDictionary = new HashMap<String, Object>();
			attendanceDictionary.put("username", currentUser.username);
			attendanceDictionary.put("absences", new HashMap<Object, Object>());
			
			currentDate = new Date();
			attendanceDictionary.put("last updated", format.format(currentDate));
			
			gson = new Gson();
			jsonInString = gson.toJson(attendanceDictionary);
			resp.getWriter().println(jsonInString);
			break;
		case "ASC": // Athletics - Schedule
			
			// Authorize Key (needs to be changed every once in a while)
			if (!Utility.authorize(req.getParameter("Key"))) return;
			
			try {
				athleticsFetcher.fetchSport(req.getParameter("Sport"), null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			Map<String, Object> athleticsDictionary = new HashMap<String, Object>();
			athleticsDictionary.put("schedule", athleticsFetcher.getAthleticGames());
			
			gson = new Gson();
			jsonInString = gson.toJson(athleticsDictionary);
			resp.getWriter().println(jsonInString);
			break;
		default:
			break;
		}
	}
}
