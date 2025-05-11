from javalang.tree import *
from collections import namedtuple
from node_operations import simultaneous_walk, type_to_string, literal_to_type
from source_file import SourceFile
from variable_manager import VariableManager, VarClass
from sys import argv

Replacement = namedtuple('Replacement', ['position', 'len', 'new_s'])
LineReplacement = namedtuple('LineReplacement', ['start', 'end', 'new_lines'])
Parameter = namedtuple('Parameter', ['name', 'type', 'values'])
ReturnValue = namedtuple('ReturnValue', ['name', 'type', 'assigns'])

class Extractor:
    def __init__(self, src, silent=False):
        self.src = src
        with open(src) as file:
            self.lines = file.readlines()
        self.file = SourceFile(src)
        self.replacements = []
        self.line_replacements = []
        self.methods = []
        self.counter = 1
        self.silent = silent
    def print(self, *args, **kwargs):
        if not self.silent:
            print(*args, **kwargs)
    def replace(self, position, len, new_s):
        self.replacements.append(Replacement(position, len, new_s))
    def replace_lines(self, start, end, new_lines):
        self.line_replacements.append(LineReplacement(tuple(start), tuple(end), new_lines))


    def extract(self, *lines):
        if type(lines) == list:
            lines = lines[0]
        if len(lines) < 2:
            raise ValueError("provide at least 2 pairs of lines")

        blocks = list(map(lambda x: self.file.narrow_down(x[0], x[1]), lines))
        n = len(blocks)
        if isinstance(blocks[0], (MethodDeclaration, ConstructorDeclaration)):
            processing_method = True
            method_type = type_to_string(blocks[0].return_type)
            blocks = list(map(lambda x: x.body, blocks))
        else:
            processing_method = False

        fields = self.file.get_field_declarations(blocks[0])
        vars = list(map(lambda x: VariableManager(fields, self.file.get_outscope_declarations(x)), blocks))
        params = []
        returns = []
        counter = 1
        const_params = {}
        var_params = {}
        ret_params = {}

        def process_vars(nodes, paths):
            nonlocal counter
            names = tuple(map(lambda x: x.qualifier.split('.')[0] if x.qualifier else x.member, nodes))
            res = [vars[i].get_type(names[i], paths[i]) for i in range(n)]
            cs, ts = map(list, zip(*res))
            # inscopes don't have to be passed in but they have to match in the ast position and also only come with inscopes
            # outscopes have to always be passed since they otherwise won't be visible
            # fields have to be passed only if different fields are used
            if VarClass.INSCOPE in cs:
                if filter(lambda x: x != VarClass.INSCOPE, cs):
                    raise ValueError("inscope matching with non-inscope")
                if len(set(ts)) > 1: raise ValueError("different inscope variables in the same place")
            elif VarClass.OUTSCOPE in cs or (all(filter(lambda x: x == VarClass.FIELD, cs)) and len(set(names)) > 1):
                if len(set(map(lambda x: x.name, ts))) > 1 or len(set(map(lambda x: tuple(x.dimensions), ts))) > 1:
                    raise ValueError("variables of different types in the same place")
                if names not in var_params:
                    params.append(Parameter("extracted%s" % counter, type_to_string(ts[0]), names))
                    counter += 1
                    var_params[names] = params[-1]
                p = var_params[names]
                self.replace(nodes[0].position, len(names[0]), p.name)

        for paths, nodes in simultaneous_walk(blocks):
            for i in range(1, n):
                if type(nodes[i]) != type(nodes[0]):
                    raise ValueError('ast types differ: not going here - %s' % list(map(lambda x: type(x).__name__, nodes)))

            # if var declaration, add type
            if isinstance(nodes[0], VariableDeclaration):
                decls = list(map(lambda x: x.declarators, nodes))
                if len(set(map(len, decls))) > 1:
                    raise ValueError('different amount of var declarators')
                for decl in zip(*decls):
                    for i in range(n):
                        vars[i].add_inscope(decl[i].name, paths[i], nodes[i].type)

            # if parameter of an in-scope method declaration, add type
            if isinstance(nodes[0], MethodDeclaration):
                params = list(map(lambda x: x.parameters, nodes))
                if len(set(map(len, params))) > 1:
                    raise ValueError('different amount of method parameters')
                for param in zip(*params):
                    for i in range(n):
                        vars[i].add_inscope(param[i].name, paths[i], param[i].type)

            # hardcoded constant difference
            if type(nodes[0]) == Literal:
                key = tuple(map(lambda x: x.value, nodes))
                if len(set(key)) == 1: continue
                if key not in const_params:
                    params.append(Parameter("extracted%s" % counter, literal_to_type(nodes[0].value), key))
                    counter += 1
                    const_params[key] = params[-1]
                p = const_params[key]
                self.replace(nodes[0].position, len(nodes[0].value), p.name)

            # reference to a variable
            if isinstance(nodes[0], (MemberReference, MethodInvocation)):
                if isinstance(paths[0][-1], (MemberReference, MethodInvocation)) and paths[0][-1].qualifier is nodes[0]: continue
                if isinstance(paths[0][-1], Assignment) and paths[0][-1].expressionl is nodes[0]: continue
                process_vars(nodes, paths)

            # assignment
            if type(nodes[0]) == Assignment:
                expressionls = list(map(lambda x: x.expressionl, nodes))
                new_paths = tuple([p + (x,) for p, x in zip(paths, nodes)])
                process_vars(expressionls, new_paths)

                names = tuple(map(lambda x: x.member if not x.qualifier else x.qualifier, expressionls))
                res = [vars[i].get_type(names[i], paths[i]) for i in range(n)]
                cs, ts = map(list, zip(*res))
                self.print(names, cs, ts)
                if VarClass.INSCOPE in cs:
                    # inscopes have to always be returned
                    if any(map(lambda x: x != VarClass.INSCOPE, cs)):
                        raise ValueError("inscope matching with non-inscope")
                    if len(set(ts)) > 1: raise ValueError("different inscope variables in the same place")
                    returns.append(ReturnValue(names[0], ts[0], [names[0] for _ in range(n)]))
                elif type(ts[0]) == ReferenceType or len(ts[0].dimensions) > 0:
                    # no need to process by-reference vars
                    pass
                elif all(map(lambda x: x != VarClass.FIELD, cs)) and len(set(names)) == 1:
                    # no need to process assignments to the same field
                    pass
                else:
                    if names in var_params:
                        name = var_params[names].name
                    else:
                        name = names[0]
                    if names not in ret_params:
                        returns.append(ReturnValue(name, type_to_string(ts[0]), names))
                        ret_params[names] = returns[-1]

        self.print('--- Parameters ---')
        for param in params:
            self.print(param)
        self.print('\n--- Return values ---')
        for return_ in returns:
            self.print(return_)

        starts = [[lines[i][0], 0] for i in range(n)]
        ends = [[lines[i][1], len(self.lines[lines[i][1] - 1])] for i in range(n)]
        if processing_method:
            for i in range(n):
                while '{' not in self.lines[starts[i][0] - 1]:
                    starts[i][0] -= 1
                starts[i][1] = self.lines[starts[i][0] - 1].index('{') + 2
                while '}' not in self.lines[ends[i][0] - 1]:
                    ends[i][0] += 1
                ends[i][1] = self.lines[ends[i][0] - 1].index('}')

        suitable_place = min(map(lambda x: self.file.get_method_start_line(x), blocks))
        self.replace_lines((suitable_place, 0), (suitable_place, 0), [
            "\tprivate ",
            method_type if processing_method else returns[0].type if len(returns) > 0 else 'void',
            " extracted_method%s(" % self.counter,
            ", ".join(map(lambda x: x.type + " " + x.name, params)),
            ") {\n",
            (starts[0], ends[0]),
            'return %s;\n\t' % returns[0].name if len(returns) > 0 else '',
            '}\n\n'
        ])

        for i in range(n):
            self.replace_lines(starts[i], ends[i], [
                returns[0].assigns[i] + ' = ' if len(returns) > 0 else '',
                "extracted_method%s(" % self.counter,
                ", ".join(map(lambda x: x.values[i], params)),
                ");\n"
            ])
        self.counter += 1


    def output_to_file(self, dst='output.java'):
        self.print('\n--- Replacements (type 1)')
        for repl1 in self.replacements:
            self.print(repl1)
        self.print('\n--- Replacements (type 2)')
        for repl2 in self.line_replacements:
            self.print(repl2)

        # apply replacements 1
        replacements = sorted(self.replacements)[::-1]
        for pos, l, new in replacements:
            line = pos[0] - 1
            i = pos[1] - 1
            self.lines[line] = self.lines[line][:i] + new + self.lines[line][i+l:]

        # apply replacements 2
        replacements = []
        for x in sorted(self.line_replacements)[::-1]:
            new_s = ''
            for y in x.new_lines:
                if type(y) == tuple:
                    new_s += self.lines[y[0][0]-1][y[0][1]-1:] + ''.join(self.lines[y[0][0]:y[1][0]-1]) + self.lines[y[1][0]-1][:y[1][1]-1]
                else:
                    new_s += y
            replacements.append(LineReplacement(x.start, x.end, new_s))
        for pos1, pos2, x in replacements:
            self.lines[pos1[0]-1:pos2[0]] = self.lines[pos1[0]-1][:pos1[1]-1] + x + self.lines[pos2[0]-1][pos2[1]:]

        with open(dst,'w') as file:
            file.write(''.join(self.lines))



if __name__ == "__main__":
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
    e = Extractor(src)
    e.extract((line_start1, line_end1), (line_start2, line_end2))
    e.output_to_file('output.java')