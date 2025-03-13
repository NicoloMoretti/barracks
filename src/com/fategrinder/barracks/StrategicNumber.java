package com.fategrinder.barracks;

public class StrategicNumber {
	public String snName;   //interesting
	public int id;
	public String category;
	public int min;	
	public int max;
	public int rmin;	 //interesting
	public int rmax;	 //interesting
	public int network;
	public int defined;
	public String version;
	public int aoe;
	public int aoc;
	public int up;
	public int de;		 //interesting
	public String shortDescription;
	public String description;
	
	public int defaultValue;
	public int effective;
	public int available; // do not use
	public int[] linked;// do not use
	public int[] related;// do not use

	StrategicNumber(String snName) {
		this.snName = snName;
		this.id = -1;
		this.category = "";
		this.min = -1;
		this.max = -1;
		this.rmin = -1;
		this.rmax = -1;
		this.network = 0;
		this.defined = 0;
		this.version = "1.0c";
		this.aoe = 0;
		this.aoc = 0;
		this.up = 0;
		this.de = 0;
		this.shortDescription = "";
		this.description = "";
	}
	
	String getLink() {
		return snName;
	}
}