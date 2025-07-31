---
layout: default
---

*ICSME'25 Move-Method Assist!*

# Replication Package Outline

1. [Demo video](#demo-video) 
2. [Prompts](#prompts) 
3. [Tool download and installation instruction](#tool-download-and-installation-instructions)
4. [Source code](#source-code)
5. [Datasets](#datasets)

## Demo-video

<video width="640" height="480" controls>
  <source src="mm-pro-demo.mp4" type="video/mp4">
  Your browser does not support the video tag.
</video>

## Prompts

### Prompt used by Critique

```kotlin
    
    mutableListOf(
            SystemMessage.from("You are an expert Java developer who specializes in move-method refactoring."),
            
            UserMessage.from("""
                Analyze this class and prioritize all candidate methods for potential movement. Follow these exact steps:
    
                1. First, analyze each candidate method using this format:
                   - Method: [name]
                   - Purpose: [one-line description of what it does]
                   - Dependencies: [what data/methods it uses from current class vs other classes]
                   - Cohesion: [how related is it to class's main purpose]
    
                2. Then, summarize:
                   - The main responsibility of this class
                   - Which methods align with this responsibility
                   - Which methods could be better placed elsewhere
    
                3. Finally, provide a JSON array of the top **$limit** candidate method signatures ordered by priority (highest priority first).
                   **Ensure that exactly $limit method signatures are included.**
    
                **DO NOT include any summary or analysis in the output. Only return the JSON array.**
    
                Class code:
                ${classCode}
    
                Candidate method signatures:
                ${moveMethodSuggestions.mapIndexed { index, suggestion -> "${index + 1}. ${suggestion.methodSignature}" }.joinToString("\n")}
                         
                **Your final output should be only a JSON list of the top $limit method signatures ordered from highest priority to lowest priority, like the example below:**
    
                [
                    "public void calculateResults(String input)",
                    "private int fetchData(String query)",
                    "protected List<String> processItems(List<Integer> items)"
                    // ... up to $limit method signatures
                ]
                """.trimIndent()
            )
        )
```

### Prompt used to find target class

```kotlin
    mutableListOf(
            SystemMessage.from("You are an expert Java developer. " +
                    "You are told that a certain method doesn't belong to a class," +
                    " and it is your responsibility to decide which class the method should move to," +
                    " based on your expertise. "),
                    
            UserMessage.from("""
                Here is the method that needs to move:
                    ${methodCode}
                
                Please decide which target class is the best option:
                   ${Gson().toJson(movePivots.map { it.psiClass.name }) }} 

                Respond with ONLY a JSON list of objects (with keys "target_class" and "rationale"), with the most important target class suggestion at the beginning of the list. 
                Include detailed information in the rationale, including why the method needs to move out the existing class, and why it should move to the target class.  
                Ex:
                 [
                    {
                        "target_class": "Customer",
                        "rationale": "calculateDiscount() relies heavily on the Customer class, so it might be more appropriate to move this method to the Customer class.",
                    }
                ]
                
                Here are the class body of the potential target classes for your reference:
                    ${potentialClassBody}
                    
                     """.trimIndent()),
    )

```

## Tool Download and Installation Instructions
MM-Assist is an IntelliJ plugin. It requires Intellij 2024.1, and an OpenAI API key. 
Check [this](/installation) page, for detailed download and installation instructions.

## Source Code
To be made available after the paper is reviewed.

## Datasets
Download [here](artifacts/datasets.zip) 
