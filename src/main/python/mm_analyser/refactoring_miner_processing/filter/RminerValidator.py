import os
import pathlib
import subprocess

import git

from mm_analyser import data_folder, project_root
from mm_analyser.refactoring_miner_processing.filter.MoveMethodValidator import MoveMethodRef, MoveMethodValidator


class RminerValidator:
    output_dir = f"{data_folder}/refminer_data"
    gradle_path = f"{project_root}/gradlew"

    def __init__(self, refminer_commit_data, project_basepath):
        self.refminer_commit_data = refminer_commit_data
        self.tp_moves = []

        self.project_basepath = project_basepath
        self.repo = git.Repo(project_basepath)
        self.commit_after = refminer_commit_data['sha1']
        self.commit_before = self.repo.commit(self.commit_after).parents[0]

    @staticmethod
    def create_new_data(i, ref):
        new_data = {
            **i,
            "move_method_refactoring": ref
        }
        new_data.pop("refactorings")
        return new_data

    def checkout_after(self):
        self.repo.git.checkout(self.commit_after, force=True)

    def checkout_before(self):
        self.repo.git.checkout(self.commit_before, force=True)

    def isStaticMove(self, ref: MoveMethodRef):
        self.repo.git.checkout(self.commit_before, force=True)
        outputpath = f"{RminerValidator.output_dir}/isStaticOut.txt"
        filepath = os.path.join(self.project_basepath, ref.left_file_path)
        result = subprocess.run([
            MoveMethodValidator.gradle_path,
            "-p", str(pathlib.Path(MoveMethodValidator.gradle_path).parent),
            "run",
            f"--args="
            f"checkIfStatic -i {filepath} -o {outputpath} -s \'{ref.left_signature}\'"
        ], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode != 0:
            # return False
            raise Exception("Failed to find if method is static")
        with open(outputpath) as f1:
            isStatic = f1.read() == 'true'
        subprocess.run(['rm', outputpath])

        return isStatic
