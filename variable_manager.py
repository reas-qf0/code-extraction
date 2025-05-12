from collections import namedtuple
from enum import Enum
from javalang.ast import walk_tree
from javalang.tree import MemberReference

InscopeVar = namedtuple('InscopeVar', ['name', 'scope', 'type', 'id'])
class VarClass(Enum):
    NOT_FOUND = -1
    FIELD = 0
    OUTSCOPE = 1
    INSCOPE = 2

class VariableManager:
    def __init__(self, fields, outscopes):
        self.fields = fields
        self.outscopes = outscopes
        self.inscopes = []
    def add_inscope(self, name, scope, type):
        l = len(self.inscopes)
        self.inscopes.append(InscopeVar(name, scope, type, l))
    def get_inscope_type(self, name, scope):
        for x_name, x_scope, x_type, x_id in self.inscopes:
            if x_name != name: continue
            if len(x_scope) > len(scope): continue
            b = True
            for x1, x2 in zip(scope, x_scope):
                if x1 is not x2:
                    b = False
                    break
            if not b: continue
            return x_id
        return None
    def get_type(self, name, scope):
        t = self.get_inscope_type(name, scope)
        if t is not None:
            return VarClass.INSCOPE, t
        if name in self.outscopes:
            return VarClass.OUTSCOPE, self.outscopes[name]
        if name in self.fields:
            return VarClass.FIELD, self.fields[name]
        return VarClass.NOT_FOUND, None
    def no_inscopes(self, block, scope):
        if isinstance(block, str):
            return self.get_inscope_type(block, scope) is None
        for path, node in walk_tree(block):
            if isinstance(node, MemberReference):
                if self.get_inscope_type(node.member, scope) is not None:
                    return False
        return True