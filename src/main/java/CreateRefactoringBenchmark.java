import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateRefactoringBenchmark {

    public static void main(String[] args) throws IOException {
//        GitHistoryRefactoringMinerImpl x = new GitHistoryRefactoringMinerImpl();
//        x.detectBetweenCommits();

        // read RMiner output.
        // For each "interesting file", that is: >=5 refactorings, in a single commit
        // Go to commit
        // Create inverse refactoring objects
        // Execute them. Remember which ones were executed successfully.
        // create branch, create commit and save this information somewhere.

        String projectDir = "/Users/abhiram/Documents/TBE/evaluation_projects/elasticsearch";
        String refminerOut = "/Users/abhiram/Documents/TBE/evaluation_projects/elastic-interesting-files.json";

        String jsonContent = Files.readString(Path.of(refminerOut));
        new JsonObject();
        JsonElement json = JsonParser.parseString(jsonContent);
        for (String filename : json.getAsJsonObject().keySet()) {
            JsonElement commitInfo = json.getAsJsonObject().get(filename).getAsJsonArray().get(0);
            String commitHash = commitInfo.getAsJsonObject().get("sha1").getAsString();

            // Checkout commit

            JsonArray refactorings = commitInfo.getAsJsonObject().get("refactorings").getAsJsonArray();
            for (JsonElement refactoring : refactorings) {
                createRefactoringObject(refactoring);
                // Execute refactoring
            }


        }

    }

    private static void createRefactoringObject(JsonElement refactoring) {

    }
}
