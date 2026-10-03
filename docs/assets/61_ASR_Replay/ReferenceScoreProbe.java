import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.kidslanguage.companion.core.model.PracticeItem;
import com.kidslanguage.companion.core.scoring.ChildSpeechEvaluationService;
public class ReferenceScoreProbe {
  static String decode(String value) { return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8); }
  public static void main(String[] args) throws Exception {
    var service = new ChildSpeechEvaluationService();
    var output = new ArrayList<String>();
    for (var line : Files.readAllLines(Path.of(args[0]))) {
      var fields = Arrays.stream(line.split("\t", -1)).map(ReferenceScoreProbe::decode).toList();
      var item = new PracticeItem(fields.get(0), fields.get(1), "reference", List.of("repeat"), fields.get(2),
          null, null, null, null, null, fields.subList(4, fields.size()), List.of());
      for (int attempt = 1; attempt <= 3; attempt++) {
        var result = service.evaluate(fields.get(3), item, attempt);
        output.add(fields.get(0) + "\t" + fields.get(1) + "\t" + attempt + "\t" + result.getStatus() + "\t" + result.getScore());
      }
    }
    Files.write(Path.of(args[1]), output, StandardCharsets.UTF_8);
  }
}
