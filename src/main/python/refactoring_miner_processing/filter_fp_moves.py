import json
import os
import re
import subprocess

from MethodSignature import MethodSignature

count = 0

class Field:
    def __init__(self, name: str, type: str):
        self.name = name
        self.type = type

def findFields(filepath, classname):
    gradle_path = ""
    outfile = ""
    subprocess.run(
        [
            gradle_path,
            "run",
            f"--args=\"-i {filepath} -o {outfile} -c {classname}\""
        ]
    )
    with open(outfile) as f1:
        field_data = json.load(outfile)

    return [Field(field['name'], field['type']) for field in field_data]


def is_tp_instance_move(refactoring):
    """
    MM is a TP if any of the following are true:
    1. Moved to a type in the original parameter list
    2. Moved to a type in the original class' fields
    3. New method signature contains a parameter of the original class.

    AND both source and target files exist in both versions of the code.
    """

    original_class = refactoring['description'].split(" to ")[0].split('.')[-1]
    target_class = refactoring['description'].split(" from class ")[-1].split('.')[-1]
    left_code_element = refactoring['leftSideLocations'][0]['codeElement']
    right_code_element = refactoring['rightSideLocations'][0]['codeElement']
    left_signature = MethodSignature.get_method_signature_parts(left_code_element)
    right_signature = MethodSignature.get_method_signature_parts(right_code_element)

    left_filepath = refactoring['leftSideLocations'][0]['filePath']

    original_class_fields = findFields(left_filepath, left_signature)

    for param in right_signature.params:
        if param.param_type == original_class:
            return True

        elif any([param.param_type == field.type for field in original_class_fields]):
            return True

    for param in left_signature.params:
        if param.param_type == target_class:
            return True

    return False


def is_tp_extractmovemethod(refactoring):
    original_class = refactoring['description'].split(' & moved to class')[0].split('.')[-1]
    right_code_element = \
        [i for i in refactoring['rightSideLocations'] if i['description'] == "extracted method declaration"] \
            [0]['codeElement'].split(":")[0]
    return re.search(r'\b' + re.escape(original_class) + r'\b', right_code_element) is not None


def is_tp_static_move(ref):
    """
    Checks if both source and target classes exist in both versions of the code.
    """
    # TODO: git checkout
    return os.path.exists(
        ref['rightSideLocations'][0]['filepath']
    ) and os.path.exists(
        ref['leftSideLocations'][0]['filepath']
    )


def isStaticMove(ref):
    # TODO: Check if static
    gradle_path = ""
    outputpath = ""
    filepath = ref['rightSideLocations'][0]['filepath']
    signature = ref['rightSideLocations'][0]['codeElement']
    subprocess.run([
        gradle_path,
        "run",
        f"--args=\"-i {filepath} -o {outputpath} -s \"{signature}\"\""
    ])
    with open(outputpath) as f1:
        isStatic = f1.read() == 'true'

    return isStatic


def checkout_prev_commit(sha1):
    # TODO: checkout commit
    pass


def filter_fp_moves(data):
    tp_moves = []
    for i in data['commits']:
        checkout_prev_commit(i['sha1'])
        for ref in i['refactorings']:
            if ref['type'] == 'Move Method':
                if isStaticMove(ref):
                    ref['isStatic'] = True
                else:
                    ref['isStatic'] = False

                if ref['isStatic'] and is_tp_static_move(ref):
                    new_data = create_new_data(i, ref)
                    tp_moves.append(new_data)
                elif is_tp_instance_move(ref):
                    new_data = create_new_data(i, ref)
                    tp_moves.append(new_data)
    return tp_moves


def create_new_data(i, ref):
    new_data = {
        **i,
        "move_method_refactoring": ref
    }
    new_data.pop("refactorings")
    return new_data


if __name__ == '__main__':
    filtered_path = "../../../../data/refminer_data/filter_fp"
    mm_path = "../../../../data/refminer_data/preprocessed"
    files = [i for i in os.listdir(mm_path) if i.endswith(".json")]
    for fname in files:
        print(fname)
        with open(os.path.join(mm_path, fname)) as f:
            move_data = json.load(f)
        filtered_moves = filter_fp_moves(move_data)
        with open(os.path.join(filtered_path, fname), "w") as f:
            json.dump(filtered_moves, f, indent=4)
