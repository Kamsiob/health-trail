# Bundled typefaces

Every face the app renders is bundled in `android/app/src/main/res/font`. Nothing is fetched at runtime, because this app works offline and a typeface that needs the network is a typeface that is sometimes absent.

**Licenses were verified against `google/fonts` METADATA.pb**, not assumed. `DESIGN.md` section 4.3 asks for exactly that. All three families are under the SIL Open Font License 1.1, which permits bundling in a closed or open application provided the fonts are not sold on their own and the copyright notice travels with them.

| File | Family | Weight | Copyright | License | Checked |
|---|---|---|---|---|---|
| `roboto_regular.ttf` | Roboto | 400 | Copyright 2011 The Roboto Project Authors | OFL 1.1 | 2026-08-16 |
| `roboto_bold.ttf` | Roboto | 700 | Copyright 2011 The Roboto Project Authors | OFL 1.1 | 2026-08-16 |
| `jetbrains_mono_regular.ttf` | JetBrains Mono | 400 | Copyright 2020 The JetBrains Mono Project Authors | OFL 1.1 | 2026-08-01 |
| `noto_sans_arabic_regular.ttf` | Noto Sans Arabic | 400 | Copyright 2022 The Noto Project Authors | OFL 1.1 | 2026-08-01 |
| `noto_sans_arabic_bold.ttf` | Noto Sans Arabic | 700 | Copyright 2022 The Noto Project Authors | OFL 1.1 | 2026-08-01 |

800 KB for all five.

**The two Roboto files are static instances of Google's official variable font**, `Roboto[wdth,wght].ttf`, cut at wght 400 and 700 with wdth 100. The variable file is the only thing `google/fonts` ships now, and a variable font on the classpath means every weight in the app depends on axis support behaving the same way on every device. Two static files do not.

## Why these

**Roboto because the approved mockups are set in it.** D181. The owner, looking at the first captures off the rebuilt theme: "the style and the font and the icons are different". The mockups are the spec, and the app was rendering a different face. It is also what Material 3 Expressive itself is drawn against, so a Material component and a heading this app writes are the same letterforms rather than two that nearly agree.

**What that gave up.** Atkinson Hyperlegible was here until D181, and it was a functional choice rather than an aesthetic one: the Braille Institute drew it for maximum character distinction for low vision readers, and this audience is stressed, frequently older, and often reading in bad light. That argument was real and it lost to the design the owner approved. **The legibility work that survives is the part that was never about the face**: the 13sp floor, two text colors at measured contrast, and the size and weight jumps of rule 15.


## How the fallback works

Each family lists its Arabic face after its Latin one. Android matches a glyph against the family in order and falls through when a face does not have it, so Arabic text picks up Noto with no locale check anywhere in the code.

**That matters because one screen can hold both scripts at once**, a person's name in one inside a sentence in another, and a locale switch would get that wrong in exactly the case nobody tests.

In Arabic, display headings render in Noto at bold weight rather than forcing a Latin display face that has no coverage, which is what `DESIGN.md` section 4.3 asks for.

## Simplified Chinese is not bundled

**A size decision, stated rather than hidden.** Noto Sans SC is around ten megabytes per weight, against 680 kilobytes for everything above put together. Two weights would roughly double the download for one of four languages.

Chinese currently falls back to the system face, which on nearly every device that ships Chinese is a real and well made one. `DESIGN.md` section 4.3 says not to assume the system will cover it, and this is not an assumption: it is a known gap with a number attached, tracked on issue #12, and the options are a subset build, a variable font, or a downloadable font module.

## Verifying a face on the device

Run the app in one language without touching the phone's own settings:

```
adb shell cmd locale set-app-locales com.kamsiob.healthtrail --locales ar
adb shell cmd locale set-app-locales com.kamsiob.healthtrail --locales ""
```

The second line clears it. This is per app, so it never changes the system language, which matters because the test device is the owner's daily driver.

**Running Arabic this way immediately found something no check had:** the app chrome is translated and the template catalog is not, so the interface was Arabic and every situation name inside it was English. That is issue #62.

**Bricolage Grotesque was removed in D174.** A display face whose appeal is
its quirks does not pair with a legibility face, and the owner said so three
times. The file is out of the build rather than left unreferenced in `res/font`,
where it would ship in every APK for nothing.
