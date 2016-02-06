package edu.columbia.cs.psl.ioclones.utils;

import java.util.*;

import com.google.gson.reflect.TypeToken;
import com.thoughtworks.xstream.XStream;

import edu.columbia.cs.psl.ioclones.pojo.IORecord;

import java.math.*;
import java.io.*;

public class Test {
	
	class Point {
		int x, y;

		public Point(int x, int y) {
			super();
			this.x = x;
			this.y = y;
		}
	}
	
	int w, l, u, g;
	public Point[] lower, upper;

	private void solve() throws Exception {
		w = nextInt();
		l = nextInt();
		u = nextInt();
		g = nextInt();
		lower = new Point[l];
		for (int i = 0; i < l; ++i) {
			lower[i] = new Point(nextInt(), nextInt());
		}
		upper = new Point[u];
		for (int i = 0; i < u; ++i) {
			upper[i] = new Point(nextInt(), nextInt());
		}
		double totalArea = getArea(w);
		double oneArea = totalArea / g;
		double last = 0;
		out.println();
		for (int i = 0; i < g - 1; ++i) {
			double lo = last, hi = w;
			for (int it = 0; it < 200; ++it) {
				double mid = (lo + hi) / 2.;
				double curArea = getArea(mid) - getArea(last);
				if (curArea > oneArea)
					hi = mid;
				else
					lo = mid;
			}
			last = (hi + lo) / 2.;
			out.printf("%.10f\n", last);
		}
	}
	
	double getArea(double x) {
		return getArea(upper, x) - getArea(lower, x);
	}
	
	final double LOWER_Y = -2000;

	private double getArea(Point[] p, double x) {
		double res = 0;
		for (int i = 1; i < p.length; ++i) {
			if (p[i].x > x) {
				double lastY = p[i - 1].y, curY;
				double dx = p[i].x - p[i - 1].x;
				curY = lastY + (double)(p[i].y - p[i - 1].y) * (x - p[i - 1].x) / dx;
				double midY = (lastY + curY) / 2.;
				midY += LOWER_Y;
				res += midY * (x - p[i - 1].x);
				break;
			} else {
				double lastY = p[i - 1].y, curY = p[i].y;
				double midY = (lastY + curY) / 2.;
				midY += LOWER_Y;
				res += midY * (p[i].x - p[i - 1].x);
			}
		}
		return res;
	}

	public void run() {
		try {
			int tc = nextInt();
			for (int it = 1; it <= tc; ++it) {
				System.err.println(it);
				out.print("Case #" + it + ": ");
				solve();
			}
		} catch (Exception e) {
			NOO(e);
		} finally {
			out.close();
		}
	}

	PrintWriter out;
	BufferedReader in;
	StringTokenizer St;

	void NOO(Exception e) {
		e.printStackTrace();
		System.exit(42);
	}

	int nextInt() {
		return Integer.parseInt(nextToken());
	}

	long nextLong() {
		return Long.parseLong(nextToken());
	}

	double nextDouble() {
		return Double.parseDouble(nextToken());
	}

	String nextToken() {
		while (!St.hasMoreTokens()) {
			try {
				String line = in.readLine();
				if (line == null)
					return null;
				St = new StringTokenizer(line);
			} catch (Exception e) {
				NOO(e);
			}
		}
		return St.nextToken();
	}

	public Test(String name) {
		try {
			in = new BufferedReader(new FileReader(name + ".in"));
			St = new StringTokenizer("");
			out = new PrintWriter(new FileWriter(name + ".out"));
		} catch (Exception e) {
			NOO(e);
		}
	}

	public Test() {
		try {
			in = new BufferedReader(new InputStreamReader(System.in));
			St = new StringTokenizer("");
			out = new PrintWriter(System.out);
		} catch (Exception e) {
			NOO(e);
		}
	}

	public void testInput(int i, double j, long k, boolean b, Object o) {
		IORecord record = new IORecord("test123", false);
		record.preload(1, i);
		record.preload(2, j);
		record.preload(4, k);
		record.preload(6, b);
		record.preload(7, o);
	}
	
	public static void main(String[] args) {
		boolean[] bs = new boolean[2];
		bs[0] = true;
		byte[] bytes = new byte[2];
		bytes[0] = (byte)1;
		System.out.println("bs: " + bs[0]);
		System.out.println("bytes: " + bytes[0]);
		System.out.println(bytes[0] == 1);
		short[] shorts = new short[2];
		shorts[0] = 1;
		char[] chars = new char[2];
		chars[0] = 'c';
	}
}

