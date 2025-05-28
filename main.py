import os, tempfile, shutil, csv
from sys import argv
from extractor import Extractor

t = 0.9

if len(argv) > 1:
    fname = ' '.join(argv[1:])
else:
    fname = input('Enter file path: ')
fname = os.path.abspath(fname)
print('Processing file:', fname)

extractor_path = os.path.abspath(os.path.dirname(__file__))
os.chdir(extractor_path)
venv_python = os.path.abspath('.venv/bin/python3')
with tempfile.TemporaryDirectory() as tmpdir:
    shutil.copy(fname, os.path.join(tmpdir, os.path.basename(fname)))
    print('Running CCStokener...')
    os.chdir('../CCStokener/ccstokener')
    if os.system('%s runner.py -i "%s" -m common -t %s -l java >/dev/null' % (venv_python, tmpdir, t)):
        print('CCStokener exited with error, exiting.')
        exit(1)


with open('./results/clonepairs.txt') as file:
    reader = csv.reader(file)
    params = [((int(x1), int(x2)), (int(x3), int(x4))) for _, x1, x2, _, x3, x4 in reader]
params.sort()
groups = []
for b1, b2 in params:
    for i in range(len(groups)):
        if b1 in groups[i] or b2 in groups[i]:
            groups[i].add(b1)
            groups[i].add(b2)
            break
    else:
        groups.append({b1, b2})
print('--- CCStokener found %s clones, divided into %s groups ---' % (sum([len(x) for x in groups]), len(groups)))

def group_to_string(group):
    return ' '.join(map(lambda x: '(%s-%s)' % x, group))

extractor = Extractor(fname, silent=True)
for i, group in enumerate(groups):
    print('Extracting group %s: ' % (i + 1), group_to_string(group), '...',end='')
    result = extractor.extract(*group)
    if len(result) == 1:
        print(result[0][1].description())
    else:
        print('further subdivided into %s subgroups:' % len(result))
        for subgroup, subgroup_result in result:
            print(f'\t{group_to_string(subgroup)} - {subgroup_result.description()}')

output_file = os.path.join(extractor_path, 'output.java')
print('--- Saving result to: %s ---' % output_file)
extractor.output_to_file(output_file)