Universal Pokemon Randomizer

By Dabomstew, fork by Voliol

Homepage: http://pokehacks.dabomstew.com/randomizer/

**Notice: New binary releases of the randomizer have been indefinitely suspended since 2016. The original repository remains available to distribute source code and facilitiate forks, at https://github.com/Dabomstew/universal-pokemon-randomizer. This fork is something I (Voliol) originally made for personal use, but then figured I might as well put up on the internet. The changes made are described in detail at the end of this file.**

# For Program Users
- by Voliol
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
- by Voliol
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
