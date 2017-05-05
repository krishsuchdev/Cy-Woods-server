package com.suchdev.CyWoodsServer;

public class AthleticGame implements Comparable<AthleticGame> {
	public Integer gameNumber;
	public String date;
	public String time;
	
	public String opponent;
	public String location;
	public boolean home;
	
	public String score;
	public String result;
	
	public AthleticGame(Integer gameNumber, String date, String time, String opponent, String location, boolean home, String score) {
		this.gameNumber = gameNumber;
		this.date = date;
		this.time = time;
		this.opponent = opponent;
		this.location = location;
		this.home = home;
		this.score = score;
		
		String[] scoreSplit = this.score.split(" - ");
		int different = Integer.parseInt(scoreSplit[0]) - Integer.parseInt(scoreSplit[1]);
		this.result = different > 0 ? "Win" : different < 0 ? "Lose" : Integer.parseInt(scoreSplit[0]) == 0 ? "Not Started" : "Tie";
		
		if (home) this.score = scoreSplit[1] + " - " + scoreSplit[0];
	}

	@Override
	public int compareTo(AthleticGame o) {
		return this.gameNumber.compareTo(o.gameNumber);
	}
	
	@Override
	public String toString() {
		return (home ? "H" : "A") + " @ " + opponent;
	}
}
