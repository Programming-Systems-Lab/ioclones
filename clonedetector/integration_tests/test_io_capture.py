#!/usr/env/bin python3

import datetime
import os
import sys
from xml.etree import ElementTree as ET

"""
   Simple script to test IO capturing features of HitoshiIO while
   JUnit tests aren't set up. 
   1. Add your test method to MultTester.java
   2. Add the expected inputs/outputs here
   3. Increment NUM_MULT_TESTS
   4. Run either standalone or through run_integration_tests.py
"""

# modify these three constants as we add more and more test methods
NUM_MULT_TESTS = 24
CORRECT_INPUTS = [
	[('int', '3'),('int', '4')],               #1
	[('int', '2'),('int', '3'),('int', '4')],  #2
	[('int', '3'),('int', '4')],               #3
	[('int', '2'), ('int', '3'),('int', '4')], #4
	[('int', '4')],                            #5
	[('int', '3')],                            #6
	[('int', '3')],                            #7
	[('int', '3'),('int', '4')],               #8
	[('int', '3')],                            #9
	[('int', '3')],                            #10
	[('int', '4')],                            #11
	[('int', '4')],                            #12
	[('int', '3'),('int', '4')],               #13
	[('int', '3'),('int', '4')],               #14
	[('int', '3')],                            #15
	[('int', '3')],                            #16
	[('int', '8')],                            #17
	[('int', '8'),('int', '4'),('int', '5')],  #18
	[('int', '4'),('int', '3'),('int','8')],   #19
	[('int', '3'),('int', '8')],               #20
    [('int', '3'),('int', '8')],               #21
    [],                                        #22 should have no inputs since y is overloaded at beginning
    [('int', '3')],                            #23
    [('int', '3')],                            #24
    [('int', '3'),('list', '\n      ')],       #25 astore is broken?
]
CORRECT_OUTPUTS = [
	[('int', '12')],               #1
	[('int', '2')],                #2
	[('int', '0')],                #3    
	[('int', '24')],               #4
	[('int', '1')],                #5
	[('int', '0')],                #6
	[('int', '1')],                #7
	[('int', '12')],               #8
	[('int', '1')],                #9
	[('int', '3')],                #10
	[('int', '3')],                #11
	[('int', '4')],                #12
	[('int', '4'), ('int', '8')],  #13
	[('int', '12')],               #14
	[('int', '3')],                #15
	[('int', '3')],                #16
	[('int', '0')],                #17
	[('int', '-5')],               #18
	[('int', '1')],                #19
    [('int', '0')],                #20
    [('int', '0')],                #21
    [('int', '4')],                #22
    [('int', '4')],                #23
    [('int', '4')],                #24
    [('int', '4'),('list', '\n      ')],                #25 need to figure out what this means
]


HITOSHIIO_PREANALYZER_COMMAND = ('java -cp target/CloneDetector-0.0.1-SNAPSHOT.'
                                 'jar edu.columbia.cs.psl.ioclones.analysis.Pre'
                                 'Analyzer -cb integration_tests/d_multtester')
HITOSHIIO_INSTRUMENTATION_COMMAND = ('java -javaagent:target/CloneDetector-0.0.'
                                     '1-SNAPSHOT.jar -noverify -cp "target/Clon'
                                     'eDetector-0.0.1-SNAPSHOT.jar:integration_'
                                     'tests/d_multtester" edu.columbia.cs.psl.i'
                                     'oclones.driver.IODriver hitoshiio_test.Mu'
                                     'ltTester')


if __name__ == '__main__':
    # compile and run hitoshiIO steps 1 + 2
    print('Compiling MultTester...')
    os.system('javac integration_tests/d_multtester/hitoshiio_test/MultTester.java')
    print('Running HitoshiIO PreAnalyzer...')
    os.system(HITOSHIIO_PREANALYZER_COMMAND)
    print('Running Instrumentation...')
    os.system(HITOSHIIO_INSTRUMENTATION_COMMAND)

    print('Unzipping captured I/Os...')
    # unzip into temporary directory
    if 'MultTester_tmp_dir' not in os.listdir():
        os.system('mkdir MultTester_tmp_dir')

    os.system(('unzip iorepo/hitoshiio_test.MultTester.zip -d MultTester_tmp_dir >/dev/null 2>'
               '&1'))

    if len(CORRECT_INPUTS) < NUM_MULT_TESTS:
        print('Only wrote in %s sets of inputs, need %s' % \
              (len(CORRECT_INPUTS), NUM_MULT_TESTS))

    if len(CORRECT_OUTPUTS) < NUM_MULT_TESTS:
        print('Only wrote in %s sets of outputs, need %s' % \
              (len(CORRECT_OUTPUTS), NUM_MULT_TESTS))

    failed_input_methods = []
    failed_output_methods = []

    print('Validating input/output capture...')
    for i in range(NUM_MULT_TESTS):
        # check each xml for correctly captured inputs/outputs
        tree = ET.parse('MultTester_tmp_dir/hitoshiio_test/hitoshiio_test.MultTester-mult%s-%s.xml' % \
                (i + 1, i + 2))
        root = tree.getroot()

        method_inputs = []
        method_outputs = []

        for sorted_inputs in root.findall('.//sortedInputs'):
            for method_input in sorted_inputs:
                method_inputs.append((method_input.tag, method_input.text))

        for sorted_outputs in root.findall('.//sortedOutputs'):
            for method_output in sorted_outputs:
                method_outputs.append((method_output.tag,
                                       method_output.text))

        if len(CORRECT_INPUTS[i]) != len(method_inputs) or \
           set(CORRECT_INPUTS[i]) != set(method_inputs):
            failed_input_methods.append([i, method_inputs])

        if len(CORRECT_OUTPUTS[i]) != len(method_outputs) or \
           set(CORRECT_OUTPUTS[i]) != set(method_outputs):
            failed_output_methods.append([i, method_outputs])

    print()
    if len(failed_input_methods) + len(failed_output_methods) == 0:
        print('Test cases succesfully passed')
        rc = 0
    else:
        for i, method_inputs in failed_input_methods:
            print(('Method mult%s failed with\n\tinputs\t\t%s;\n\texpect'
                   'ed\t%s') % \
                              (i + 1, method_inputs, CORRECT_INPUTS[i]))
        print()
        for i, method_outputs in failed_output_methods:
            print(('Method mult%s failed with\n\toutputs\t\t%s;\n\texpec'
                   'ted\t%s') % \
                              (i + 1, method_outputs, CORRECT_OUTPUTS[i]))
        rc = -1

    # cleanup
    os.system('rm integration_tests/d_multtester/hitoshiio_test/MultTester.class')
    os.system('rm -r MultTester_tmp_dir')
    os.system('rm iorepo/hitoshiio_test.MultTester.zip')

    print('Cleanup complete. Exiting now with rc=%s.' % rc)
    sys.exit(rc)
