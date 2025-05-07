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
    return type.name + "[]" * len(type.dimensions)

def simultaneous_walk(node1, node2, path1=tuple(), path2=tuple()):
    #print('sim_walk', node1, node2)
    if type(node1) != type(node2):
        yield path1, path2, node1, node2
        return
    if is_list(node1):
        for x1, x2 in zip(node1, node2):
            for x in simultaneous_walk(x1, x2, path1, path2):
                yield x
    if is_node(node1):
        yield path1, path2, node1, node2
        for attr, value1, value2 in zip(node1.attrs, node1.children, node2.children):
            if value1 is not None and value2 is not None:
                for x in simultaneous_walk(value1, value2, path1 + (node1,), path2 + (node2,)):
                    yield x