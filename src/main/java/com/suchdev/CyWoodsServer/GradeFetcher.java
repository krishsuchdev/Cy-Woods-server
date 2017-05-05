package com.suchdev.CyWoodsServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.*;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GradeFetcher {
	
	// URLS
	public static final String HAC_ACCOUNT_LOGIN_URL = "https://home-access.cfisd.net/HomeAccess/Account/LogOn";
	public static final String HAC_SCHEDULE_URL = "https://home-access.cfisd.net/HomeAccess/Home/WeekView";
	public static final String HAC_ATTENDANCE_URL = "https://home-access.cfisd.net/HomeAccess/Content/Attendance/MonthlyView.aspx";
	public static final String HAC_ASSIGNMENTS_URL = "https://home-access.cfisd.net/HomeAccess/Content/Student/Assignments.aspx";
	public static final String HAC_REPORTCARD_URL = "https://home-access.cfisd.net/HomeAccess/Content/Student/ReportCards.aspx";
	
	// Cookies
	private String authCookie;
	private String sessionID;
	private String siteCode;
	
	// User
	private User currentUser;
	
	public GradeFetcher() {
		this.currentUser = new User();
		this.currentUser.classes = new ArrayList<Class>();
	}
	
	public User getUser() {
		return currentUser;
	}
	
	public boolean login(String username, String password) {
		this.currentUser = new User(Utility.decode(username), Utility.decode(password));
		
		if (this.currentUser.username.equals("s0") && this.currentUser.password.equals("test"))
			return true;
		
		Response hacResponse;
		try {
			hacResponse = Jsoup.connect(HAC_ACCOUNT_LOGIN_URL)
				.data("Database", "10")
				.data("LogOnDetails.UserName", this.currentUser.username)
				.data("LogOnDetails.Password", this.currentUser.password)
				.method(Method.POST)
				.timeout(20*1000)
				.ignoreContentType(true)
				.ignoreHttpErrors(true)
				.execute();
			hacResponse.parse();
			
			authCookie = hacResponse.cookie(".AuthCookie");
			sessionID = hacResponse.cookie("ASP.NET_SessionId");
			siteCode = hacResponse.cookie("SPIHACSiteCode");
		
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		return true;
	}
	
	public ArrayList<Class> fetchGradingPeriodGrades() throws Exception {
		if (this.currentUser.username.equals("s0")) {
			this.currentUser.name = "Wildcat";
			Class onlyClass = new Class("Computer Science I", new Teacher("Mr. Knapsack", "no-email-found"));
			onlyClass.grade = "100.00";
			onlyClass.assignments.add(new Assignment("Test", "Summative Assessment", "100.00", new HashMap<String, Object>()));
			onlyClass.categories.add(new Category("Summative Assessments", 100.0, 100.0));
			onlyClass.categories.get(0).calculateEstimations(onlyClass);
			this.currentUser.classes.add(onlyClass);
		} else {
			this.fetchSchedule();
			this.fetchAssignments();
		}
		return this.currentUser.classes;
	}
	
	public ReportCard fetchSemesterGrades() throws Exception {
		if (this.currentUser.username.equals("s0")) {
			ReportCard reportCard = new ReportCard();
			this.currentUser.reportCard = reportCard;
		} else {
			this.fetchReportCard();
		}
		return this.currentUser.reportCard;
	}
	
	public void fetchSchedule() throws Exception {
		Response hacResponse = Jsoup.connect(HAC_SCHEDULE_URL)
			.cookie(".AuthCookie", authCookie)
			.cookie("ASP.NET_SessionId", sessionID)
			//.cookie("SPIHACSiteCode", siteCode)
			.timeout(10*1000)
			.ignoreContentType(true)
			.ignoreHttpErrors(true)
			.execute();
		Document hacDocument = hacResponse.parse();
		
		this.currentUser.name = hacDocument.select(".sg-banner-menu-element").select("span").text();
		this.currentUser.classes = new ArrayList<Class>();
		
		//Elements classNodes = hacDocument.select(".sg-5px-margin");
		Elements classNodes = hacDocument.select("tr");
		for (Element classNode : classNodes) {
			if (classNode.select("a").isEmpty()) continue;
			String className = classNode.select("#courseName").text();
			String teacherName = classNode.select("#staffName").text();
			String teacherEmail = classNode.select("a").get(1).attr("href").split(":")[1];
			String grade = classNode.select("#average").text();
			if ("Lunch, Early Release, Late Arrival".contains(className)) continue;
			Class thisClass = new Class(className, new Teacher(teacherName, teacherEmail), grade);
			this.currentUser.classes.add(thisClass);
		}
		this.currentUser.classes = new ArrayList<Class>(new LinkedHashSet<Class>(this.currentUser.classes));
	}
	
	public void fetchAssignments() throws Exception {
		Response hacResponse = Jsoup.connect(HAC_ASSIGNMENTS_URL)
			.cookie(".AuthCookie", authCookie)
			.cookie("ASP.NET_SessionId", sessionID)
			//.cookie("SPIHACSiteCode", siteCode)
			.data("ctl00$plnMain$ddlReportCardRuns", "2")
			.timeout(10*1000)
			.ignoreContentType(true)
			.ignoreHttpErrors(true)
			.execute();
		Document hacDocument = hacResponse.parse();
		
		Elements classInfoNodes = hacDocument.select(".AssignmentClass");
		for (Element classInfoNode : classInfoNodes) {
			Elements sgNodes = classInfoNode.select(".sg-header-heading");
			if (sgNodes.isEmpty()) continue;
			String registeredClassName = sgNodes.get(0).text();
		    Matcher matcher = Pattern.compile("[A-z]").matcher(registeredClassName);
		    String className;
		    if (matcher.find()) {
		    	className = registeredClassName.substring(matcher.start());
		    } else {
		    	className = registeredClassName;
		    }
		    
		    int classIndex = this.currentUser.classes.indexOf(new Class(className));
		    if (classIndex == -1) continue;
		    else {
		    	Class useClass = this.currentUser.classes.get(classIndex);
		    	useClass.assignments = new ArrayList<Assignment>();
		    	useClass.categories = new ArrayList<Category>();
		    	
		    	String averageText = classInfoNode.select("span").get(1).text().replaceAll("[^0-9\\.-]", "").trim();
		    	if (averageText.isEmpty()) averageText = "- -";
		    	useClass.grade = averageText;
		    	
		    	Elements rowNodes = classInfoNode.select(".sg-asp-table-data-row");
		    	if (rowNodes.isEmpty()) continue;
	    		
	    		for (Element rowNode : rowNodes) {
	    			Elements assignmentNodes = rowNode.select("td");
	    			if (assignmentNodes.get(2).text().contains("*")) {
	    				// Assignment
	    				String assignmentName = assignmentNodes.get(2).text().replace("*", "").trim();
	    				String assignmentType = assignmentNodes.get(3).text().replace("*", "").trim();
	    				
	    				String assignmentPointsText = assignmentNodes.get(4).text().replace("*", "").trim();
	    				String assignmentTotalPointsText = assignmentNodes.get(5).text().replace("*", "").trim();
	    				
	    				HashMap<String, Object> extra = new HashMap<String, Object>();
	    				if (assignmentNodes.get(4).toString().contains("strike")) extra.put("strikeThrough", true);
	    				
	    				Assignment assignment;
	    				try {
	    					double assignmentGrade = Double.parseDouble(assignmentPointsText) / Double.parseDouble(assignmentTotalPointsText) * 100.0;
	    					assignment = new Assignment(assignmentName, assignmentType, assignmentGrade, extra);
	    				} catch (Exception e) {
	    					assignment = new Assignment(assignmentName, assignmentType, (assignmentPointsText.length() == 1 && "XZ".contains(assignmentPointsText)) ? assignmentPointsText : "- -", extra);
	    				};
	    				
	    				useClass.assignments.add(assignment);
	    			} else {
	    				// Category
	    				Elements categoryNodes = assignmentNodes;
	    				
	    				String categoryName = categoryNodes.get(0).text().trim();
	    				
	    				String categoryPointsText = categoryNodes.get(1).text().trim();
	    				String categoryTotalPointsText = categoryNodes.get(2).text().trim();
	    				double categoryAverage = -1.0;
	    				
	    				try {
	    					categoryAverage = Double.parseDouble(categoryPointsText) / Double.parseDouble(categoryTotalPointsText) * 100.0;
	    				} catch (Exception e) {};
	    				
	    				String categoryWeightText = categoryNodes.get(4).text().trim();
	    				double categoryWeight = 0.0;
	    				try {
	    					categoryWeight = Double.parseDouble(categoryWeightText);
	    				} catch (Exception e) {};
	    				
	    				Category category = new Category(categoryName, categoryAverage, categoryWeight);
	    				useClass.categories.add(category);
	    			}
	    		}
	    		
	    		for (int assignmentIndex = 0; assignmentIndex < useClass.assignments.size(); assignmentIndex++) {
	    			Assignment assignment = useClass.assignments.get(assignmentIndex);
	    			int duplicateSymbol = 2;
					while (useClass.assignments.indexOf(assignment) != useClass.assignments.lastIndexOf(assignment))
						assignment.name = assignment.name.replaceAll(" \\(\\d+\\)", "") + " (" + duplicateSymbol++ + ")";
	    		}
	    		
	    		for (Category category : useClass.categories) category.calculateEstimations(useClass);
		    }
		}
	}
	
	public HashMap<String, TreeMap<Integer, Integer>> fetchAbsences() {
		if (this.currentUser.username.equals("s0")) return new HashMap<String, TreeMap<Integer, Integer>>();
		Response hacResponse;
		try {
			hacResponse = Jsoup.connect(HAC_ATTENDANCE_URL)
				.cookie(".AuthCookie", authCookie)
				.cookie("ASP.NET_SessionId", sessionID)
				//.cookie("SPIHACSiteCode", siteCode)
				.timeout(10*1000)
				.ignoreContentType(true)
				.ignoreHttpErrors(true)
				.execute();
			
			Document hacDocument = hacResponse.parse();
			
			Elements monthLinkNodes = hacDocument.select(".sg-asp-calendar-header").get(0).select("[href]");
			String[] previousMonthParams = monthLinkNodes.get(0).attr("href").split("\'");
			
			String month = hacDocument.select("td").get(0).text().split(" ")[1];
			TreeMap<Integer, Integer> unexcused = new TreeMap<Integer, Integer>();
			TreeMap<Integer, Integer> tardies = new TreeMap<Integer, Integer>();
			do {
				for (Element i : hacDocument.select("td[title]")) {
					String[] absenceDateData = i.attr("title").split("[\n\r]");
					for (int data = 0; data < absenceDateData.length; data += 2) {
						int classPeriod;
						try {
							classPeriod = Integer.parseInt(absenceDateData[data].trim());
						} catch (NumberFormatException e) {
							continue;
						};
						String excuse = absenceDateData[data + 1].trim();
						
						if (classPeriod / 10 > 0) classPeriod = (int)Math.round((double)classPeriod / 10.0);
						
						if ("After Tardy, DMC Absences, Doctor\'s Note, Doctor\'s Return, Late Note, Left Message, No Contact, Parent Contact, Runaway Unexcused, Skipping, Student Contact, Unexcused, Multiple Attendance Codes".contains(excuse)) {
							int unexcusedSoFar;
							if (unexcused.containsKey(classPeriod)) unexcusedSoFar = unexcused.get(classPeriod);
							else unexcusedSoFar = 0;
							unexcused.put(classPeriod, unexcusedSoFar + 1);
						} else if ("Tardy".contains(excuse)) {
							int tardiesSoFar;
							if (tardies.containsKey(classPeriod)) tardiesSoFar = tardies.get(classPeriod);
							else tardiesSoFar = 0;
							tardies.put(classPeriod, tardiesSoFar + 1);
						}
					}
				}
				
				hacResponse = Jsoup.connect(HAC_ATTENDANCE_URL)
						.cookie(".AuthCookie", authCookie)
						.cookie("ASP.NET_SessionId", sessionID)
						//.cookie("SPIHACSiteCode", siteCode)
						.data("__VIEWSTATE", hacDocument.select("#__VIEWSTATE").attr("value"))
						.data("__EVENTVALIDATION", hacDocument.select("#__EVENTVALIDATION").attr("value"))
						.data("__EVENTTARGET", previousMonthParams[1])
						.data("__EVENTARGUMENT", previousMonthParams[3])
						.timeout(10*1000)
						.ignoreContentType(true)
						.ignoreHttpErrors(true)
						.execute();
				hacDocument = hacResponse.parse();
				monthLinkNodes = hacDocument.select(".sg-asp-calendar-header").get(0).select("[href]");
				previousMonthParams = monthLinkNodes.get(0).attr("href").split("\'");
				
				month = hacDocument.select("td").get(0).text().split(" ")[1];
				if (month.startsWith("20")) break;
			} while (true);
			
			HashMap<String, TreeMap<Integer, Integer>> absenceData = new HashMap<String, TreeMap<Integer, Integer>>();
			absenceData.put("unexcused", unexcused);
			absenceData.put("tardies", tardies);
			return absenceData;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			return new HashMap<String, TreeMap<Integer, Integer>>();
		}
	}
	
	public void fetchReportCard() throws Exception {
		Response hacResponse = Jsoup.connect(HAC_REPORTCARD_URL)
			.cookie(".AuthCookie", authCookie)
			.cookie("ASP.NET_SessionId", sessionID)
			//.cookie("SPIHACSiteCode", siteCode)
			.timeout(10*1000)
			.ignoreContentType(true)
			.ignoreHttpErrors(true)
			.execute();
		Document hacDocument = hacResponse.parse();
		
		ReportCard reportCard = new ReportCard();
		Elements classNodes = hacDocument.select(".sg-asp-table-data-row");
		for (Element classNode : classNodes) {
			Elements columnElements = classNode.select("td");
			
			String className = columnElements.get(1).text();
			
			String[] nineWeeksGrades = new String[4];
			String[] finalExamGrades = new String[2];
			String[] semesterGrades = new String[2];
			String absences = columnElements.get(20).text().trim();
			for (int nineWeeksIndex = 0; nineWeeksIndex < 4; nineWeeksIndex++)
				nineWeeksGrades[nineWeeksIndex] = columnElements.get(nineWeeksIndex + 7 + (nineWeeksIndex > 2 ? 2 : 0)).text().trim();
			for (int semesterIndex = 0; semesterIndex < 2; semesterIndex++) {
				finalExamGrades[semesterIndex] = columnElements.get(9 + (semesterIndex * 4)).text().trim();
				semesterGrades[semesterIndex] = columnElements.get(10 + (semesterIndex * 4)).text().trim();
			}
			
			HashMap<String, String> reportCardData = new HashMap<String, String>();
			reportCardData.put("firstNineWeeksGrade", nineWeeksGrades[0]);
			reportCardData.put("secondNineWeeksGrade", nineWeeksGrades[1]);
			reportCardData.put("finalExamGrade", finalExamGrades[0]);
			reportCardData.put("semesterGrade", semesterGrades[0] + ".0");
			reportCardData.put("finalExamGradeToKeep", "- -");
			reportCardData.put("finalExamGradeToNext", "- -");
			
			if (reportCardData.get("secondNineWeeksGrade").isEmpty()) {
				Class sameClass = null;
				for (Class course : this.currentUser.classes) {
					if (course.name.equals(className)) {
						sameClass = course;
						break;
					}
				}
				try {
					reportCardData.put("secondNineWeeksGrade", Math.round(Double.parseDouble(sameClass.grade)) + "");
				} catch (Exception e) {}
			}
			
			if (reportCardData.get("semesterGrade").equals(".0")) {
				int completedNineWeeks = 0;
				double semesterPoints = 0.0;
				try {
					semesterPoints += Double.parseDouble(reportCardData.get("firstNineWeeksGrade"));
					completedNineWeeks++;
					semesterPoints += Double.parseDouble(reportCardData.get("secondNineWeeksGrade"));
					completedNineWeeks++;
					semesterPoints += Double.parseDouble(reportCardData.get("finalExamGrade"));
				} catch (Exception e) {};
				if (semesterPoints != 0.0 && completedNineWeeks > 0) {
					double semesterGrade = (double)(semesterPoints / (double)completedNineWeeks);
					reportCardData.put("semesterGrade", String.format("%.1f", semesterGrade));
					
					double nextGrade = Math.floor(Math.round(semesterGrade) / 10.0) * 10.0 + 10.0 - 0.5;
					double keepGrade = Math.min(89.5, Math.floor(Math.round(semesterGrade) / 10.0) * 10.0 - 0.5);
					
					double gradeToNext = Math.ceil(nextGrade * 7.0 - semesterPoints * 3.0);
					double gradeToKeep = Math.ceil(keepGrade * 7.0 - semesterPoints * 3.0);
					
					if (gradeToNext > 110 || nextGrade > 89.5) reportCardData.put("finalExamGradeToKeep", String.format("%.0f", gradeToKeep));
					else reportCardData.put("finalExamGradeToNext", String.format("%.0f", gradeToNext));
				}
			}
			
			reportCard.classes.put(className, reportCardData);
		}
		this.currentUser.reportCard = reportCard;
	}
}

class User {
	String username;
	String password;
	String name;
	String school;
	String gradeLevel;
	
	ArrayList<Class> classes;
	
	ReportCard reportCard;
	
	public User() {
		this.username = "";
		this.password = "";
		this.name = "";
		this.school = "";
		this.gradeLevel = "";
		
		this.classes = new ArrayList<Class>();
	}
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.name = "";
		this.school = "";
		this.gradeLevel = "";
		
		this.classes = new ArrayList<Class>();
	}
}

