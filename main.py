from javalang.tree import *
from collections import namedtuple
from node_operations import simultaneous_walk, type_to_string, literal_to_type
from source_file import SourceFile
from variable_manager import VariableManager, VarClass
from sys import argv

if len(argv) >= 6:
    src = argv[1]
    line_start1 = int(argv[2])
    line_end1 = int(argv[3])
    line_start2 = int(argv[4])
    line_end2 = int(argv[5])
else:
    src = input("source file: ")
    line_start1, line_end1 = map(int, input("1st block (startline-endline): ").split('-'))
    line_start2, line_end2 = map(int, input("2nd block (startline-endline): ").split('-'))

with open(src) as file:
    lines = file.readlines()
    lines1 = lines[line_start1-1:line_end1]
    lines2 = lines[line_start2-1:line_end2]
file = SourceFile(src)
block1 = file.narrow_down(line_start1, line_end1)
block2 = file.narrow_down(line_start2, line_end2)
if isinstance(block1, (MethodDeclaration, ConstructorDeclaration)):
    processing_method = True
    method_type = type_to_string(block1.return_type)
    block1 = block1.body
    block2 = block2.body
else:
    processing_method = False
print(block1)

fields = file.get_field_declarations(block1)
vars1 = VariableManager(fields, file.get_outscope_declarations(block1))
vars2 = VariableManager(fields, file.get_outscope_declarations(block2))

Parameter = namedtuple('Parameter', ['name', 'type', 'value1', 'value2'])
ReturnValue = namedtuple('ReturnValue', ['name', 'type', 'assign1', 'assign2'])
Replacement = namedtuple('Replacement', ['position', 'len', 'new_s'])

params = []
replacements = []
returns = []
counter = 1

const_params = {}
var_params = {}
ret_params = {}

def process_vars(node1, node2, path1, path2):
    global counter
    if not node1.qualifier:
        name1 = node1.member
        name2 = node2.member
    else:
        name1 = node1.qualifier.split('.')[0]
        name2 = node2.qualifier.split('.')[0]
    c1, t1 = vars1.get_type(name1, path1)
    c2, t2 = vars2.get_type(name2, path2)
    # inscopes don't have to be passed in but they have to match in the ast position and also only come with inscopes
    # outscopes have to always be passed since they otherwise won't be visible
    # fields have to be passed only if different fields are used
    if c1 == VarClass.INSCOPE or c2 == VarClass.INSCOPE:
        if c1 != VarClass.INSCOPE or c2 != VarClass.INSCOPE:
            raise ValueError("inscope matching with non-inscope")
        if t1 != t2: raise ValueError("different inscope variables in the same place")
    elif c1 == VarClass.OUTSCOPE or c2 == VarClass.OUTSCOPE or \
            (c1 == VarClass.FIELD and c2 == VarClass.FIELD and name1 != name2):
        if t1.name != t2.name or t1.dimensions != t2.dimensions: raise ValueError("variables of different types in the same place")
        key = (name1, name2)
        if key not in var_params:
            params.append(Parameter("extracted%s" % counter, type_to_string(t1), name1, name2))
            counter += 1
            var_params[key] = params[-1]
        p = var_params[key]
        replacements.append(Replacement(node1.position, len(name1), p.name))

