package edu.columbia.cs.psl.ioclones.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class JointAnalyzer {
	
	public static Options options = new Options();
	
	static {
		options.addOption("c", true, "clone file");
		options.getOption("c").setRequired(true);
		options.addOption("l", true, "line info");
		options.getOption("l").setRequired(true);
	}
	
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		String cloneFile = cmd.getOptionValue("c");
		if (cloneFile == null) {
			System.err.println("Invalid clone file");
			System.exit(-1);
		}
		
		String lineInfo = cmd.getOptionValue("l");
		if (lineInfo == null) {
			System.err.println("Invalid line info");
			System.exit(-1);
		}
		
		Map<String, Integer> lineMap = new HashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(lineInfo));
		String buf = "";
		
		//header
		System.out.println("Line Header: " + br.readLine());
		while ((buf = br.readLine()) != null) {
			String[] info = buf.split(",");
			String methodKey = info[0];
			Integer lines = Integer.valueOf(info[1]);
			lineMap.put(methodKey, lines);
		}
		System.out.println("Check line info: " + lineMap.size());
		/*lineMap.forEach((m, l)->{
			System.out.println(m + " " + l);
		});*/
		
		BufferedReader cloneReader = new BufferedReader(new FileReader(cloneFile));
		System.out.println("Clone header: " + cloneReader.readLine());
		String cbuf = "";
		Map<String, Integer> mInC = new TreeMap<String, Integer>();
		int cloneCounter = 0;
		
		HashMap<TreeSet<String>, int[]> clonesCrossYears = new HashMap<TreeSet<String>, int[]>();
		while ((cbuf = cloneReader.readLine()) != null) {
			String[] info = cbuf.split(",");
			String m1 = info[2];
			m1 = m1.replace("\"", "");
			if (!lineMap.containsKey(m1)) {
				System.err.println("Cannot find method: " + m1);
				System.exit(-1);
			}
			int m1Line = lineMap.get(m1);
			
			String m2 = info[4];
			m2 = m2.replace("\"", "");
			if (!lineMap.containsKey(m2)) {
				System.err.println("Cannot find method: " + m2);
				System.exit(-1);
			}
			int m2Line = lineMap.get(m2);
			
			mInC.put(m1, m1Line);
			mInC.put(m2, m2Line);
			
			//System.out.println("M1: " + m1 + " " + m1Line);
			//System.out.println("M2: " + m2 + " " + m2Line);
			
			String m1Class = m1.split("-")[0];
			String m1Pkg = m1Class.split("\\.")[0];
			
			String m2Class = m2.split("-")[0];
			String m2Pkg = m2Class.split("\\.")[0];
			
			TreeSet<String> combinedKey = new TreeSet<String>();
			combinedKey.add(m1Pkg);
			combinedKey.add(m2Pkg);
			if (!clonesCrossYears.containsKey(combinedKey)) {
				int[] dist = new int[2];
				clonesCrossYears.put(combinedKey, dist);
			}
			
			//int check = Math.max(m1Line, m2Line);
			int[]dist = clonesCrossYears.get(combinedKey);
			double d = ((double)(m1Line + m2Line))/2;
			int check = (int)Math.round(d);
			/*if (check < 3) {
				dist[0]++;
			} else if (check >=3 && check < 8) {
				dist[1]++;
			} else {
				dist[2]++;
			}*/
			if (check <= 5) {
				System.out.println("M1 M2: " + m1 + " " + m2 + " " + m1Line + " " + m2Line);
				dist[0]++;
			} else {
				dist[1]++;
			}
			
			cloneCounter++;
		}
		
		System.out.println("Total clones: " + cloneCounter);
		System.out.println("Check mInC: " + mInC.size());
		int totalLines = 0;
		for (String m: mInC.keySet()) {
			int line = mInC.get(m);
			totalLines += line;
			System.out.println("Method: " + m);
			System.out.println("Lines: " + line);
		}
		double avgLine = ((double)totalLines)/mInC.size();
		System.out.println("Total lines: " + totalLines);
		System.out.println("Avg. lines: "+ avgLine);
		
		System.out.println("Clones per years: ");
		for (TreeSet<String> yearKey: clonesCrossYears.keySet()) {
			System.out.println("Year key: " + yearKey);
			System.out.println("# Clones: " + Arrays.toString(clonesCrossYears.get(yearKey)));
		}
	}

}
