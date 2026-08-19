# The three the owner saw, traced to the code, 2026-08-18

## D-1  Six headers, not one

| where | bar | scrolled color | title style | collapses |
|---|---|---|---|---|
| `ui/v4/Page.kt` (62 screens) | LargeFlexibleTopAppBar | **surfaceContainer** | bar default | yes, exitUntilCollapsed |
| `screens/TodayField.kt` (Today, live) | own LargeFlexibleTopAppBar | **background** | bar default | yes |
| `screens/Notebook.kt` | own | **background** | bar default | yes |
| `screens/MedicationList.kt` | own | **surfaceContainer** | bar default | yes |
| `screens/OneThread.kt` | own | **background** | bar default | yes |
| `screens/ProjectsScreen.kt` (destination) | **none** | n/a | `type.displayM` | **no**, scrolls away |
| `screens/TodayScreen.kt` Masthead (fallback) | **none** | n/a | eyebrow + name in list | no |
| Setup, Disclaimer, SituationPicker, CorrectSubject | **none** | n/a | `type.displayL` in a Column | no |

Four separate title treatments, two scroll models, two scrolled surfaces.

## D-2  Title and lamp are on different rows while the bar collapses

`LargeFlexibleTopAppBar` pins `actions` to the top row and puts the large title
on the second. Expanded, the lamp sits above and right of the title; collapsed,
the title rises into the lamp's row. Every intermediate scroll offset draws them
misaligned. That is Material's own behavior, not a bug in the call, so matching
Today means picking one collapse model and one bar for the whole app.
Projects is the opposite failure: title and lamp are one `Row` inside the list,
so they stay aligned and both leave the screen entirely, with nothing pinned.

## D-3  Empty states: 23 `SectionEmpty` calls, 8 with no action

No action passed, where one exists on the screen:
- `IncidentScreen.kt`, `EmergencyCardScreen.kt`, `ProjectPeopleScreen.kt`,
  `ProjectTrailScreen.kt`, `MonthReviewScreen.kt`, `ProjectPaperworkScreen.kt`,
  `BinScreen.kt`, `SearchScreen.kt`.
Bin and Search are correct: nothing to do from an empty bin or a search with no
hits. The other six have a real action and do not offer it inside the block.
The action, where passed, is `ActionEmphasis` quiet and the screen's floating
button is the filled one, so the block never carries the emphasized route.