class ReportCard {
	HashMap<String, HashMap<String, String>> classes;
	
	public ReportCard() {
		this.classes = new HashMap<String, HashMap<String, String>>();
	}
}

class Class {
	String name;
	Teacher teacher;
	String grade;
	ArrayList<Category> categories;
	ArrayList<Assignment> assignments;
	
	public Class(String name) {
		this.name = name;
		this.teacher = new Teacher();
		this.grade = "- -";
		this.categories = new ArrayList<Category>();
		this.assignments = new ArrayList<Assignment>();
	}
	
	public Class(String name, Teacher teacher) {
		this.name = name;
		this.teacher = teacher;
		this.grade = "- -";
		this.categories = new ArrayList<Category>();
		this.assignments = new ArrayList<Assignment>();
	}
	
	public Class(String name, Teacher teacher, String grade) {
		this.name = name;
		this.teacher = teacher;
		this.grade = grade;
		this.categories = new ArrayList<Category>();
		this.assignments = new ArrayList<Assignment>();
	}
	
	public boolean equals(Object other) {
		return other instanceof Class && name.equals(((Class)other).name) || other instanceof String && name.equals((String)other);
	}
	
	public int hashCode() {
	    return name.hashCode();
	}
}

class Teacher {
	String name;
	String email;
	