for path1, path2, node1, node2 in simultaneous_walk(block1, block2):
    if type(node1) != type(node2):
        raise ValueError('ast types differ: not going here -', type(node1).__name__, '!=', type(node2).__name__)

    # if var declaration, add type
    if isinstance(node1, VariableDeclaration):
        if len(node1.declarators) != len(node2.declarators):
            raise ValueError('different amount of var declarators')
        for decl1, decl2 in zip(node1.declarators, node2.declarators):
            vars1.add_inscope(decl1.name, path1, node1.type)
            vars2.add_inscope(decl2.name, path2, node2.type)

    # if parameter of an in-scope method declaration, add type
    if isinstance(node1, MethodDeclaration):
        if len(node1.parameters) != len(node2.parameters):
            raise ValueError('different amount of method parameters')
        for param1, param2 in zip(node1.parameters, node2.parameters):
            vars1.add_inscope(param1.name, path1, param1.type)
            vars2.add_inscope(param2.name, path2, param2.type)

    # hardcoded constant difference
    if type(node1) == Literal and node1.value != node2.value:
        key = (node1.value, node2.value)
        if key not in const_params:
            params.append(Parameter("extracted%s" % counter, literal_to_type(node1.value), node1.value, node2.value))
            counter += 1
            const_params[key] = params[-1]
        p = const_params[key]
        replacements.append(Replacement(node1.position, len(node1.value), p.name))

    # reference to a variable
    if isinstance(node1, (MemberReference, MethodInvocation)):
        if isinstance(path1[-1], (MemberReference, MethodInvocation)) and path1[-1].qualifier is node1: continue
        if isinstance(path1[-1], Assignment) and path1[-1].expressionl is node1: continue
        process_vars(node1, node2, path1, path2)

    # assignment
    if type(node1) == Assignment:
        process_vars(node1.expressionl, node2.expressionl, path1 + (node1,), path2 + (node2,))
        name1 = node1.expressionl.member if not node1.expressionl.qualifier else node1.expressionl.qualifier
        name2 = node2.expressionl.member if not node2.expressionl.qualifier else node2.expressionl.qualifier
        c1, t1 = vars1.get_type(name1, path1)
        c2, t2 = vars2.get_type(name2, path2)
        print(name1, c1, t1, '\t\t', name2, c2, t2)
        if c1 == VarClass.INSCOPE or c2 == VarClass.INSCOPE:
            # inscopes have to always be returned
            if c1 != VarClass.INSCOPE or c2 != VarClass.INSCOPE:
                raise ValueError("inscope matching with non-inscope")
            if t1 != t2: raise ValueError("different inscope variables in the same place")
            returns.append(ReturnValue(name1, t1, name1, name1))
        elif type(t1) == ReferenceType or len(t1.dimensions) > 0:
            # no need to process by-reference vars
            pass
        elif c1 == VarClass.FIELD and c2 == VarClass.FIELD and name1 == name2:
            # no need to process assignments to the same field
            pass
        else:
            if (name1, name2) in var_params:
                name = var_params[(name1, name2)].name
            else:
                name = name1
            if (name1, name2) not in ret_params:
                returns.append(ReturnValue(name, type_to_string(t1), name1, name2))
                ret_params[(name1, name2)] = returns[-1]

print('--- Parameters ---')
for param in params:
    print(param)
print('\n--- Replacements ---')
for replacement in replacements:
    print(replacement)
print('\n--- Return values ---')
for return_ in returns:
    print(return_)


# apply replacements
replacements.sort()
replacements = replacements[::-1]
for pos, l, new in replacements:
    line = pos[0] - line_start1
    i = pos[1] - 1
    lines1[line] = lines1[line][:i] + new + lines1[line][i+l:]

# output to file
left_line1s, left_line1e, left_line2s, left_line2e = '', '', '', ''
if processing_method:
    while '{' not in lines1[0]:
        line_start1 += 1
        lines1 = lines1[1:]
    i = lines1[0].index('{')
    left_line1s = lines1[0][:i+1]
    lines1[0] = lines1[0][i+1:]

    while '}' not in lines1[-1]:
        line_end1 -= 1
        lines1 = lines1[:-1]
    i = lines1[-1].index('}')
    left_line1e = lines1[-1][i:]
    lines1[-1] = lines1[-1][:i]

    while '{' not in lines2[0]:
        line_start2 += 1
        lines2 = lines2[1:]
    i = lines2[0].index('{')
    left_line2s = lines2[0][:i+1]
    lines2[0] = lines2[0][i+1:]

    while '}' not in lines2[-1]:
        line_end2 -= 1
        lines2 = lines2[:-1]
    i = lines2[-1].index('}')
    left_line2e = lines2[-1][i:]
    lines2[-1] = lines2[-1][:i]

suitable_place = min(file.get_method_start_line(block1), file.get_method_start_line(block2))
with open('output.java','w') as file:
    for line in lines[:suitable_place-1]:
        file.write(line)

    file.write("\tprivate ")
    if processing_method:
        file.write(method_type)
    elif len(returns) > 0:
        file.write(returns[0].type)
    else:
        file.write("void")
    file.write(" extracted(")
    file.write(", ".join(map(lambda x: x.type + " " + x.name, params)))
    file.write(") {\n")

    file.write(''.join(lines1))
    if len(returns) > 0:
        file.write('return %s;\n\t' % returns[0].name)
    file.write('}\n\n')
    for line in lines[suitable_place-1:line_start1-1]:
        file.write(line)
    file.write(left_line1s)

    file.write("\t\t\t")
    if len(returns) > 0:
        file.write(returns[0].assign1)
        file.write(" = ")
    if processing_method and method_type != "void":
        file.write("return ")
    file.write("extracted(")
    file.write(", ".join(map(lambda x: x.value1, params)))
    file.write(");\n")

    file.write(left_line1e)
    for line in lines[line_end1:line_start2-1]:
        file.write(line)
    file.write(left_line2s)

    file.write("\t\t\t")
    if len(returns) > 0:
        file.write(returns[0].assign2)
        file.write(" = ")
    if processing_method and method_type != "void":
        file.write("return ")
    file.write("extracted(")
    file.write(", ".join(map(lambda x: x.value2, params)))
    file.write(");\n")

    file.write(left_line2e)
    for line in lines[line_end2:]:
        file.write(line)