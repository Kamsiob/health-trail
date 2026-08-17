# The acceptance criteria for the rebuild

**The owner set these on 2026-08-17 and they are the definition of done for the whole interface replacement.** Written here because a session compacts and this must survive it.

**The run does not stop until every phase below is finished.** A phase is finished when its evidence is in the repository, not when it feels done.

---

## The method, which is not negotiable

**Nothing old is edited. Everything is written from scratch.** The owner, 2026-08-17: "no old design language at all. get rid of it so it doesn't influence. there's no reason to change it or update it. you're building from scratch."

**This corrects how the run was going.** Components were being recolored in place: a raised card became a tonal one, a chevron became a symbol, a button became Material's. **That is exactly what [D178] forbids and what `docs/V4.md` 3 calls half converted.** A recolored old component is still the old arrangement, and the old arrangement is what the owner keeps recognizing.

**So:**

1. **The new component set lives in `ui/v4/`** and is written from Material 3 Expressive primitives and the language in `docs/V4.md` 2.1. Nothing is copied from the old files.
2. **A screen is rewritten onto the new set**, never adapted. `docs/V4.md` 3: rewritten or left alone.
3. **An old component is deleted the moment its last call site is gone.** Not deprecated, not left for later, deleted, so it cannot influence anything.
4. **The old package is empty when this is finished.** That is the test of whether the language was replaced rather than repainted.

## The phases, in order

**1. The drawn screens.** Apply the mockups' design to the screens `docs/screenshots/m3v4-{0..5}-light.png` draw: Today, the notebook, a project, the care team, a form, a document. Top to bottom, left to right, fully.

**2. Extract the language.** The mockups are six screens and the app has far more. **Write down the underlying design philosophy and design language as rules**, not as a description of six pictures, so every screen the mockups do not draw can be built from the rules rather than guessed at. `docs/V4.md` is where it goes, and rule 12 already governs a screen nobody drew.

**3. Every element and every component.** Apply the extracted language to **every component in `ui/components` and every element on every screen**, not only the ones the mockups happen to contain.

**4. Every screen, end to end.** Continue until the entire app is converted, A to Z. No screen is skipped and none is left half converted, `docs/V4.md` 3.

**5. Verification that everything was touched.** A team confirms that every screen and every component was in fact updated and refreshed. **Read only, rule 8**: subagents check and report, they never write. The output is a list of what was verified and what was missed.

**6. Expert review.** A dedicated panel of user interface, user experience, design and visual effects reviewers looks for areas to improve in **every part** of the app, and reports them. Findings are fixed, not filed.

**7. User testing by persona.** In depth walks from the personas in `TESTING-PERSONAS.md`, on the phone, looking for what breaks for a real person rather than what fails a check.

**8. Final quality control.** One last pass: everything works, the checks pass, the suite passes, read from the counts and never from the exit code.

**9. Deliver the APK** for the owner to test.

---

## What binds every phase

- **Rule 8: subagents never write.** The teams in phases 5 to 7 read, run, check and report. Every fix is written by the session itself.
- **Rule 21: look at it on the phone.** No screen is called done from a screenshot alone, and no phase is called done from a check passing. `HANDOFF.md` section 5.7.
- **Rule 11: nothing unfinished reaches the person.** Every screen ships with its empty, one, many, partially filled, long text, loading and error states.
- **Rule 19: accessibility is a gate**, verified with the reader on, font at maximum, and reduced motion actually enabled.
- **Rule 24 as amended by D180: American English only.** RTL is not a gate.
- **D183: measure the drawing rather than judge it.** The captures are 1080 at 3x, so dp is px/3.

## The delivery

**`HANDOFF.md` said not to build a release APK** because the owner was holding delivery until he approved the design. **Phase 9 supersedes that**: he asked for the APK as the last step of this run. It is built when phases 1 to 8 are finished and not before.

## Progress

**Keep this section current. It is the only place that says how far the run got.**

| Phase | State |
|---|---|
| 1. The drawn screens | **done: all six.** Today, the notebook, the project, the care team and the document carry the drawing. The form was already right |
| 2. Extract the language | done: `docs/V4.md` 2.1, every rule measured off the drawings |
| 3. Every component | `ui/v4` has `Block`, `BlockTone`, `FactBlock`, `Eyebrow`, `Lead`, `BigNumber`, `Body`, `ListRow`, `RowDivider`, `SearchDoor`, `PaperCard`, `Action`, `ActionEmphasis`, `IconAction`, `Sheet`, `SheetBody`, `rememberSheet`, `Avatar`, `PersonHero`, `PersonRow`, `Segments`. `Page` and `labeledBlock` are the interior scaffold: the same back tag the old footer had so tests press back unchanged, a hero above the name for a screen whose subject is an object, and **its own window insets, because a page opens over the shell rather than inside it**. Still to write: the stat block, the field, the chips, the switch row, the spine, the chart |
| 4. Every screen | **7 of 85 rewritten onto `ui/v4`: the notebook, the document, the care team, the person, medications, the questions, About.** The old set is still standing because it still has callers: `QuietButton` 153, `GroupedSurface` 101, `DenseRow` 99, `GroupHeader` 90, `SectionScaffold` 70, `Thumbnail` 12. Each is deleted as its last caller goes |
| 5. Verify everything was touched | not started |
| 6. Expert review | not started |
| 7. Persona testing | not started |
| 8. Final quality control | not started |
| 9. Deliver the APK | not started |
