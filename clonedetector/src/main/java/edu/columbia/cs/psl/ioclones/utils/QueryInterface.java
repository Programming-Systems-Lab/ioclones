package edu.columbia.cs.psl.ioclones.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.cs.psl.ioclones.driver.IODriver;

public class QueryInterface {
	
	private static final Logger logger = LogManager.getLogger(QueryInterface.class);
	
	public static void basicInfoQuery(Connection conn, 
			int classId, 
			String mName) {
		try {
			String methodQuery = "SELECT * FROM METHODINFO WHERE C_ID = ? and METHOD_DESC LIKE ?";
			PreparedStatement methodPs = conn.prepareStatement(methodQuery);
			methodPs.setInt(1, classId);
			methodPs.setString(2, mName + "%");
			ResultSet methodRS = methodPs.executeQuery();
			
			TypeToken<Map<Integer, TreeSet<Integer>>> mapToken = 
					new TypeToken<Map<Integer, TreeSet<Integer>>>(){};
			boolean getResult = false;
			while (methodRS.next()) {
				getResult = true;
				String methodName = methodRS.getString("METHOD_DESC");
				Map<Integer, TreeSet<Integer>> writtenParams = 
						IOUtils.jsonToObj(methodRS.getString("WRITTEN_PARAMS"), mapToken);
				System.out.println("Method name: " + methodName);
				System.out.println("Written params: " + writtenParams);
			}
			
			if (!getResult) {
				//System.out.println("No method result for: " + classId + " " + mName);
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
	
	public static void treeTraverse(Connection conn, 
			String rootClass, 
			String methodName) {
		try {
			System.out.println("Root class: " + rootClass);
			System.out.println("Target method: " + methodName);
			
			LinkedList<String> queue = new LinkedList<String>();
			Set<String> visited = new HashSet<String>();
			queue.add(rootClass);
			while (queue.size() > 0) {
				String className = queue.removeFirst();
				System.out.println("Current class: " + className);
				
				visited.add(className);
				
				String query = "SELECT * FROM CLASSINFO WHERE CLASSNAME=?";
				PreparedStatement ps = conn.prepareStatement(query);
				ps.setString(1, className);
				
				ResultSet rs = ps.executeQuery();
				TypeToken<List<String>> listToken = new TypeToken<List<String>>(){};
				if (rs.next()) {
					int id = rs.getInt("ID");
					String name = rs.getString("CLASSNAME");
					String parent = rs.getString("PARENT");
					List<String> interfaces = IOUtils.jsonToObj(rs.getString("INTERFACES"), listToken);
					List<String> children = IOUtils.jsonToObj(rs.getString("CHILDREN"), listToken);
					
					children.forEach(c->{
						if (!visited.contains(c) && !queue.contains(c)) {
							queue.add(c);
						}
					});
					
					basicInfoQuery(conn, id, methodName);
				}
			}
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
	}
	
	public static void main(String[] args) {
		try {
			String className = "java.io.ObjectInputStream";
			String methodName = "readObject-()";
			
			Class.forName("org.sqlite.JDBC");
			String dbpath = "jdbc:sqlite:" + IODriver.profileDir + "/methodeps.db";
			Connection conn = DriverManager.getConnection(dbpath);
			logger.info("Connect to sqlite");
			
			treeTraverse(conn, className, methodName);
			conn.close();
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		
	}

}
