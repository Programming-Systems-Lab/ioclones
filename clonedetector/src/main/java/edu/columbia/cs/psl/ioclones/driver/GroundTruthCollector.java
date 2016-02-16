package edu.columbia.cs.psl.ioclones.driver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Console;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class GroundTruthCollector {
	
	public static char connect = '@';
	
	public static final String urlHeader = "jdbc:mysql://";
	
	public static final String urlTail = "?useServerPrepStmts=false&rewriteBatchedStatements=true&autoReconnect=true";
	
	public static Set<String> collectTruth(File truthCsv) {
		Set<String> ret = new HashSet<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(truthCsv));
			//Header
			br.readLine();
			
			String buf = "";
			while ((buf = br.readLine()) != null) {
				String[] data = buf.split(",");
				String m1 = data[2];
				String m2 = data[4];
				String total = m1 + connect + m2;
				
				ret.add(total);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return ret;
	}
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Truth path: ");
		String path = scanner.nextLine();
		File truthFile = new File(path);
		if (!truthFile.exists()) {
			System.err.println("Invalid truth file: " + truthFile.getAbsolutePath());
			System.exit(-1);
		}
		Set<String> truth = collectTruth(truthFile);
		System.out.println("Ground truth: " + truth.size());
		
		System.out.println("DB path: ");
		String db = scanner.nextLine();
		db = urlHeader + db + urlTail;
		System.out.println("Confirm db path: " + db);
		
		System.out.println("Username: ");
		String userName = scanner.nextLine();
		
		Console console = System.console();
		char[] pwArray = console.readPassword("Password: ");
		String pw = new String(pwArray);
		
		System.out.println("Try DB connection");
		Connection attempt = IOUtils.getConnection(db, userName, pw);
		if (attempt == null) {
			System.err.println("Connection fails...");
			String retry = console.readLine("Fail to connect to database, try again?");
			System.exit(-1);
		} else {
			System.out.println("Connection succeeds");
		}
		
		System.out.println("Start entry: ");
		int start = Integer.valueOf(scanner.nextLine());
		System.out.println("Confirm start entry: " + start);
		
		System.out.println("End entry: ");
		int end = Integer.valueOf(scanner.nextLine());
		System.out.println("Confirm end entry: " + end);
		
		
		try {
			Connection conn = IOUtils.getConnection(db, userName, pw);
			
			String sql = "SELECT * FROM hitoshio_row WHERE comp_id>=? and comp_id<=? and sim>=0.85";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setInt(1, start);
			stmt.setInt(2, end);
			ResultSet rs = stmt.executeQuery();
			Set<String> collected = new HashSet<String>();
			while (rs.next()) {
				String m1 = rs.getString("method1");
				String m2 = rs.getString("method2");
				
				String check = m1 + connect + m2;
				if (truth.contains(check)) {
					collected.add(check);
				}
			}
			System.out.println("Collected truth: " + collected.size());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}	
	
	public static class MethodTuple {
		public String method1;
		
		public String method2;
		
		public MethodTuple(String method1, String method2) {
			this.method1 = method1;
			this.method2 = method2;
		}
		
		public int hashCode() {
			String total = method1 + method2;
			return total.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (!(obj instanceof MethodTuple)) {
				return false;
			}
			
			MethodTuple tmp = (MethodTuple) obj;
			if (!tmp.method1.equals(this.method1)) {
				return false;
			}
			
			if (!tmp.method2.equals(this.method2)) {
				return false;
			}
			
			return true;
		}
	}

}
