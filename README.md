Universal Pokemon Randomizer ZX - V branch

**This branch is no longer getting updates. [See UPR FVX for continued development.](https://github.com/upr-fvx/universal-pokemon-randomizer-fvx)**

Continuously based on the UPR ZX, by Ajarmar, with significant contributions from darkeye, cleartonic,
in turn based on the Universal Pokemon Randomizer by Dabomstew

Palette randomization based off work by Artemis251, contains several features from [foxoftheasterisk/UPR-ZX-closer-to-vanilla](https://github.com/foxoftheasterisk/UPR-ZX-closer-to-vanilla).

# Branch Info

This is an alternate branch of the [UPR ZX](https://github.com/Ajarmar/universal-pokemon-randomizer-zx). 
Its goal is to be kept up with the main branch of ZX, while adding features such as palette randomization, 
and a bunch of refactoring for ease-of-development. 

I do not have the tools/computer to be able to handle games past Gen V. For this reason, while later generation support will be merged in from the main branch, I cannot guarantee V branch features will mesh with those generations un-buggily. And features that need generation-specific implementations simply won't make it past Gen V.  

The only _known_ break is with encounters/wild Pokémon in ORAS. Said game had some special handling which had to be taken out when rewriting encounter code, and it has not been put back in.

# Contributing

If you want to contribute something to the codebase, I recommend creating an issue for it first (using the`Contribution Idea` template). This way, we can discuss how to accomplish this, and possibly whether it's a good fit for the randomizer. My plan is to have a more laissez-faire attitute compared to the main ZX branch, so as long as it doesn't mess up the main UI/usability it should be fine. 

[The main branch Wiki Page](https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/Building-Universal-Pokemon-Randomizer-ZX) explains how to set up to build/test locally, with the caveat that the V branch runs off Java 18.

### What is a good fit for the randomizer?

In general, the UPR should have settings as universal as possible (up to Gen V). This means that an idea preferably should work in as many games as possible, and also that it's something that many people will find useful/fun. If the setting is very niche, it will mostly just bloat the GUI.

If your idea is a change to an existing setting rather than a new setting, it needs to be well motivated.

# Feature requests

I gladly take feature requests to know what the user-base wants, but be aware that I am a single person working on this, and will implement them (or not) at my own discretion and pace. 
Features related to other ones in the V branch (like palette randomization) are more likely to get picked up, but the above still applies. If you want to guarantee your feature makes it in, the only way is to pick up Java and code it yourself. It's fun :) 

# Bug reports

If you encounter something that seems to be a bug, submit an issue using the `Bug Report` issue template.

# Other problems

If you have problems using the randomizer, it could be because of some problem with Java or your operating system. **If you have problems with starting the randomizer specifically, [read this page first before creating an issue.](https://github.com/Ajarmar/universal-pokemon-randomizer-zx/wiki/About-Java)** If that page does not solve your problem, submit an issue using the `Need Help` issue template.
