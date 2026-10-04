package io.github.balbianoluciano.ljbu.api.web;

import io.github.balbianoluciano.ljbu.api.content.Challenge;
import io.github.balbianoluciano.ljbu.api.content.ChallengeSpec;
import io.github.balbianoluciano.ljbu.api.content.Content;
import io.github.balbianoluciano.ljbu.api.content.Domain;
import io.github.balbianoluciano.ljbu.api.web.Views.Analogy;
import io.github.balbianoluciano.ljbu.api.web.Views.ChallengeSummary;
import io.github.balbianoluciano.ljbu.api.web.Views.ChallengeView;
import io.github.balbianoluciano.ljbu.api.web.Views.Criterion;
import io.github.balbianoluciano.ljbu.api.web.Views.GoverningBody;
import io.github.balbianoluciano.ljbu.api.web.Views.Hint;
import io.github.balbianoluciano.ljbu.api.web.Views.ModuleList;
import io.github.balbianoluciano.ljbu.api.web.Views.ModuleSummary;
import io.github.balbianoluciano.ljbu.api.web.Views.PieceSummary;
import io.github.balbianoluciano.ljbu.api.web.Views.RealReference;
import io.github.balbianoluciano.ljbu.api.web.Views.Reference;
import io.github.balbianoluciano.ljbu.api.web.Views.RegionalFaculty;
import io.github.balbianoluciano.ljbu.api.web.Views.Rule;
import io.github.balbianoluciano.ljbu.api.web.Views.Solution;
import io.github.balbianoluciano.ljbu.api.web.Views.StarterFile;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Serves the content. Hints and the solution have their own routes: the web asks when due. */
@RestController
@RequestMapping("/api/v1")
public class ChallengeController {

  private final Content content;

  public ChallengeController(Content content) {
    this.content = content;
  }

  @GetMapping("/modules")
  public ModuleList modules(@RequestParam(defaultValue = "es") String lang) {
    return new ModuleList(
        content.modules().stream()
            .map(
                module ->
                    new ModuleSummary(
                        module.id(),
                        module.order(),
                        module.title().in(lang),
                        module.goal().in(lang),
                        content.challengesOf(module.id()).stream()
                            .map(Challenge::spec)
                            .map(
                                spec ->
                                    new ChallengeSummary(
                                        spec.id(),
                                        spec.order(),
                                        spec.title().in(lang),
                                        spec.concept(),
                                        expectedPieces(spec, lang)))
                            .toList()))
            .toList());
  }

  @GetMapping("/challenges/{id}")
  public ChallengeView challenge(
      @PathVariable String id, @RequestParam(defaultValue = "es") String lang) {
    Challenge challenge = find(id);
    ChallengeSpec spec = challenge.spec();
    ChallengeSpec.RealReference reference = spec.scene().realReference();
    List<String> faculties =
        reference == null || reference.regionalFaculties() == null
            ? List.of()
            : reference.regionalFaculties();
    List<String> bodies =
        reference == null || reference.governingBodies() == null
            ? List.of()
            : reference.governingBodies();
    return new ChallengeView(
        spec.id(),
        spec.module(),
        spec.order(),
        spec.title().in(lang),
        spec.concept(),
        spec.brief().in(lang),
        spec.criteria().stream()
            .map(criterion -> new Criterion(criterion.checks(), criterion.text().in(lang)))
            .toList(),
        challenge.starter().stream()
            .map(
                file ->
                    new StarterFile(file.path(), file.content(), challenge.isEditable(file.path())))
            .toList(),
        spec.hints().size(),
        spec.rules().stream()
            .map(content.domain().rules()::get)
            .map(ChallengeController::view)
            .toList(),
        new RealReference(
            faculties.stream()
                .map(content.domain().regionalFaculties()::get)
                .map(
                    faculty ->
                        new RegionalFaculty(
                            faculty.id(), faculty.name(), faculty.city(), faculty.province()))
                .toList(),
            bodies.stream()
                .map(content.domain().governingBodies()::get)
                .map(
                    body ->
                        new GoverningBody(
                            body.id(),
                            body.name(),
                            body.kind(),
                            body.composition(),
                            body.mandateInYears()))
                .toList()),
        spec.analogy() == null
            ? null
            : new Analogy(
                spec.analogy().passed().in(lang),
                spec.analogy().incomplete().in(lang),
                spec.analogy().failed().in(lang)),
        (spec.references() == null ? List.<ChallengeSpec.Reference>of() : spec.references())
            .stream()
                .map(doc -> new Reference(doc.title().in(lang), doc.url(), doc.source()))
                .toList());
  }

  @GetMapping("/challenges/{id}/hints/{level}")
  public Hint hint(
      @PathVariable String id,
      @PathVariable int level,
      @RequestParam(defaultValue = "es") String lang) {
    Challenge challenge = find(id);
    if (level < 1 || level > challenge.spec().hints().size()) {
      throw ApiException.notFound("hint_not_found", "The challenge has no hint of that level.");
    }
    return new Hint(level, challenge.spec().hints().get(level - 1).in(lang));
  }

  @GetMapping("/challenges/{id}/solution")
  public Solution solution(@PathVariable String id) {
    Challenge challenge = find(id);
    return new Solution(challenge.solution(), challenge.solutionExplanation());
  }

  private Challenge find(String id) {
    return content
        .challenge(id)
        .orElseThrow(
            () ->
                ApiException.notFound("challenge_not_found", "There is no challenge " + id + "."));
  }

  private static Rule view(Domain.Rule rule) {
    return new Rule(rule.id(), rule.statement(), rule.source(), rule.url());
  }

  /** The pieces the brief asks for, drawn with the archetype of their binding. */
  private static List<PieceSummary> expectedPieces(ChallengeSpec spec, String lang) {
    return spec.scene().pieces().stream()
        .map(
            piece ->
                new PieceSummary(
                    spec.scene().bindings().stream()
                        .filter(binding -> binding.type().equals(piece.type()))
                        .map(ChallengeSpec.Binding::archetype)
                        .findFirst()
                        .orElse("generic-block"),
                    piece.label().in(lang)))
        .toList();
  }
}
