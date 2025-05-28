from javalang.tree import *

def is_list(obj):
    return isinstance(obj, (list, tuple))
def is_node(obj):
    return isinstance(obj, Node)
def is_node_array(obj):
    return isinstance(obj, (list, tuple)) and len(obj) > 0 and is_node(obj[0])
def types(path):
    if path is None: return None
    return list(map(lambda x: type(x).__name__, path))

def literal_to_type(value):
    if value == 'true' or value == 'false': return 'bool'
    if value[0] == '"': return 'String'
    if "." in value: return 'float'
    return 'int'
def type_to_string(type):
    if type is None: return "void"
    s = type.name
    if isinstance(type, ReferenceType) and type.arguments:
        s += '<' + ', '.join(map(lambda x: type_to_string(x.type), type.arguments)) + '>'
    s += "[]" * len(type.dimensions)
    return s

def simultaneous_walk(nodes, paths=None):
    n = len(nodes)
    if paths is None:
        paths = [() for _ in range(n)]
    #print('sim_walk', node1, node2)
    for i in range(1, n):
        if type(nodes[i]) != type(nodes[0]):
            yield paths, nodes
            return
    if is_list(nodes[0]):
        for xs in zip(*nodes):
            yield from simultaneous_walk(xs, paths)
    if is_node(nodes[0]):
        new_paths = tuple([p + (x,) for p,x in zip(paths, nodes)])
        for attr in nodes[0].attrs:
            values = list(map(lambda x: x.__dict__[attr], nodes))
            if None not in values:
                yield from simultaneous_walk(values, new_paths)
        yield paths, nodes

def separate_references(node):
    a = []
    if node.qualifier:
        a = node.qualifier.split('.')
    for selector in [node] + (node.selectors or []):
        a.append(selector)
    return a