	public Teacher() {
		this.name = "None";
		this.email = "";
	}
	
	public Teacher(String name, String email) {
		this.name = name;
		this.email = email;
	}
}

class Assignment {
	String name;
	String type;
	String grade;
	
	HashMap<String, Object> extra;
	
	public Assignment(String name, String type, String grade, HashMap<String, Object> extra) {
		this.name = name;
		this.type = type;
		this.grade = grade;
		
		this.extra = extra;
	}
	
	public Assignment(String name, String type, Double grade, HashMap<String, Object> extra) {
		this.name = name;
		this.type = type;
		this.grade = String.format("%.2f", grade);
		
		this.extra = extra;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Assignment) return this.name.equals(((Assignment)other).name);
		return false;
	}
}

class Category {
	String name;
	String average;
	String weight;
	
	String gradeToKeep;
	String gradeToNext;
	
	public Category(String name, String average, String weight) {
		this.name = name;
		this.average = average;
		this.weight = weight;
	}
	
	public Category(String name, Double average, Double weight) {
		this.name = name;
		this.average = String.format("%.2f", average);
		this.weight = String.format("%d%%", Math.round(weight));
		
		if (average == -1.0) this.average = "- -";
	}
	
	public void calculateEstimations(Class course) {
		double nextGrade = Math.max(69.5, Math.floor(Math.round(Double.parseDouble(course.grade)) / 10.0) * 10.0 + 10.0 - 0.5);
		double keepGrade = Math.min(89.5, Math.floor(Math.round(Double.parseDouble(course.grade)) / 10.0) * 10.0 - 0.5);
		
		double totalWeight = 0.0;
		double categoryWeight = Double.parseDouble(this.weight.replaceAll("\\D", ""));
		double currentAverage = Double.parseDouble(course.grade);
		double categoryAverage = Double.parseDouble(this.average);
		if (currentAverage < 0.0) return;
		for (Category addCategory : course.categories) {
			double weight = Double.parseDouble(addCategory.weight.replaceAll("\\D", ""));
			totalWeight += weight;
		}
		categoryWeight /= (totalWeight /= 100.0) * 100.0;
		
		double categoryPoints = 0.0;
		double assignmentsInCategory = 0.0;
		for (Assignment assignment : course.assignments) {
			if (assignment.type.equals(this.name)) {
				try {
					categoryPoints += Double.parseDouble(assignment.grade);
					assignmentsInCategory += 1.0;
				} catch (Exception e) {};
			}
		}
		
		String gradeToKeep = String.format("%.0f", Math.max(0.0, Math.ceil(((assignmentsInCategory + 1.0) / categoryWeight * (keepGrade - (currentAverage - categoryWeight * (categoryPoints / assignmentsInCategory)))) - categoryPoints)));
		String gradeToNext = String.format("%.0f", Math.ceil(Math.ceil(((assignmentsInCategory + 1.0) / categoryWeight * (nextGrade - (currentAverage - categoryWeight * (categoryPoints / assignmentsInCategory)))) - categoryPoints)));
		if (Double.parseDouble(gradeToNext) > 110 || nextGrade > 89.5) this.gradeToKeep = gradeToKeep;
		else/* if (keepGrade >= 69.5)*/ this.gradeToNext = gradeToNext;
	}
}
