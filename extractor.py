from javalang.tokenizer import Separator
from javalang.tree import *
from collections import namedtuple
from node_operations import simultaneous_walk, type_to_string, literal_to_type, separate_references, types
from source_file import SourceFile
from variable_manager import VariableManager, VarClass
from sys import argv

Replacement = namedtuple('Replacement', ['position', 'len', 'new_s'])
LineReplacement = namedtuple('LineReplacement', ['start', 'end', 'new_lines'])
Parameter = namedtuple('Parameter', ['name', 'type', 'values'])
ReturnValue = namedtuple('ReturnValue', ['name', 'type', 'assigns'])

class ExtractionResult:
    CUSTOM_ERROR = -1
    SUCCESS = 0
    DIFF_PROPERTIES = 1
    INVALID_INPUT = 2

    @classmethod
    def success(cls):
        return ExtractionResult(ExtractionResult.SUCCESS, None)
    @classmethod
    def error(cls, info):
        return ExtractionResult(ExtractionResult.CUSTOM_ERROR, info)
    @classmethod
    def different_properties(cls, info):
        return ExtractionResult(ExtractionResult.DIFF_PROPERTIES, info)
    @classmethod
    def invalid_input(cls, info):
        return ExtractionResult(ExtractionResult.INVALID_INPUT, info)

    def __init__(self, code, info):
        self.code = code
        self.info = info
    def __bool__(self):
        return bool(self.code)
    def description(self):
        match self.code:
            case 0:
                return 'success'
            case -1:
                return 'failure: %s' % self.info
            case 1:
                return 'failure: critical differences in AST nodes/their properties (%s)' % self.info
            case 2:
                return 'failure: invalid input (%s)' % self.info

