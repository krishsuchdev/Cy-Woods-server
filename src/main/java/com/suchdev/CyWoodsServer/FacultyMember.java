package com.suchdev.CyWoodsServer;

import java.util.ArrayList;

public class FacultyMember {
	String name;
	String email;
	String website;
	
	ArrayList<String> departments;
	ArrayList<String> subjects;
	
	public FacultyMember() {
		this.departments = new ArrayList<String>();
		this.subjects = new ArrayList<String>();
	}
	
	public FacultyMember(String name) {
		this.name = name;
		
		this.departments = new ArrayList<String>();
		this.subjects = new ArrayList<String>();
	}
	
	public FacultyMember(String name, String email, String website) {
		this.name = name;
		this.email = email;
		this.website = website;
		
		this.departments = new ArrayList<String>();
		this.subjects = new ArrayList<String>();
	}
	
	public FacultyMember(String name, String email, String website, ArrayList<String> departments, ArrayList<String> subjects) {
		this.name = name;
		this.email = email;
		this.website = website;
		
		this.departments = departments;
		this.subjects = subjects;
	}
	
	public String getId() {
		String[] splitName = name.split(", ");
		splitName[0] = splitName[0].replace("_", " ");
		splitName[1] = splitName[1].replace("_", " ");
		return (splitName[0] + splitName[1] + (splitName[0].length() + splitName[1].length())).replaceAll("[^A-z0-9]", "").toLowerCase();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof FacultyMember) return this.getId().equals(((FacultyMember) other).getId());
 		return false;
	}
	
	@Override
	public String toString() {
		String r = name + "\n" + email + "\n" + website + "\n";
		if (departments.isEmpty()) r += "Unknown\nUnknown\n";
		else for (int i = 0; i < departments.size(); i++) r += departments.get(i) + "\n" + subjects.get(i) + "\n";
		return r;
	}
}
