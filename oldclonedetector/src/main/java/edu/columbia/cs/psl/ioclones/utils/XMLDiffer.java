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

import edu.columbia.cs.psl.ioclones.sim.SimAnalyzer;

public class XMLDiffer {
	
	private static final Logger logger = LogManager.getLogger(XMLDiffer.class);
	
	private static final DifferenceEvaluator valEvaluator = new DifferenceEvaluator() {
		@Override
		public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
			/*System.out.println("Check: " + outcome + " " + comparison.getType());
			System.out.println("Outcome: " + outcome);
			System.out.println("Comp. type: " + comparison.getType());
			System.out.println("Control: " + comparison.getControlDetails().getTarget());
			System.out.println("Test: " + comparison.getTestDetails().getTarget());*/
			if (outcome == ComparisonResult.DIFFERENT) {
				switch(comparison.getType()) {
					case TEXT_VALUE:
						Detail control = comparison.getControlDetails();
						Detail test = comparison.getTestDetails();
						//System.out.println(control.getTarget().getNodeName());
						//System.out.println(control.getValue());
						
						if (control != null && test != null) {
							Object controlVal = control.getValue();
							Object testVal = test.getValue();
							if (Number.class.isAssignableFrom(controlVal.getClass()) 
									&& Number.class.isAssignableFrom(testVal.getClass())) {
								Number controlNum = (Number) controlVal;
								Number testNum = (Number) testVal;
								
								if (Math.abs(controlNum.doubleValue() - testNum.doubleValue()) < SimAnalyzer.TOLERANCE) {
									outcome = ComparisonResult.EQUAL;
								}
							}
						}
						
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
	
	/*public static Diff xmlDiff(String xml1, String xml2) {
		DifferenceEvaluator chainedEvaluator = DifferenceEvaluators.chain(DifferenceEvaluators.Default, valEvaluator);
		//.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
		Diff d = DiffBuilder.compare(Input.fromString(xml1))
				.withTest(Input.fromString(xml2))
				.ignoreWhitespace()
				.withDifferenceEvaluator(valEvaluator)
				.build();
		
		return d;
	}*/
	
	public static Diff xmlDiff(Object obj1, Object obj2) {
		long before = Runtime.getRuntime().freeMemory();
		//DifferenceEvaluator chainedEvaluator = DifferenceEvaluators.chain(DifferenceEvaluators.Default, valEvaluator);
		Diff d = DiffBuilder.compare(Input.from(obj1))
				.withTest(Input.from(obj2))
				.ignoreWhitespace()
				.withDifferenceEvaluator(valEvaluator)
				.build();
		/*d.getDifferences().forEach(diff->{
			System.out.println(diff.getComparison());
			System.out.println(diff.getResult());
		});*/
		long end = Runtime.getRuntime().freeMemory();
		double diff = ((double)(end - before))/Math.pow(10, 6);
		if (diff > 1000) {
			logger.info("Suspicious xml diff: " + obj1 + " " + obj2);
		}
		
		return d;
	}
	
	public static Diff xmlDiff(String s1, String s2) {
		//DifferenceEvaluator chainedEvaluator = DifferenceEvaluators.chain(DifferenceEvaluators.Default, valEvaluator);
		Diff d = DiffBuilder.compare(Input.fromString(s1))
				.withTest(Input.fromString(s2))
				.ignoreWhitespace()
				.withDifferenceEvaluator(valEvaluator)
				.build();
		/*d.getDifferences().forEach(diff->{
			System.out.println(diff.getComparison());
			System.out.println(diff.getResult());
		});*/
		
		return d;
	}
	
	public static void xmlDiff(File f1, File f2) {
		Diff d = DiffBuilder.compare(Input.fromFile(f1).build()).withTest(Input.fromFile(f2)).build();
		d.getDifferences().forEach(diff->{
			//System.out.println(diff);
			System.out.println(diff.getComparison());
			System.out.println(diff.getResult());
		});
	}
}
