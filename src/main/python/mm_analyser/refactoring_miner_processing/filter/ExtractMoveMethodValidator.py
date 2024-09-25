import os
import subprocess
import pathlib
import json

import RminerValidator as rv
import MoveMethodValidator as mm


class ExtractMoveMethodRef(mm.MoveMethodRef):

    def __init__(self, right_file_path, left_file_path, right_signature: mm.MethodSignature,
                 left_signature: mm.MethodSignature, original_class, target_class, description,
                 extraction_start, extraction_end
                 ):
        super().__init__(right_file_path, left_file_path, right_signature, left_signature, original_class, target_class,
                         description)
        self.extraction_start = extraction_start
        self.extraction_end = extraction_end

    @staticmethod
    def create_from(ref):
        return ExtractMoveMethodRef()


class ExtractMoveMethodValidator(rv.RminerValidator):
    type = "Extract And Move Method"

    def get_valid_moves(self):
        print(self.project_basepath)
        for ref in self.refminer_commit_data['refactorings']:
            if ref['type'] == ExtractMoveMethodValidator.type:

                emm_obj = ExtractMoveMethodRef.create_from(ref)

                if not self.preconditions(emm_obj):
                    continue  # don't add to list
                if self.isStaticMove(emm_obj):
                    ref['isStatic'] = True
                    if self.is_tp_static_move(emm_obj):
                        new_data = super(self).create_new_data(
                            self.refminer_commit_data, ref)
                        self.tp_moves.append(new_data)

                else:
                    ref['isStatic'] = False
                    if self.is_tp_instance_move(emm_obj):
                        print("Found instance move!")
                        new_data = super(self).create_new_data(
                            self.refminer_commit_data, ref)
                        self.tp_moves.append(new_data)
                    else:
                        print("False-positive.")
        return self.tp_moves

    def preconditions(self, emm_obj: ExtractMoveMethodRef):
        return True

    def is_tp_instance_move(self, emm_obj):
        pass

    def is_tp_static_move(self, emm_obj):
        return True

    def findTypesInRange(self, emm_obj: ExtractMoveMethodRef, file_path):

        outputpath = f"{ExtractMoveMethodValidator.output_dir}/typesInRange.json"
        filepath = os.path.join(self.project_basepath, file_path)
        result = subprocess.run([
            super(self).gradle_path,
            "-p", str(pathlib.Path(super(self).gradle_path).parent),
            "run",
            f"--args="
            f"findTypesInRange -i {filepath} -o {outputpath} -s {emm_obj.extraction_start} -e {emm_obj.extraction_end}"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            raise Exception(f"Failed to find if types in range for {filepath}")
        with open(outputpath) as f:
            type_in_range = json.load(f)
        subprocess.run(['rm', outputpath])
        return type_in_range
