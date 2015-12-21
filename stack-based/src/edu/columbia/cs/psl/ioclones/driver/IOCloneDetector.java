package edu.columbia.cs.psl.ioclones.driver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.cs.psl.ioclones.analysis.ISimilarity;
import edu.columbia.cs.psl.ioclones.analysis.SimpleHasher;
import edu.columbia.cs.psl.ioclones.pojo.IORecord;
import edu.columbia.cs.psl.ioclones.utils.IOUtils;

public class IOCloneDetector {
	
	private static final Logger logger = LogManager.getLogger(IOCloneDetector.class);
	
	public static void main(String[] args) {
		String basePath = "iorepo";
		File baseDir = new File(basePath);
		List<IORecord> records = new ArrayList<IORecord>();
		IOUtils.collectIORecords(baseDir, records);
		
		logger.info("Total IO records: " + records.size());
		ISimilarity calculator = new SimpleHasher();
		//Can be multi-thread here
		for (int i = 0; i < records.size(); i++) {
			IORecord io1 = records.get(i);
			for (int j = i + 1; j < records.size(); j++) {
				IORecord io2 = records.get(j);
				
				if (io1.getMethodKey().equals(io2.getMethodKey())) {
					continue ;
				}
				
				double sim = calculator.computeTotalSim(io1, io2);
				logger.info(io1.getMethodKey() + " vs " + io2.getMethodKey() + ": " + sim);
			}
		}
	}

}
