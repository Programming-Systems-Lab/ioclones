package newtest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/*
 * Test Reading from a File and Writing to a file
 */
public class BreakTest6 {
	
	static void readFromFile() {
		String fileName = "/Harsha/workspace/Java_J2EE/ioclones/clonedetector/src/test/java/newtest/file/fileforreading.txt";

		//read file into stream, try-with-resources
		try (Stream<String> stream = Files.lines(Paths.get(fileName))) {

			stream.forEach(System.out::println);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void writeToFile() throws IOException {
		String content = "Writing a line to file";

		File file = new File("/Harsha/workspace/Java_J2EE/ioclones/clonedetector/src/test/java/newtest/file/filetowrite.txt");

		// if file doesnt exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(content);
		bw.close();

		System.out.println("Done");
	}

	public static void main(String[] args) throws IOException {
		readFromFile();
		writeToFile();
	}

}
