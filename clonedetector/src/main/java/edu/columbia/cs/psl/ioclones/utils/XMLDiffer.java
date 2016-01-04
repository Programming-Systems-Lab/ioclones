package edu.columbia.cs.psl.ioclones.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.Comparison.Detail;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

public class XMLDiffer {
	
	private static final Logger logger = LogManager.getLogger(XMLDiffer.class);
	
	private static final Class[] parameters = new Class[]{URL.class};
	
	private static final DifferenceEvaluator valEvaluator = new DifferenceEvaluator() {
		@Override
		public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
			System.out.println("Check: " + outcome + " " + comparison.getType());
			if (outcome == ComparisonResult.DIFFERENT) {
				switch(comparison.getType()) {
					case TEXT_VALUE:
						Detail d = comparison.getControlDetails();
						System.out.println(d.getTarget().getNodeName());
						System.out.println(d.getValue());
						break ;	
					case NODE_TYPE:
					case HAS_DOCTYPE_DECLARATION:
	                case DOCTYPE_SYSTEM_ID:
	                case SCHEMA_LOCATION:
	                case NO_NAMESPACE_SCHEMA_LOCATION:
	                case NAMESPACE_PREFIX:
	                case ATTR_VALUE_EXPLICITLY_SPECIFIED:
	                case CHILD_NODELIST_SEQUENCE:
	                case XML_ENCODING:
	                    outcome = ComparisonResult.SIMILAR;
	                    break;
				} 
			}
			return outcome;
		}
	};
	
	public static void xmlDiff(String xml1, String xml2) {
		DifferenceEvaluator chainedEvaluator = DifferenceEvaluators.chain(DifferenceEvaluators.Default, valEvaluator);
		//.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
		Diff d = DiffBuilder.compare(Input.fromString(xml1))
				.withTest(Input.fromString(xml2))
				.withDifferenceEvaluator(valEvaluator)
				.ignoreWhitespace()
				.build();
		d.getDifferences().forEach(diff->{
			//System.out.println(diff);
			System.out.println(diff.getComparison());
			System.out.println(diff.getResult());
		});
	}
	
	public static void xmlDiff(File f1, File f2) {
		Diff d = DiffBuilder.compare(Input.fromFile(f1).build()).withTest(Input.fromFile(f2)).build();
		d.getDifferences().forEach(diff->{
			//System.out.println(diff);
			System.out.println(diff.getComparison());
			System.out.println(diff.getResult());
		});
	}
	
	public static void main(String[] args) throws Exception {
		File codebaseFile = new File("/Users/mikefhsu/Desktop/code_repos/io_play/bin");
		if (!codebaseFile.exists()) {
			logger.error("Invalid codebase: " + codebaseFile.getAbsolutePath());
			System.exit(-1);
		}
		URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
		Class sysClass = URLClassLoader.class;
		Method method = sysClass.getDeclaredMethod("addURL", parameters);
		method.setAccessible(true);
		method.invoke(sysloader, new Object[]{codebaseFile.toURI().toURL()});
		
		File f1 = new File("/Users/mikefhsu/Desktop/code_repos/R5P1Y11.aditsu/R5P1Y11.aditsu.Cakes-get-389.xml");
		File f2 = new File("/Users/mikefhsu/Desktop/code_repos/R5P1Y11.aditsu/R5P1Y11.aditsu.Cakes-get-390.xml");
		xmlDiff(f1, f2);
		
		Object obj1 = IOUtils.fromXML2Obj(f1);
		Object obj2 = IOUtils.fromXML2Obj(f2);
		
		String xml1 = IOUtils.fromObj2XML(obj1);
		String xml2 = IOUtils.fromObj2XML(obj2);
		
		xmlDiff(xml1, xml2);
	}

}
