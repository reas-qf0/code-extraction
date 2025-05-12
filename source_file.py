from node_operations import is_node, is_list, is_node_array
import javalang
from javalang.tree import *
from javalang.ast import walk_tree



def get_bounds(node):
    left = float('inf') if node.position is None else node.position[0]
    right = float('-inf') if node.position is None else node.position[0]
    for attr, value in zip(node.attrs, node.children):
        if value is None: continue
        if is_node(value):
            bounds = get_bounds(value)
            left = min(left, bounds[0])
            right = max(right, bounds[1])
        if is_node_array(value):
            left = min(left, get_bounds(value[0])[0])
            right = max(right, get_bounds(value[-1])[1])
    return left, right

def narrow_down(node, line_start, line_end):
    bounds = get_bounds(node)
    if bounds[0] >= line_start and bounds[1] <= line_end:
        return node
    for attr, value in zip(node.attrs, node.children):
        if is_node(value):
            bounds2 = get_bounds(value)
            if bounds2[0] <= line_end and bounds2[1] >= line_start:
                return narrow_down(value, line_start, line_end)
        if is_node_array(value):
            fitting_nodes = []
            for node in value:
                bounds2 = get_bounds(node)
                if bounds2[0] <= line_end and bounds2[1] >= line_start:
                    fitting_nodes.append(node)
            if len(fitting_nodes) == 1:
                return narrow_down(fitting_nodes[0], line_start, line_end)
            elif len(fitting_nodes) > 1:
                return fitting_nodes
    return None

class SourceFile:
    def __init__(self, path):
        self.path = path
        with open(path) as file:
            self.tokens = list(javalang.tokenizer.tokenize(file.read()))
        self.tree = javalang.parser.parse(self.tokens)
        self.walk = list(filter(lambda x: is_node(x[1]), walk_tree(self.tree)))
    def narrow_down(self, line_start, line_end):
        return narrow_down(self.tree, line_start, line_end)
    def get_path(self, node):
        if type(node) == list: node = node[0]
        for path, n in self.walk:
            if n is node:
                return path
        return None

    def find_token(self, pos):
        return next(filter(lambda i: self.tokens[i].position >= pos, range(len(self.tokens))))
    def find_matching_paren(self, i):
        parens = {'(': ')', '[': ']', '{': '}'}
        paren = self.tokens[i].value
        count = 1
        while True:
            i += 1
            if self.tokens[i].value == paren: count += 1
            if self.tokens[i].value == parens[paren]: count -= 1
            if count == 0: return i

    def get_field_declarations(self, node):
        path = self.get_path(node)
        res = {}
        for node in path[::-1]:
            if isinstance(node, ClassDeclaration):
                for node2 in node.body:
                    if isinstance(node2, FieldDeclaration):
                        for decl in node2.declarators:
                            res[decl.name] = node2.type
                return res
        return None

    def get_outscope_declarations(self, node):
        path = self.get_path(node)
        res = {}
        counting = False
        for node in path:
            if not counting:
                if isinstance(node, ClassDeclaration): counting = True
                continue
            if isinstance(node, ForStatement) and isinstance(node.control.init, VariableDeclaration):
                for decl in node.control.init.declarators:
                    res[decl.name] = node.control.init.type
            if isinstance(node, MethodDeclaration):
                for param in node.parameters:
                    res[param.name] = param.type
            if is_list(node):
                for node2 in node:
                    if isinstance(node2, VariableDeclaration):
                        for decl in node2.declarators:
                            res[decl.name] = node2.type
        return res

    def get_method_start_line(self, block):
        path = self.get_path(block)
        for node in path[::-1]:
            if isinstance(node, (MethodDeclaration, ConstructorDeclaration)):
                return node.position[0]
        return None