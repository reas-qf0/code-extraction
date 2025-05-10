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

os.chdir(os.path.abspath(os.path.dirname(__file__)))
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
groups = []
for b1, b2 in params:
    for i in range(len(groups)):
        if b1 in groups[i] or b2 in groups[i]:
            groups[i].add(b1)
            groups[i].add(b2)
            break
    else:
        groups.append({b1, b2})
print('--- CCStokener found %s clones, divided into %s groups ---' % (len(params), len(groups)))

extractor = Extractor(fname, silent=True)
for i, group in enumerate(groups):
    print('Extracting group %s: ' % (i + 1),
          ' '.join(map(lambda x: '(%s-%s)' % x, group)),
          '...',end='')
    try:
        extractor.extract(*group)
        print('success')
    except Exception as e:
        print('failure')
        print('\tmessage:', e)
print('--- Saving result to: output.java ---')
extractor.output_to_file('output.java')