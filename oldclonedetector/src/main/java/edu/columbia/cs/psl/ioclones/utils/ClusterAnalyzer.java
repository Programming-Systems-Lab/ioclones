package edu.columbia.cs.psl.ioclones.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class ClusterAnalyzer {
	
	private final static String header = "method,real_label,1st,1st_sim,2nd,2nd_sim,3rd,3rd_sim,4th,4th_sim,5th,5th_sim,label_count,predict_label\n";
	
	public static final String urlHeader = "jdbc:mysql://";
	
	public static final String urlTail = "?useServerPrepStmts=false&rewriteBatchedStatements=true&autoReconnect=true";
	
	public static Options options = new Options();
	
	static {
		options.addOption("start", true, "Comp id (start)");
		options.addOption("end", true, "Comp id (end)");
		options.addOption("k", true, "Neightbor number");
		options.addOption("lines", true, "Line thresh");
		options.addOption("similarity", true, "Similarity threshold");
		options.addOption("filter", false, "Filter out read and next");
		options.addOption("break", false, "Break tie");
		options.addOption("username", true, "DB username");
		options.addOption("url", true, "DB url");
		options.addOption("lineinfo", true, "line info.");
		
		options.getOption("start").setRequired(true);
		options.getOption("end").setRequired(true);
		options.getOption("k").setRequired(true);
		options.getOption("lines").setRequired(true);
		options.getOption("similarity").setRequired(true);
		options.getOption("username").setRequired(true);
		options.getOption("url").setRequired(true);
		options.getOption("lineinfo").setRequired(true);
	}
	
	public static void main(String[] args) {
		Console console = System.console();
		try {
			if (console == null) {
				System.err.println("Null consoel!");
				System.exit(-1);
			}
						
			System.out.println("Password: ");
			char[] passArray = console.readPassword();
			final String password = new String(passArray);
			
			CommandLineParser parser = new DefaultParser();
			CommandLine commands = parser.parse(options, args);
			
			int compIdStart = Integer.valueOf(commands.getOptionValue("start"));
			int compIdEnd = Integer.valueOf(commands.getOptionValue("end"));
			int kNum = Integer.valueOf(commands.getOptionValue("k"));
			int lineSize = Integer.valueOf(commands.getOptionValue("lines"));
			double simThresh = Double.valueOf(commands.getOptionValue("similarity"));
			boolean filter = commands.hasOption("filter");
			boolean breakTie = commands.hasOption("break");
						
			String username = commands.getOptionValue("username");
			String dburl = commands.getOptionValue("url");
			dburl = urlHeader + dburl + urlTail;
			String lineinfoPath = commands.getOptionValue("lineinfo");
			File lineinfo = new File(lineinfoPath);
			if (!lineinfo.exists()) {
				System.out.println("Invalid line info path");
				System.exit(-1);
			}
			
			Map<String, Integer> lineMap = new HashMap<String, Integer>();
			BufferedReader br = new BufferedReader(new FileReader(lineinfo));
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
			
			System.out.println("Confirm query settings:");
			System.out.println("DB url: " + dburl);
			System.out.println("DB username: " + username);
			System.out.println("Line info: " + lineinfo.getAbsolutePath());
			System.out.println("Comp id (start): " + compIdStart);
			System.out.println("Comp id (end): " + compIdEnd); 
			System.out.println("Line size: " + lineSize);
			System.out.println("Similarity threshold: " + simThresh);
			System.out.println("Filter next and read: " + filter);
			System.out.println("Break tie: " + breakTie);
			
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(dburl, username, password);
			
			Set<String> allMethods = new HashSet<String>();
			String subQuery = "SELECT distinct method1 FROM hitoshio_row WHERE comp_id between " + compIdStart + " and " + compIdEnd;
			PreparedStatement subStatement = connect.prepareStatement(subQuery);
			ResultSet subResult = subStatement.executeQuery();
			int subCount = 0;
			while (subResult.next()) {
				String subName = subResult.getString("method1");
				allMethods.add(subName);
				subCount++;
			}
			System.out.println("# of method1: " + subCount);
			
			String targetQuery = "SELECT distinct method2 FROM hitoshio_row WHERE comp_id between " + compIdStart + " and " + compIdEnd;
			PreparedStatement targetStatement = connect.prepareStatement(targetQuery);
			ResultSet targetResult = targetStatement.executeQuery();
			int targetCount = 0;
			while (targetResult.next()) {
				String targetName = targetResult.getString("method2");
				allMethods.add(targetName);
				targetCount++;
			}
			System.out.println("# of method2: " + targetCount);
			System.out.println("# of total methods: " + allMethods.size());
			
			StringBuilder result = new StringBuilder();
			result.append(header);
			int correct = 0;
			int total = 0;
			for (String method: allMethods) {
				StringBuilder row = new StringBuilder();
				//String method = "R5P1Y14.darnley.A:solve:():Ljava.lang.String";
				
				System.out.println("Me: " + method);
				String[] myInfo = method.split("\\.");
				String myLabel = myInfo[0];
				String myName = myInfo[1];
				String myMethod = method.split("-")[1];
				
				//Filter out readXX, or nextXX, which are some little utility functions
				if (filter) {
					if (myMethod.startsWith("read") || myMethod.startsWith("next")) {
						System.out.println("Filter out utility method: " + method + "\n");
						continue ;
					}
				}
				
				row.append(method + "," + myLabel + ",");
				
				String knnQuery = "SELECT rt.* FROM hitoshio_row rt " +
									"INNER JOIN (SELECT method1, method2, MAX(sim) as max_sim " +
										"FROM hitoshio_row " +
										"WHERE (comp_id between ? and ?) and sim >= ? and (method1 = ? or method2 = ?) " +    
										"GROUP BY method1, method2) max_rec " +
										"ON rt.method1 = max_rec.method1 and rt.method2 = max_rec.method2 and rt.sim = max_rec.max_sim " +
									"WHERE (comp_id between ? and ?) ORDER BY sim desc;";
				PreparedStatement knnStatement = connect.prepareStatement(knnQuery);
				knnStatement.setInt(1, compIdStart);
				knnStatement.setInt(2, compIdEnd);
				knnStatement.setDouble(3, simThresh);
				knnStatement.setString(4, method);
				knnStatement.setString(5, method);
				knnStatement.setInt(6, compIdStart);
				knnStatement.setInt(7, compIdEnd);
				
				ResultSet knnResult = knnStatement.executeQuery();
				
				int count = 0;
				
				double lastSimilarity = 0;
				HashMap<String, List<Neighbor>> neighborRecord = new HashMap<String, List<Neighbor>>();
				HashSet<String> neighborCache = new HashSet<String>();
				while (knnResult.next()) {					
					String knnSub = knnResult.getString("method1");
					String knnTarget = knnResult.getString("method2");
					
					int lineSub = lineMap.get(knnSub);
					int lineTarget = lineMap.get(knnTarget);
					double avg = ((double)(lineSub + lineTarget))/2;
					int avgLine = (int)Math.round(avg);
					if (avgLine < lineSize) {
						System.out.println("Under-sized clones");
						System.out.println("method1: " + knnSub + " " + lineSub);
						System.out.println("method2: " + knnTarget + " " + lineTarget);
						continue ;
					}
					
					double similarity = knnResult.getDouble("sim");
					
					if (count >= kNum) {
						if (breakTie) {
							break ;
						} else {
							if (similarity < lastSimilarity) {
								break ;
							}
						}
					}
					
					String neighbor = "";
					String trace = "";
					boolean checkSub = true;
					if (knnSub.equals(method)) {
						neighbor = knnTarget;
						checkSub = false;
					} else {
						neighbor = knnSub;
					}
					
					//Skip the the same username from different years?
					String[] neighborInfo = neighbor.split("\\.");
					String neighborLabel = neighborInfo[0];
					String neighborName = neighborInfo[1];
					String neighborMethod = neighbor.split("-")[1];
					
					/*if (neighborName.equals(myName))
						continue ;*/
					
					if (filter) {
						if (neighborMethod.startsWith("read") || neighborMethod.startsWith("next")) {
							System.out.println("Filter neighbor utility method: " + neighbor);
							continue ;
						}
					}
										
					if (neighborCache.contains(neighbor)) {
						continue ;
					}
					neighborCache.add(neighbor);
					
					//Record the best
					if (count < 5)
						row.append(neighbor + "," + similarity + ",");
					
					Neighbor newNeighbor = new Neighbor();
					newNeighbor.methodName = neighbor;
					newNeighbor.username = neighborName;
					newNeighbor.label = neighborLabel;
					newNeighbor.similarity = similarity;
										
					if (!neighborRecord.containsKey(neighborLabel)) {
						List<Neighbor> neighborList = new ArrayList<Neighbor>();
						neighborList.add(newNeighbor);
						neighborRecord.put(neighborLabel, neighborList);
					} else {
						neighborRecord.get(neighborLabel).add(newNeighbor);
					}
					
					count++;
					lastSimilarity = similarity;
				}
				
				if (neighborRecord.size() == 0) {
					System.out.println("Query no result\n");
					continue ;
				}
				
				int remained = 5- count;
				for (int i = 0; i < remained; i++) {
					row.append(" , ,");
				}
				
				System.out.println("Check neighbor count: ");
				List<String> bestLabels = new ArrayList<String>();
				int bestLabelCount = Integer.MIN_VALUE;
				String countString = "";
				for (String label: neighborRecord.keySet()) {
					int labelCount = neighborRecord.get(label).size();
					System.out.println("Label: " + label + " " + labelCount);
					String labelSummary = label + ":" + labelCount + "-";
					countString += labelSummary;
					
					if (labelCount >= bestLabelCount) {
						if (labelCount > bestLabelCount) {
							bestLabelCount = labelCount;
							bestLabels.clear();
							bestLabels.add(label);
						} else {
							bestLabels.add(label);
						}
					}
				}
				countString = countString.substring(0, countString.length() - 1);
				System.out.println("Count string: " + countString);
				row.append(countString + ",");
				System.out.println("Check best labels: " + bestLabels);
				
				String bestLabel = "";
				if (bestLabels.size() == 1) {
					bestLabel = bestLabels.get(0);
				} else {
					double bestSum = 0;
					for (String label: bestLabels) {
						double curSum = 0;
						List<Neighbor> neighbors = neighborRecord.get(label);
						for (Neighbor n: neighbors) {
							curSum += n.similarity;
						}
						
						if (curSum > bestSum) {
							bestSum = curSum;
							bestLabel = label;
						}	
					}
				}
				System.out.println("Best label: " + bestLabel + "\n");
				row.append(bestLabel + "\n");
				result.append(row);
				
				if (myLabel.equals(bestLabel)) {
					correct++;
				}
				total++;
			}
			
			File resultDir = new File("./results");
			if (!resultDir.exists()) {
				resultDir.mkdir();
			}
			
			String simString = String.valueOf(simThresh).split("\\.")[1];
			String filterString = (filter==true?"f":"u");
			String fileName = resultDir.getAbsolutePath() + "/knn_result_" + kNum + "_" + lineSize + "_" + simString + "_" + filterString + ".csv";
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			bw.write(result.toString());
			bw.close();
			System.out.println("Result path: " + fileName);
			double precision = ((double)correct)/total;
			System.out.println("Correct total precision: " + correct + " " + total + " " + precision);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	public static class Neighbor {
		String methodName;
		
		String username;
		
		String label;
				
		double similarity;
	}

}
