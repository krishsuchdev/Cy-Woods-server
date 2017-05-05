package com.suchdev.CyWoodsServer;

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FacultyFetcher {
	// URLS
		public static final String FACULTY_WEBPAGES_URL = "https://app.cfisd.net/urlcap/campus_list_011.html";
		
		// News Items
		private ArrayList<FacultyMember> faculty;
		
		public FacultyFetcher() {
			faculty = new ArrayList<FacultyMember>();
		}
		public ArrayList<FacultyMember> getFaculty() {
			return faculty;
		}
		public void mergeFacultyMember(FacultyMember facultyMember) {
			if (faculty.contains(facultyMember)) {
				FacultyMember oldFacultyMember = faculty.get(faculty.indexOf(facultyMember));
				oldFacultyMember.departments = facultyMember.departments;
				oldFacultyMember.subjects = facultyMember.subjects;
			} else {
				System.out.println("Old Teacher: " + facultyMember.name);
			}
		}
		public void fetchTeachers(Runnable function) throws Exception {
			faculty = new ArrayList<FacultyMember>();
			Document facultyDoc = Jsoup.connect(FACULTY_WEBPAGES_URL).get();
			Elements facultyNodes = facultyDoc.select("tr").get(1).select("a");
			FacultyMember currentFaculty = new FacultyMember();
			for (Element facultyNode : facultyNodes) {
				if (currentFaculty.name != null && facultyNode.text().trim().length() > 1) {
					this.faculty.add(currentFaculty);
					currentFaculty = new FacultyMember();
				}
				if (currentFaculty.name == null) {
					currentFaculty.name = facultyNode.text();
					if (facultyNode.select("a").attr("href").trim().length() == 0) currentFaculty.website = "";
				}
				else if (currentFaculty.website == null) currentFaculty.website = facultyNode.select("a").attr("href");
				else if (currentFaculty.email == null) currentFaculty.email = facultyNode.select("a").attr("href");
			}
			function.run();
		}
}
