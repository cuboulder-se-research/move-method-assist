import git

class RminerValidator:
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
