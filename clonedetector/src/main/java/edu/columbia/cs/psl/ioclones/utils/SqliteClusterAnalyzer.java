package edu.columbia.cs.psl.ioclones.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import edu.columbia.cs.psl.ioclones.driver.IODriver;
import edu.columbia.cs.psl.ioclones.utils.ClusterAnalyzer.Neighbor;

public class SqliteClusterAnalyzer {
	
	private final static String header = "method,real_label,1st,1st_sim,2nd,2nd_sim,3rd,3rd_sim,4th,4th_sim,5th,5th_sim,label_count,predict_label\n";
	
	public static Options options = new Options();
	
	static {
		options.addOption("db", true, "Database location");
		options.addOption("k", true, "Neightbor number");
		options.addOption("lines", true, "Line thresh");
		options.addOption("similarity", true, "Similarity threshold");
		options.addOption("filter", false, "Filter out read and next");
		options.addOption("break", false, "Break tie");
		options.addOption("constructors", true, "Constructor file");
		
		options.getOption("db").setRequired(true);
		options.getOption("k").setRequired(true);
		options.getOption("lines").setRequired(true);
		options.getOption("similarity").setRequired(true);
		options.getOption("constructors").setRequired(true);
	}
	
	public static void main(String[] args) {
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine command = parser.parse(options, args);
			
			String dbPath = command.getOptionValue("db");
			String constPath = command.getOptionValue("constructors");
			int simThresh = Integer.valueOf(command.getOptionValue("similarity"));
			int kNum = Integer.valueOf(command.getOptionValue("k"));
			int lineSize = Integer.valueOf(command.getOptionValue("lines"));
			boolean filter = command.hasOption("filter");
			boolean breakTie = command.hasOption("break");
			
			System.out.println("Database: " + dbPath);
			System.out.println("Constructors: " + constPath);
			System.out.println("Similarity: " + simThresh);
			System.out.println("k: " + kNum);
			System.out.println("Line size: " + lineSize);
			System.out.println("Filter next and read: " + filter);
			System.out.println("Break tie: " + breakTie);
			
			BufferedReader br = new BufferedReader(new FileReader(constPath));
			Set<Integer> consts = new HashSet<Integer>();
			String data = null;
			while ((data = br.readLine()) != null) {
				String[] splits = data.split(",");
				int constId = Integer.valueOf(splits[0]);
				consts.add(constId);
			}
			System.out.println("Const size: " + consts.size());
			
			Class.forName("org.sqlite.JDBC");
			String dbpath = "jdbc:sqlite:" + dbPath + ".db";
			Connection connect = DriverManager.getConnection(dbpath);
			
			Map<Integer, MethodMeta> allMethods = new HashMap<Integer, MethodMeta>();
			String methodQuery = "SELECT * FROM functions";
			PreparedStatement methodStatement = connect.prepareStatement(methodQuery);
			ResultSet methodResult = methodStatement.executeQuery();
			while (methodResult.next()) {
				int id = methodResult.getInt(1);
				String file = methodResult.getString(2);
				String methodName = methodResult.getString(3).trim();
				int startLine = methodResult.getInt(4);
				int endLine = methodResult.getInt(5);
				int lines = endLine - startLine + 1;
				
				MethodMeta methodInfo = new MethodMeta();
				methodInfo.methodId = id;
				methodInfo.file = file;
				methodInfo.label = file.split("/")[6];
				methodInfo.methodName = methodName;
				methodInfo.startLine = startLine;
				methodInfo.endLine = endLine;
				methodInfo.lines = lines;
				
				if (allMethods.containsKey(id)) {
					//weird
					System.err.println("Duplicated method: " + id + " " + file + " " + methodName);
					System.exit(-1);
				} else {
					allMethods.put(id, methodInfo);
				}
			}
			System.out.println("# of methods: " + allMethods.size());
			
			//Filter out constructors
			
			StringBuilder result = new StringBuilder();
			result.append(header);
			for (int id: allMethods.keySet()) {
				StringBuilder row = new StringBuilder();
				
				MethodMeta methodInfo = allMethods.get(id);
				String method = methodInfo.toString();
				//System.out.println("Me: " + method);
				//Dirty, but this is the only way...
				String myLabel = methodInfo.label;
				//System.out.println("My label: " + myLabel);
				String myMethod = methodInfo.methodName;
				//System.out.println("My method name: " + methodInfo.methodName);
				
				//Constructors
				if (consts.contains(methodInfo.methodId)) {
					continue ;
				}
				
				//Filter out readXX, or nextXX, which are some little utility functions
				if (filter) {
					if (myMethod.startsWith("read") || myMethod.startsWith("next")) {
						//System.out.println("Filter out utility method: " + myMethod + "\n");
						continue ;
					}
				}
				
				row.append(method + "," + myLabel + ",");
				
				String knnQuery = "SELECT rt.* FROM FinalTable rt " +
									"INNER JOIN (SELECT ID1, ID2, MAX(Threshold) as max_sim " +
										"FROM FinalTable " +
										"WHERE Threshold >= ? and (ID1 = ? or ID2 = ?) " +    
										"GROUP BY ID1, ID2) max_rec " +
										"ON rt.ID1 = max_rec.ID1 and rt.ID2 = max_rec.ID2 and rt.Threshold = max_rec.max_sim " +
									"ORDER BY Threshold desc;";
				PreparedStatement knnStatement = connect.prepareStatement(knnQuery);
				knnStatement.setInt(1, simThresh);
				knnStatement.setInt(2, methodInfo.methodId);
				knnStatement.setInt(3, methodInfo.methodId);
				
				ResultSet knnResult = knnStatement.executeQuery();
				
				int count = 0;
				double lastSimilarity = 0;
				HashMap<String, List<Neighbor>> neighborRecord = new HashMap<String, List<Neighbor>>();
				HashSet<Integer> neighborCache = new HashSet<Integer>();
				while (knnResult.next()) {
					int knnSub = knnResult.getInt("ID1");
					int knnTarget = knnResult.getInt("ID2");
					
					MethodMeta method1 = allMethods.get(knnSub);
					MethodMeta method2 = allMethods.get(knnTarget);
					
					int lineSub = method1.lines;
					int lineTarget = method2.lines;
					double avg = ((double)(lineSub + lineTarget))/2;
					int avgLine = (int)Math.round(avg);
					//System.out.println("m1 v2 m2: " + method1 + " " + method2 + " " + avgLine);
					if (avgLine < lineSize) {
						//System.out.println("Under-sized clones");
						//System.out.println("method1: " + method1 + " " + lineSub);
						//System.out.println("method2: " + method2 + " " + lineTarget);
						continue ;
					}
					
					int similarity = knnResult.getInt("Threshold");
					
					if (count >= kNum) {
						if (breakTie) {
							break ;
						} else {
							if (similarity < lastSimilarity) {
								break ;
							}
						}
					}
					
					MethodMeta neighbor = null;
					boolean checkSub = true;
					if (knnSub == methodInfo.methodId) {
						neighbor = method2;
						checkSub = false;
					} else {
						neighbor = method1;
					}
					
					String neighborLabel = neighbor.label;
					//System.out.println("Neightbor label: " + neighborLabel);
					String neighborMethod = neighbor.methodName;
					
					/*if (neighborName.equals(myName))
						continue ;*/
					
					if (consts.contains(neighbor.methodId)) {
						continue ;
					}
					
					if (filter) {
						if (neighborMethod.startsWith("read") || neighborMethod.startsWith("next")) {
							//System.out.println("Filter neighbor utility method: " + neighbor);
							continue ;
						}
					}
										
					if (neighborCache.contains(neighbor.methodId)) {
						continue ;
					}
					neighborCache.add(neighbor.methodId);
					
					//Record the best
					if (count < 5)
						row.append(neighbor + "," + similarity + ",");
					
					Neighbor newNeighbor = new Neighbor();
					newNeighbor.methodName = neighbor.toString();
					newNeighbor.username = neighbor.toString();
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
					//System.out.println("Query no result\n");
					continue ;
				}
				
				int remained = 5- count;
				for (int i = 0; i < remained; i++) {
					row.append(" , ,");
				}
				
				//System.out.println("Check neighbor count: ");
				List<String> bestLabels = new ArrayList<String>();
				int bestLabelCount = Integer.MIN_VALUE;
				String countString = "";
				for (String label: neighborRecord.keySet()) {
					int labelCount = neighborRecord.get(label).size();
					//System.out.println("Label: " + label + " " + labelCount);
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
				//System.out.println("Count string: " + countString);
				row.append(countString + ",");
				//System.out.println("Check best labels: " + bestLabels);
				
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
				//System.out.println("Best label: " + bestLabel + "\n");
				row.append(bestLabel + "\n");
				result.append(row);
			}
			
			File resultDir = new File("./competitor");
			if (!resultDir.exists()) {
				resultDir.mkdir();
			}
			
			String simString = String.valueOf(simThresh);
			String filterString = (filter==true?"f":"u");
			String fileName = resultDir.getAbsolutePath() + "/knn_result_" + kNum + "_" + lineSize + "_" + simString + "_" + filterString + ".csv";
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			bw.write(result.toString());
			bw.close();
			System.out.println("Result path: " + fileName);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static class MethodMeta {
		public int methodId;
		
		public String file;
		
		public String label;
		
		public String methodName;
		
		public int startLine;
		
		public int endLine;
		
		public int lines;
		
		@Override
		public String toString() {
			return label + "-" + methodId + "-" + methodName + "-" + startLine + "-" + endLine;
		}
	}

}
