# Bundled typefaces

Every face the app renders is bundled in `android/app/src/main/res/font`. Nothing is fetched at runtime, because this app works offline and a typeface that needs the network is a typeface that is sometimes absent.

**Licenses were verified against `google/fonts` METADATA.pb on 2026-08-01**, not assumed. `DESIGN.md` section 4.3 asks for exactly that. All four families are under the SIL Open Font License 1.1, which permits bundling in a closed or open application provided the fonts are not sold on their own and the copyright notice travels with them.

| File | Family | Weight | Copyright | License |
|---|---|---|---|---|
| `bricolage_grotesque_bold.ttf` | Bricolage Grotesque | 700 | Copyright 2022 The Bricolage Grotesque Project Authors | OFL 1.1 |
| `atkinson_hyperlegible_regular.ttf` | Atkinson Hyperlegible | 400 | Copyright 2020 Braille Institute of America, Inc. | OFL 1.1 |
| `atkinson_hyperlegible_bold.ttf` | Atkinson Hyperlegible | 700 | Copyright 2020 Braille Institute of America, Inc. | OFL 1.1 |
| `jetbrains_mono_regular.ttf` | JetBrains Mono | 400 | Copyright 2020 The JetBrains Mono Project Authors | OFL 1.1 |
| `noto_sans_arabic_regular.ttf` | Noto Sans Arabic | 400 | Copyright 2022 The Noto Project Authors | OFL 1.1 |
| `noto_sans_arabic_bold.ttf` | Noto Sans Arabic | 700 | Copyright 2022 The Noto Project Authors | OFL 1.1 |

680 KB for all six.

## Why these

**Atkinson Hyperlegible is a functional choice, not an aesthetic one.** The Braille Institute designed it for maximum character distinction for low vision readers. The audience for this app is stressed, frequently older, and often reading in bad light, which is the case the face exists for. It is the reason the body face is not simply the display face at a smaller size.

Bricolage Grotesque carries display text. JetBrains Mono carries eyebrows, counts, timestamps, and metadata.

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
