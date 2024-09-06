import json


def myindex(list, ele):
    try:
        return list.index(ele)
    except ValueError:
        return -1


plugin_outfiles = [
    'comparison_derby_small.json',
    'comparison_ant_small.json',
    'comparison.json',
    'comparison_drjava_large.json',
    'comparison_drjava_small.json',
    'comparison_jfreechart_large.json',
    'comparison_jfreechart_small.json',
    'comparison_jgroups_large.json',
    'comparison_jgroups_small.json',
    'comparison_jhotdraw_large.json',
    'comparison_jhotdraw_small.json',
    'comparison_jtopen_large.json',
    'comparison_junit_large.json',
    'comparison_junit_small.json',
    'comparison_mvnforum_large.json',
    'comparison_mvnforum_small.json'

]

combined_output = []
for file_name in plugin_outfiles:
    with open(f'../../../data/{file_name}') as f:
        data = json.load(f)
    combined_output += data
combined_output = [i for i in combined_output if len(i['telemetry'].keys())]

for evaluation_data in combined_output:
    oracle = evaluation_data['oracle']
    method_name = oracle.split("::")[1].split('(')[0]
    alias_method_name = method_name + '2'

    target_class = oracle.split('.')[-1]

    telemetry = evaluation_data['telemetry']

    if len(telemetry['candidatesTelemetryData']['candidates']) == 0:
        evaluation_data['recall_method_and_class_position'] = -1
        evaluation_data['recall_method_position'] = -1
        continue

    filtered_llm_priority = [m for m in telemetry["llmMethodPriority"]['priority_method_names']
                             if
                             m in telemetry["targetClassMap"] and len(telemetry["targetClassMap"][m]['target_classes'])]
    evaluation_data['recall_method_position'] = \
        max(myindex(filtered_llm_priority, method_name), myindex(filtered_llm_priority, alias_method_name))
    if evaluation_data['recall_method_position'] == -1:
        evaluation_data['recall_method_class_position'] = -1
        continue
    try:
        suggested_target_classes = telemetry['targetClassMap'][method_name]['target_classes_sorted_by_llm']
    except KeyError:
        suggested_target_classes = telemetry['targetClassMap'][alias_method_name]['target_classes_sorted_by_llm']
    evaluation_data['recall_method_class_position'] = \
        myindex(suggested_target_classes, target_class)

recall_method_and_class_1 = len(
    [i for i in combined_output if i['recall_method_position'] == 0 and i['recall_method_class_position'] == 0]) / len(
    combined_output)
recall_method_1 = len([i for i in combined_output if i['recall_method_position'] == 0]) / len(combined_output)

recall_method_and_class_2 = len([i for i in combined_output if
                                 i['recall_method_position'] in [0, 1] and i['recall_method_class_position'] in [
                                     0]]) / len(combined_output)
recall_method_2 = len([i for i in combined_output if i['recall_method_position'] in [0, 1]]) / len(combined_output)


recall_method_and_class_all = len([i for i in combined_output if
                                 i['recall_method_position']!=-1 and i['recall_method_class_position'] !=-1]) / len(combined_output)
recall_method_all = len([i for i in combined_output if i['recall_method_position'] !=-1]) / len(combined_output)

print(f"recall method @1 = {recall_method_1}")
print(f"recall method&class @1 = {recall_method_and_class_1}")

print(f"recall method @2 = {recall_method_1}")
print(f"recall method&class @2 = {recall_method_and_class_1}")
print(combined_output)
