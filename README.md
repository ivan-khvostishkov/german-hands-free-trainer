# German Hands-Free Audio Trainer by NoSocial.Net

A tool that creates narrated German language training videos from official Goethe-Zertifikat exam materials for A1, A2, and B1 levels.

## 🎯 **[📚 Grammar Cheat Sheets](GrammarCheatSheets.pdf)**
Essential German grammar reference tables for articles, pronouns, and noun declensions.

## How It Works

The trainer automatically:
1. Downloads official "Wortliste" PDFs from the Goethe Institut website
2. Extracts text using [Amazon Textract](https://aws.amazon.com/textract/)
3. Translates phrases from German to English with [Amazon Translate](https://aws.amazon.com/translate/)
4. Generates narration using [Amazon Polly](https://aws.amazon.com/polly/)
5. Creates videos with hardcoded subtitles using [FFmpeg](https://www.ffmpeg.org/)

## Training Videos

Use the [BookTube](https://github.com/ivan-khvostishkov/booktube) app for hands-free learning with random positioning and infinite looping:

* **German A1**: https://booktube.nosocial.net/?v=tSCrnPUt-pU&sleep=60&loop=true&pos=random&edit=true&title=German+A1+Trainer
* **German A2**: https://booktube.nosocial.net/?v=esvhhEGLTNA&sleep=60&loop=true&pos=random&edit=true&title=German+A2+Trainer
* **German B1**: https://booktube.nosocial.net/?v=wNoTY_TZvqI+UBcIZf76mIs+JYDI_HhWIgs&sleep=60&loop=true&pos=random&title=German+B1+Hands-Free+Audio+Trainer&edit=true

## Recommended Usage

Open the BookTube app on your mobile device in Edge or Firefox browser. Listen through headphones while walking outdoors for optimal learning experience.

---

*Parts of this project were generated with [Amazon Q Developer](https://aws.amazon.com/q/developer/).*
