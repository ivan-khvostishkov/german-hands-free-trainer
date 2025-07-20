# German Hands-Free Trainer by NoSocial.Net

German Hands-free Trainer is the tool that narrates phrases from German A1-A2 and B1 [Goethe-Zertifikat](https://www.goethe.de/de/spr/kup/prf/prf/sd1/inf.html) exams.

It works by downloading the "Wortliste" PDF with German words and sample phrases from the Goethe Insitut website, parsing it with [Amazon Textract](https://aws.amazon.com/textract/), extracting and translating the phrases from German into English by [Amazon Translate](https://aws.amazon.com/translate/) and narrating the phrases with [Amazon Polly](https://aws.amazon.com/polly/). Finally, it assembles the audio files into a video file and hardcodes subtitles with [FFMpeg](https://www.ffmpeg.org/) using the speech marks that Polly produces during speech-to-text conversion. The resulting videos I've published on YouTube on my channel.

For convenience and for regular training, use the BookTube app that has the language learning feature to jump to a random position of the YouTube video and loop it indefinitely:

* German A1: https://booktube.nosocial.net/?v=tSCrnPUt-pU&sleep=20&loop=true&pos=random&edit=true
* German A2: https://booktube.nosocial.net/?v=esvhhEGLTNA&sleep=20&loop=true&pos=random&edit=true
* German B1: TBD

I recommend that you open the app and play the video in the Firefox browser on your mobile device and listen to it carefully through headphones from the locked screen while walking outside, breathing the fresh air.

_Note: Parts of the code of this project were generated with [Amazon Q Developer](https://aws.amazon.com/q/developer/)._
