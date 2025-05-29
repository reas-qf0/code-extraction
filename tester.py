import os
from sys import argv

from extractor import Extractor, ExtractionResult

if len(argv) < 3:
    print('usage: tester.py (test name) [auto|(range1) (range2) [(range3) ...]] (type) [type_params]')

name = argv[1]
if argv[2] == 'auto':
    if not os.system('python3 main.py tests/%s.java' % name):
        print(f'Error in test {name}: detection/extraction failed')
        exit(1)
    i = 3
else:
    ranges = []
    i = 2
    while '-' in argv[i]:
        ranges.append(tuple(map(int, argv[i].split('-'))))
        i += 1
    extractor = Extractor('tests/%s.java' % name)
    if not all(map(lambda x: x[1].code == ExtractionResult.SUCCESS, extractor.extract(*ranges))):
        print(f'Error in test {name}: detection/extraction failed')
        exit(1)
    extractor.output_to_file('output.java')

test_type = argv[i]
test_args = argv[i+1:]
if test_type == 'compiles' or test_type == 'runs':
    os.system('javac output.java')
    os.unlink('output.java')
    try:
        open('output.class')
    except FileNotFoundError:
        print('error - compile failed')
        exit(1)
    if test_type == 'runs':
        code = os.system(f'java output {' '.join(test_args)}')
        os.unlink('output.class')
        if code != 0:
            print(f'error - execution finished with error code {code}')
            exit(1)