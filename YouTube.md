# German A1 Hands-Free Trainer   

German A1 Hands-free Trainer narrates phrases from German A1: Start Deutsch 1 word list for the Goethe Insitut exam:

https://www.goethe.de/de/spr/kup/prf/prf/sd1/inf.html

To prepare for the exam, I recommend that you play the video in the Firefox browser on your mobile device and listen to it carefully through headphones from the locked screen while walking outside, breathing the fresh air and keeping your hands free from your phone.

For convenience and for regular training, the video has 7 parts, one for each day of the week:

* 00:00:00 Part 1 (Monday)
* 00:43:59 Part 2 (Tuesday)
* 01:27:44 Part 3 (Wednesday)
* 02:11:29 Part 4 (Thursday)
* 02:55:19 Part 5 (Friday)
* 03:39:10 Part 6 (Saturday)
* 04:22:44 Part 7 (Sunday)

Check the other German trainer videos on my YouTube channel:

* German A2: https://www.youtube.com/watch?v=esvhhEGLTNA
* German B1: TBD


To create these videos, I developed the tool, which works by downloading the "Wortliste" PDF with German words and sample phrases from the Goethe Insitut website, parsing it with Amazon Textract, extracting and translating the phrases from German into English by Amazon Translate and narrating the phrases with Amazon Polly. Finally, it assembles the audio files into a video file and hardcodes subtitles with FFMpeg using the speech marks that Polly produces during speech-to-text conversion.

See the source code of the tool on my GitHub:

https://github.com/ivan-khvostishkov/german-a1-trainer


# German A2 Hands-Free Trainer   

German A2 Hands-free Trainer narrates phrases from German A2 word list for the Goethe Insitut exam:

https://www.goethe.de/de/spr/kup/prf/prf/sd1/inf.html

For convenience and for regular training, visit the radio app that jumps to the random position of the video and loops it indefinitely:

https://german-a2.nosocial.net/ - FIXME: update 

To prepare for the exam, I recommend that you open the app and play the video in the Firefox browser on your mobile device and listen to it carefully through headphones from the locked screen while walking outside, breathing the fresh air.

Check the other German trainer videos on my YouTube channel:

* German A1 (Start Deutsch 1): https://youtu.be/tSCrnPUt-pU
* German B1: TBD

To create these videos, I developed the tool, which works by downloading the "Wortliste" PDF with German words and sample phrases from the Goethe Insitut website, parsing it with Amazon Textract, extracting and translating the phrases from German into English by Amazon Translate and narrating the phrases with Amazon Polly. Finally, it assembles the audio files into a video file and hardcodes subtitles with FFMpeg using the speech marks that Polly produces during speech-to-text conversion.

See the source code of the tool on my GitHub:

https://github.com/ivan-khvostishkov/german-a1-trainer