class Extractor:
    def __init__(self, src, silent=False):
        self.src = src
        with open(src) as file:
            self.lines = file.readlines()
        self.file = SourceFile(src)
        self.replacements = []
        self.replacements2 = []
        self.line_replacements = []
        self.methods = []
        self.counter = 1
        self.silent = silent
    def print(self, *args, **kwargs):
        if not self.silent:
            print(*args, **kwargs)
    def get_segment(self, pos1, pos2):
        line1, i1, line2, i2 = pos1[0] - 1, pos1[1] - 1, pos2[0] - 1, pos2[1]
        if line1 == line2:
            return self.lines[line1][i1:i2]
        return self.lines[line1][i1:] + ''.join(self.lines[line1+1:line2]) + self.lines[line2][:i2]
    def replace(self, position, len, new_s):
        self.replacements2.append(Replacement(position, len, new_s))
    def apply_replacements(self):
        self.replacements.extend(self.replacements2)
        self.replacements2 = []
    def replace_lines(self, start, end, new_lines):
        self.line_replacements.append(LineReplacement(tuple(start), tuple(end), new_lines))


    def detect_type(self, expression, var_manager, scope):
        if not isinstance(expression, list):
            return self.detect_type([expression], var_manager, scope)
        #print('detect_type', expression, types(scope))
        if len(expression) == 1:
            if isinstance(expression[0], str):
                t = var_manager.get_type(expression[0], scope)[1]
                if t is None: return '{undetermined_type}'
                return type_to_string(t)
            elif isinstance(expression[0], MemberReference):
                t = var_manager.get_type(expression[0].member, scope)[1]
                if t is None: return '{undetermined_type}'
                return type_to_string(t)
            elif isinstance(expression[0], This):
                return self.file.get_class_name(expression[0])
            elif isinstance(expression[0], MethodInvocation):
                argument_types = [self.detect_type(x, var_manager, scope) for x in expression[0].arguments]
                t = self.file.find_return_type(expression[0], expression[0].member, argument_types)
                if t is None: return '{undetermined_type}'
                return type_to_string(t)
        elif len(expression) == 2:
            if isinstance(expression[0], This) and isinstance(expression[1], MemberReference):
                t = var_manager.fields.get(expression[1].member)
                if t is None: return '{undetermined_type}'
                return type_to_string(t)
        return '{undetermined_type}'

    def extract_nonrecursively(self, *lines):
        self.replacements2 = []
        if len(lines) < 2:
            return ExtractionResult.invalid_input("only one block provided")

        blocks = list(map(lambda x: self.file.narrow_down(x[0], x[1]), lines))
        n = len(blocks)
        if isinstance(blocks[0], (MethodDeclaration, ConstructorDeclaration)):
            processing_method = True
            method_type = type_to_string(blocks[0].return_type)
            method_static = any(map(lambda x: 'static' in x.modifiers, blocks))
            blocks = list(map(lambda x: x.body, blocks))
        else:
            processing_method = False

        self.print('Processing method: %s' % processing_method)
        if processing_method:
            self.print('\tReturn type: %s' % method_type)
            self.print('\tIs method static: %s' % method_static)
        self.print()

        fields = self.file.get_field_declarations(blocks[0])
        vars = list(map(lambda x: VariableManager(fields, self.file.get_outscope_declarations(x)), blocks))
        params = []
        returns = []
        counter = 1
        const_params = {}
        var_params = {}
        ret_params = {}

        for paths, nodes in simultaneous_walk(blocks):
            node_types = list(map(lambda x: type(x).__name__, nodes))
            if len(set(node_types)) > 1:
                return ExtractionResult.different_properties(node_types)

            # if var declaration, add type
            if isinstance(nodes[0], VariableDeclaration):
                decls = list(map(lambda x: x.declarators, nodes))
                if len(set(map(len, decls))) > 1:
                    return ExtractionResult.different_properties(list(map(len, decls)))
                for decl in zip(*decls):
                    for i in range(n):
                        vars[i].add_inscope(decl[i].name, paths[i], nodes[i].type)

            # if parameter of an in-scope method declaration, add type
            if isinstance(nodes[0], MethodDeclaration):
                params = list(map(lambda x: x.parameters, nodes))
                if len(set(map(len, params))) > 1:
                    return ExtractionResult.different_properties(list(map(len, params)))
                for param in zip(*params):
                    for i in range(n):
                        vars[i].add_inscope(param[i].name, paths[i], param[i].type)

            # if for control, add type
            if isinstance(nodes[0], EnhancedForControl):
                for i in range(n):
                    for decl in nodes[i].var.declarators:
                        vars[i].add_inscope(decl.name, paths[i], nodes[i].var.type)

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
            if isinstance(nodes[0], (MemberReference, MethodInvocation, This)):
                if isinstance(paths[0][-1], (MemberReference, MethodInvocation, This)): continue
                refs = [separate_references(x) for x in nodes]

                # find "common" suffix
                suff_i = 0
                while suff_i < min(map(len, refs)):
                    curs = [x[len(x) - suff_i - 1] for x in refs]
                    sat = False
                    if all(map(lambda x: isinstance(x, str), curs)):
                        if len(set(curs)) == 1:
                            sat = True
                    if all(map(lambda x: isinstance(x, MemberReference), curs)):
                        if len(set([x.member for x in curs])) == 1:
                            sat = True
                    if all(map(lambda x: isinstance(x, MethodInvocation), curs)):
                        if len(set([x.member for x in curs])) == 1 and len(set([len(x.arguments) for x in curs])) == 1:
                            sat = True
                    if all(map(lambda x: isinstance(x, ArraySelector), curs)):
                        sat = True
                    if sat:
                        suff_i += 1
                    else:
                        break

                if any(map(lambda x: len(x) == suff_i, refs)):
                    if not all(map(lambda x: len(x) == suff_i, refs)):
                        suff_i += 1
                    else:
                        # exact match, add only if outscope
                        name = refs[0][0].member if isinstance(refs[0][0], MemberReference) else refs[0][0]
                        var_c, var_t = vars[0].get_type(name, paths[0])
                        if var_c == VarClass.OUTSCOPE:
                            names = (name,) * n
                            if names not in var_params:
                                params.append(Parameter("extracted%s" % counter, type_to_string(var_t), names))
                                counter += 1
                                var_params[names] = params[-1]
                            p = var_params[names]
                            self.replace(nodes[0].position, len(name), p.name)
                        continue


                # check that resulting prefixes are same/comparable and turn them to strings
                names = []
                var_types = []
                for i, x in enumerate(refs):
                    pref = x[:len(x) - suff_i]
                    # check that resulting prefixes are acceptable
                    if not all(map(lambda x: vars[i].no_inscopes(x, paths[i]), pref)):
                        return ExtractionResult.error('member ref: inscopes in prefix')

                    # defer the resulting parameter's type
                    var_types.append(self.detect_type(pref, vars[i], paths[i]))

                    # turn parameter to string
                    if not nodes[i].position:
                        return ExtractionResult.error('smd javalang')
                    token_i = self.file.find_token(nodes[i].position)
                    if token_i is None: continue
                    start = self.file.tokens[token_i].position
                    if isinstance(pref[0], MethodInvocation):
                        token_i = self.file.find_matching_paren(token_i + 1)
                    for y in pref[1:]:
                        if isinstance(y, MemberReference):
                            token_i += 2
                        if isinstance(y, ArraySelector):
                            token_i = self.file.find_matching_paren(token_i + 2)
                        if isinstance(y, MethodInvocation):
                            token_i = self.file.find_matching_paren(token_i + 3)
                    end_token = self.file.tokens[token_i]
                    end_pos = (end_token.position[0], end_token.position[1] + len(end_token.value) - 1)
                    names.append(self.get_segment(start, end_pos))

                if len(set(var_types)) > 1:
                    return ExtractionResult.different_properties(var_types)
                if var_types[0] == '{undetermined_type}':
                    var_type = input(f'Enter type for {'/'.join(names)} (leave blank if types are different): ')
                    if var_type == '':
                        return ExtractionResult.error('undetermined types reported as different')
                    var_types = [var_type]

                # add replacement
                names = tuple(names)
                if names not in var_params:
                    params.append(Parameter("extracted%s" % counter, var_types[0], names))
                    counter += 1
                    var_params[names] = params[-1]
                p = var_params[names]
                self.replace(nodes[0].position, len(names[0]), p.name)

            # assignment
            if type(nodes[0]) == Assignment:
                expressionls = list(map(lambda x: x.expressionl, nodes))
                new_paths = tuple([p + (x,) for p, x in zip(paths, nodes)])
                if all(map(lambda x: isinstance(x, This), expressionls)):
                    continue

                names = tuple(map(lambda x: x.member if not x.qualifier else x.qualifier, expressionls))
                res = [vars[i].get_type(names[i], paths[i]) for i in range(n)]
                cs, ts = map(list, zip(*res))
                if VarClass.INSCOPE in cs:
                    # inscopes have to always be returned
                    if any(map(lambda x: x != VarClass.INSCOPE, cs)):
                        return ExtractionResult.error("inscope matching with non-inscope")
                    if len(set(ts)) > 1:
                        return ExtractionResult.error("different inscope variables in the same place")
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

        self.apply_replacements()
        starts = [[lines[i][0], 1] for i in range(n)]
        ends = [[lines[i][1], len(self.lines[lines[i][1] - 1])] for i in range(n)]
        if processing_method:
            for i in range(n):
                while '{' not in self.lines[starts[i][0] - 1]:
                    starts[i][0] += 1
                starts[i][1] = self.lines[starts[i][0] - 1].index('{') + 2
                while '}' not in self.lines[ends[i][0] - 1]:
                    ends[i][0] -= 1
                ends[i][1] = self.lines[ends[i][0] - 1].index('}')

        suitable_place = min(map(lambda x: self.file.get_method_start_line(x), blocks)) - 1
        self.replace_lines((suitable_place, 1), (suitable_place, 1), [
            "\tprivate ",
            "static " if processing_method and method_static else "",
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
                'return ' if processing_method and method_type != 'void' else '',
                returns[0].assigns[i] + ' = ' if len(returns) > 0 else '',
                "extracted_method%s(" % self.counter,
                ", ".join(map(lambda x: x.values[i], params)),
                ");\n"
            ])
        self.counter += 1

        return ExtractionResult.success()


    def extract(self, *lines):
        result = self.extract_nonrecursively(*lines)
        if result.code != 1:
            return [[lines, result]]
        if len(set(result.info)) == len(lines):
            return [[lines, result]]
        d = {}
        for i in range(len(lines)):
            d.setdefault(result.info[i], [])
            d[result.info[i]].append(lines[i])

        results = []
        for group in d.values():
            results.extend(self.extract(*group))
        return results


    def output_to_file(self, dst='output.java'):
        self.print('\n--- Replacements (type 1) ---')
        for repl1 in self.replacements:
            self.print(repl1)
        self.print('\n--- Replacements (type 2) ---')
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
                    new_s += self.get_segment(y[0], y[1])
                else:
                    new_s += y
            replacements.append(LineReplacement(x.start, x.end, new_s))
        for pos1, pos2, x in replacements:
            self.lines[pos1[0]-1:pos2[0]] = self.lines[pos1[0]-1][:pos1[1]-1] + x + self.lines[pos2[0]-1][pos2[1]:]

        with open(dst,'w') as file:
            file.write(''.join(self.lines))



if __name__ == "__main__":
    if len(argv) >= 4:
        src = argv[1]
        ranges = list(map(lambda x: tuple(map(int, x.split('-'))), argv[2:]))
    else:
        src = input("source file: ")
        ranges = list(map(lambda x: tuple(map(int, x.split('-'))), input('enter blocks in the format of startline-endline startline-endline ...').split()))
    e = Extractor(src)
    for result in e.extract(*ranges):
        if result[1].code != ExtractionResult.SUCCESS:
            print(result[0], ': failure during extraction')
            print('code:', result[1].code)
            print('message:', result[1].info)
            break
    else:
        e.output_to_file('output.java')