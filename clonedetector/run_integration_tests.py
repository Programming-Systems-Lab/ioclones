import os
import sys

if __name__ == '__main__':
    print('Beginning the running of integration test suite\n')
    rcs = []

    for test_file in os.listdir('integration_tests'):
        if not test_file.endswith('.py'):
            continue
        print('Running %s\n' % test_file)
        rc = os.system('python3 integration_tests/%s' % test_file) # travis ci linux
        print('\n%s completed with rc %s' % (test_file, rc))
        if rc != 0:
            rcs.append(rc)

    if rcs:
        sys.exit(-1)
