# Bilibili additional audio reference review

Reviewed 2026-11-08. These are public gameplay reference recordings, not verified isolated sound assets. No audio playback or perceptual audition was performed. No game sound resources were changed or clips extracted by this research subtask.

## Acquisition

Both pages downloaded using normal public yt-dlp access, without cookies or authentication bypass. Socket timeout 15 seconds; retries 1. Audio and video remain separate. Metadata retains source and duration. Higher premium-only formats were not requested.

| Source | Public page/title | Duration | Audio | Video |
|---|---|---:|---|---|
| BV1HX4y1V7oM | https://www.bilibili.com/video/BV1HX4y1V7oM/ — apex 进毒圈声音bug，连环响了解一下; 悠远之; 2021-03-11 | 68.939s | AAC format 30280, ~1.69 MiB | format 30064, 720p, ~14.35 MiB |
| BV1yRKVzyEoW | https://www.bilibili.com/video/BV1yRKVzyEoW/ — APEX万蒂奇史上最吊位移，50米！直接干到重生飞船里面！; SKY_WILL阿毅; 2025-06-25 | 32.387s | AAC format 30280, ~673 KiB | format 100046, 360p, ~1.54 MiB; 30064 unavailable |

## Evidence and candidate review windows

OCR sampled video frames every 3 seconds. OCR identifies visible text, not audio content; a caption cannot establish isolation, noise level, exact sound onset, or audible rights ownership.

### Ring: BV1HX4y1V7oM

The page title explicitly describes a repeating ring-entry sound bug. Description: 我已经崩溃了，重生求你修修这个bug吧. This is stronger event relevance than a generic SFX compilation.

The sampled frames from 0 through 66 seconds contain gameplay HUD, compass, player names, kill feed, and prompts; 热能护盾 is visible at 21–24 seconds. No sampled caption names a ring closing, ring warning, ring-entry, or damage tick event. No precise ring sound onset can be inferred from OCR.

Candidate review window: the complete 0–68.939s recording, with 18–27s useful for manual inspection of the heat-shield context. That context does not prove a clean ring damage sound. Do not label any arbitrary subwindow as a confirmed original isolated ring sound.

Verdict: usable as a downloaded original-gameplay reference for later listening, unavailable as a verified ready-to-install ring asset.

### Ship: BV1yRKVzyEoW

Observed captions/states:

- 3s: 补给仓要来了.
- 6s: 这么活，核子你完了 (OCR may contain recognition mistakes).
- 9s: 等待重生中 / 击杀回放.
- 12s: 兄弟们这个版本改动啥.
- 15s: custom firing-range menu, 但是我一进来玩这个万地奇 (recognition of legend name imperfect).
- 18s: 玩的我是笑不拢嘴啊, over firing-range UI.
- 21s: 50米的距离兄弟们你们知道吗, over firing-range UI.
- 24s: 我的绝招准备好了！ and 重生飞船里面, still with firing-range menu; mention in commentary does not prove a ship engine sound at this time.
- 27s: 兄弟们赶紧上线看看吧, over firing-range UI.

These captions demonstrate commentary and scene changes. The ship in the title does not establish continuous engine ambience. Candidate manual review window: 0–9s for gameplay preceding the death replay. The 9–15s transition and later firing-range demonstration must not be described as clean ship engine footage. No isolated door opening/closing sound is identified.

Verdict: usable as a downloaded gameplay/commentary reference, unavailable as a verified ready-to-install engine/door asset.

## Source and reuse limits

Public download success establishes technical access, not permission to redistribute EA sound assets in another game. No explicit EA license for these clips or isolated ship/ring SFX was found. The ring page also displays 未经作者授权，禁止转载. The prior official EA creator hub lists stream overlays and creator visuals, not a downloadable ship/ring SFX pack. Keep references separately attributed and do not claim synthetic replacements are original Apex audio.

## Existing pure SFX compilation

Parent reviewed BV1sB4y1v7C2 with OCR and found weapon/healing/ability sounds, without ship or ring sounds. Its metadata contains no subtitles or chapters. Do not relabel one of those unrelated effects as ship/ring audio.

## Frozen per-event status

| Event | Verified original isolated clip | Final status |
|---|---|---|
| ring warning | None | Missing; leave original-audio interface silent until identified audio is available. |
| ring closing | None | Missing; no verified closing announcement or closing SFX segment. |
| ring damage | None | Ring-entry-bug gameplay downloaded, but damage tick not identified or isolated. Missing as installable original asset. |
| engine | None | Gameplay/commentary downloaded; no verified clean engine start/loop/stop. Any temporary original composition must be labelled as such. |
| door | None | No source identified for isolated ship door open/close. |
| exit/jump | None | No verified isolated ship-exit or jump-wind clip acquired. |

Research frozen after the two successful downloads and completed OCR. Both background jobs exited 0 and were collected. No game resources were edited by this subtask. The parent is handling its separately built V7 installation and any temporary original-composition transport sounds; those are not Apex original audio.

## Follow-up: announcer event semantics

The community wiki page https://apexlegends.wiki.gg/wiki/Announcer/Default_Voice_lines was readable through read_page on 2026-11-08. Its Ring Status section identifies “Warning - Ring movement in progress.” and “Warning - Ring closing.” as ring-closing announcements. These describe a circle already moving, so neither should be labelled as the ten-second advance warning. Countdown lines instead announce the beginning of a round countdown. This establishes event wording only; it does not supply a verified audio file or a precise advance-warning clip.

Direct raw-markup inspection returned HTTP 403; this is recorded in announcer-media-review.json. No audio was acquired, and no restriction was bypassed. Bilibili search still returned a verification page, and the cloud page reader reported rate limiting; existing downloaded references remain intact. Installed game resources were not changed by this follow-up.

## Follow-up: official creator ZIP inspection

On 2026-11-08 the official EA stream-assets pages were fetched as raw HTML and exposed two exact download URLs. The ordinary package https://media.contentapi.ea.com/content/dam/apex-legends/common/creator-resources/apex-legends-stream-package.zip was inspected in memory (33,640,322 bytes, 20 ZIP entries). Extensions were .gif, .png, .txt, .url, and .webm; there were no independent .wav/.ogg/.mp3/.flac/.m4a entries. This does not establish whether its WebM files contain audio and does not prove the entire package is silent. No ring/ship audio was identified or installed.

The follow-up increased the bounded read limit to 256 MiB and inspected both packages completely. The second official package https://media.contentapi.ea.com/content/dam/apex-legends/common/creator-resources/apex-late-winter-stream-package.zip is 88,330,328 bytes with 14 entries, also without independent audio entries. All four ordinary-package WebM files and all three winter-package WebM files were extracted only under references/official-stream-media and probed using the existing bundled ffmpeg executable: all seven have no audio stream. ffmpeg's expected non-zero missing-stream result was explicitly checked for the text “matches no streams”; it is not a failed decoding claim. These two exact official creator packages therefore supply none of the requested sounds. Exact source links and outcomes are recorded in ea-creator-archive-links.json and ea-creator-archive-contents.json. Nothing was extracted to mod resources or copied into the running instance.

## Result

New successfully acquired public reference media: two AAC tracks and two separate videos. Confirmed isolated original ring, engine, and door clips: none. No extraction timestamps or clean-loop claims are supported without listening and further selection. Target desired lengths after identification: engine start 1–2s, stable loop 3–8s with 80–150ms crossfade, stop 1–3s, door 0.5–2s with full transient, ring entry/warning as complete one-shots, ring damage as a single correctly identified tick. These are editing requirements, not observed timings.
