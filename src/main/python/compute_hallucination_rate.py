import json
class MoveMethodSuggestion:

    def __init__(self, method_name, target_class):
        self.target_class = target_class
        self.method_name = method_name

    def __hash__(self):
        return hash(self.method_name + self.target_class)

    def __eq__(self, other):
        if isinstance(other, MoveMethodSuggestion):
            return other.method_name == self.method_name and other.target_class == self.target_class
        return False


files = [
    'comparison_ant_large.json',
    'comparison_ant_small.json',
    'comparison_derby_large.json',
    'comparison_derby_small.json',
    'comparison_drjava_large.json',
    'comparison_drjava_small.json',
    'comparison_jfreechart_large.json',
    'comparison_jfreechart_small.json',
    'comparison_jgroups_large.json',
    'comparison_jgroups_small.json',
    'comparison_jhotdraw_large.json',
    'comparison_jhotdraw_small.json',
    'comparison_jtopen_large.json', #incomplete
    'comparison_junit_large.json',
    'comparison_junit_small.json',
    'comparison_mvnforum_large.json',
    'comparison_mvnforum_small.json',
    'comparison_tapestry_large.json',
    'comparison_tapestry_small.json',
    'comparison_lucene_small.json',
    'comparison_lucene_large.json',
]

combined_data = []
for fname in files:
    with open(f"../../../data/synthetic_corpus_comparison/{fname}") as f:
        combined_data += json.load(f)

combined_data = [i for i in combined_data if len(i['telemetry'].keys())]
total_suggestions = 0
plausible_suggestions = 0

for data in combined_data:
    telemetry = data["telemetry"]
    vanilla_suggestions = set()
    for iteration in telemetry["iterationData"]:
        for mm in iteration["suggested_move_methods"]:
            vanilla_suggestions.add(
                MoveMethodSuggestion(
                    mm["method_name"],
                    mm["target_class"]
                )
            )


    try:
        priority_methods = telemetry["llmMethodPriority"]["priority_method_names"]
    except:
        continue
    vanilla_suggestions_priority = [i for i in vanilla_suggestions
                                    if (i.method_name in priority_methods and i.method_name in telemetry["targetClassMap"])]
    total_suggestions += len(vanilla_suggestions_priority)
    for suggetion in vanilla_suggestions_priority:
        if (suggetion.target_class in telemetry["targetClassMap"][suggetion.method_name]["target_classes_sorted_by_llm"]):
            plausible_suggestions += 1

plausible_rate = plausible_suggestions/total_suggestions
print(f"{plausible_rate=}")
print(f"{1-plausible_rate=}")
print(f"{plausible_suggestions=}")
print(f"{total_suggestions=}")
