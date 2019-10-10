Universal Pokemon Randomizer

By Dabomstew, fork by Voliol

Homepage: http://pokehacks.dabomstew.com/randomizer/

**Notice: New binary releases of the randomizer have been indefinitely suspended since 2016. The original repository remains available to distribute source code and facilitiate forks, at https://github.com/Dabomstew/universal-pokemon-randomizer. This fork is something I (Voliol) originally made for personal use, but then figured I might as well put up on the internet. The changes made are described in detail at the end of this file.**

# For Program Users
-by Voliol

I would advise you to read this file, especially the "Fair Warning" and 
"Compiling the Program" parts. However, there is also another file detailing the
usage of the program and giving credit to those who originally made it a possibility.
Check it out if you want to.

# Fair Warning
This GitHub repo will have live updates to the randomizer as I program them, but
it won't be at all pretty. Expect lots of spaghetti code and very little use of
best practices. Individual commits may well break randomization, so you might have
to look back in the history a little before you find something usable.

# Compiling the Program
-by Voliol

I do not plan to host any binaries (readymade programs that do not need to be 
compiled before being used) for this fork. This means that if you by chance want 
to try it out you will have to compile it by yourself. You do this by downloading 
a Java interpreter (such as Eclipse https://www.eclipse.org/downloads/),
which you will then have to learn to use. I won't go into details, as they
are sure to change, but it isn't very difficult and there are plenty of
tutorials out there to use. I wish you the best of luck :)

# Forks
Fork as you like, as long as you obey the terms of the included license.

# Pull Requests
-by Voliol

I am yet not very familiar with the nature of GitHub, and exactly how 
Pull Requests work. However, if you do create one I'll look into it and see both
how it works and whether I am interested in such a collaboration. Being part of 
creating the ultimate Randomizer I think would be grand, if such a oppurtunity
arises.

## Differences between this Fork and Dabomstew's Original Version
-by Voliol
 - Added an option to randomize the evolutions directly after the base stat updates, before those other things (base stats, types, abilities) that could be set to follow evolutions. With this, you will get more natural evolutionary lines. 
 - Added an option to prevent random evolutionary lines from converging.
 - Added an option to prevent pokémon from evolving into another with a lower BST.
 - Added an option to forbid lines with split evolutions from being used when choosing "Random (basic Pokémon with 2 evolutions)" as the option in starter randomization.
 - The export log contains a section meant to make it compatible with the 
  emerald-randomizer's family pallete swaps. However, it does not fully work 
  as intended, due to issues with the in-game indexes of gen III Pokémon.

## What is this Branch? ##
-by Voliol
This is a branch where I put various uncompleted features, because honestly, I have too little time and dealing with GUI is a pain.
Feel free to take these features, finish them, and put them in your own fork. Actually, that is encouraged, I would love someone realising the potential of these edits. I've put some time into some of them, after all.
Here's some kind of (poorly formatted) list of the features here that are NOT in my main branch, and also what they are missing. "GUI missing" means that they are not fully implemented into the randomizer's GUI, not that they look bad in-game:

- Set the Gen II Newdex to resemble a "real" Pokédex according to the actual Pokémon found in the game. Respects (random) evolutions, event Pokémon and legendaries. Works with all Generation II games but the Japanese (and Korean) ones. GUI missing, and the encounters are not read in an optimal order (following in-game progression), possibly bugs with unavailable encounters being read. 
The framework for setting the Pokédex order works for multiple (all?) Gens, but implementing it into the game requires different solutions for each Gen, with Gen II being relatively simple, due to having a "regional dex" that fits all possible Pokémon in the game, and no other features that relates to the "regional dex" (like a diploma). 

- Randomize the colors/palettes in Gen II and Gen I (when played on a GBC). GUI missing. Might not fully work on Yellow, have not tested.

- When randomizing evolutions in Emerald, the log contains a section that can be pasted into artemis251's Emerald Randomizer, that can then be used for random palettes following random evolution-lines in Gen III. Has no GUI and would preferably be somehow built-in.

- Some framework +notes added for a feature where stone-evos would get a type according to the stone. It needs to know which item ID's corresponds to which stone.
