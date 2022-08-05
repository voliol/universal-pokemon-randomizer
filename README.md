Universal Pokemon Randomizer

By Dabomstew

Palette branch 
by voliol

# Description of the Palette Branch
What is this: 
A branch for a framework for palette randomization in all Generations (I-V), 
and porting the implementation from Artemis251's Pokemon Emerald Randomizer for Gens III-V. 
Both are mostly finished, and it should be fully usable for all Gen I-III games.
My next step is to attempt a migration to UPR ZX, to be part of the more active development over there.

Feedback is highly appreciated, whether you are well versed in the UPR or not. 
This is also on Github to facilitate collaborations. If you want to implement parts of in your branch,
it has reached a stage where no radical changes should be made; this is a good time.

If you have any questions, I will try my best to answer them. For technical notes, see GRAPHICS_GUIDE.md.
I also have these two guides for explaining how Gen II/III Pokémon palettes work:
https://voliol.neocities.org/articles/genIIpalettes.html
https://voliol.neocities.org/articles/genIIIpalettes.html 

## What it can do right now:


- It can randomize palettes of all vanilla Gen I and Gen II games.  

- It can randomize palettes of all vanilla Gen III games. 
 
- There is a framework to randomize palettes of all Gens IV-V games, but both generations require palette description files, 
  and better support for forms.
  
- All parts of Artemis251's system are implemented, including the "sibling colors".  
  
- Some parts of Artemis251's system have been changed/replaced, most notably colors
  are not dictated by just the basic Pokémon, but later evolutions that gain/lose/change 
  types may have their colors reflect this, as well as their relation to the prevo. 
  Randomized evolution lines are also supported.
  
- Sprites are dumped when you open a rom, so palettes can be viewed quickly. 
  See the Pokemon-sprite-dump folder.
  
- It can read .png/.bmp files and convert them into formats that can be inserted into Gen III-V games.


## What lies further into the future (after a migration to ZX):
 
- Supporting Pokémon forms.

- Palette description files for most games, though I might be "finished" before doing all
of them as it is a lot of manual work. DP is prioritized as I did some of it
during an earlier/unpublished/crappy attempt at porting Artemis251's system. 
Note there is a developer tool for making palette description files. 
 
- Image reading (from file) / writing (to ROM) framework for all Gens. See GRAPHICS_GUIDE.md.

- Documentation and credits could always be improved.

- Various other things to be touched up in the framework. I'm quite new to Java/programming
  at a higher level so many of my TODO's are about best practices/patterns and such.


**The rest of the readme is from the version of the Randomizer this was branched from:**

Homepage: http://pokehacks.dabomstew.com/randomizer/

Forks used -

- 0xhexrobot - https://github.com/0xhexrobot/universal-pokemon-randomizer.git
- Ajarmar - https://github.com/Ajarmar/universal-pokemon-randomizer-zx.git
- Challenert - https://github.com/challenert/universal-pokemon-randomizer
- Hejsil - https://github.com/Hejsil/universal-pokemon-randomizer
- Hwaterke - https://github.com/hwaterke/universal-pokemon-randomizer
- Juanmferreira93 - https://github.com/juanmferreira93/universal-pokemon-randomizer
- Mikabre - https://github.com/mikabre/universal-pokemon-randomizer
- Tj963 - https://github.com/tj963/universal-pokemon-randomizer
- TricksterGuy - https://github.com/TricksterGuy/universal-pokemon-randomizer
- Ttgmichael - https://github.com/ttgmichael/universal-pokemon-randomizer
- Voliol - https://github.com/voliol/universal-pokemon-randomizer

**Notice: New binary releases of the randomizer have been indefinitely suspended since 2016. The original repository remains available to distribute source code and facilitiate forks.**

# For Program Users

If you're looking to actually _use_ the randomizer as opposed to looking at the
source code, you're reading the wrong readme file. This file shouldn't be
included in official releases, but if it is, head on over to UserGuide.txt instead.

I would advise you to read this file, especially the "Fair Warning" and
"Compiling the Program" parts. However, there is also another file detailing the
usage of the program and giving credit to those who originally made it a possibility.
Check it out if you want to.

# Fair Warning

This GitHub repo should not have any automatic updates as it points to the original
(and now archived) website. If Dabomstew ever does resume updates, there is a good
chance that the functionality in here will break and/or be removed.

# Compiling the Program

Binaries are hosted at `https://github.com/brentspector/universal-pokemon-randomizer/releases`

A development guide can be found at [DEVGUIDE.md](./DEVGUIDE.md)

As this is a Maven project, you will need to create a build/run configuration which
accomplishes the equivalent of `mvn clean package`, or just run it from the command
line. This will build a jar for you under the `target` folder which contains any
modifications to the code you have made.

Another option is to build in a docker container. You can build by running
`docker run -it --rm -v "$PWD":/usr/src/mymaven -v "$HOME/.m2":/root/.m2 -w /usr/src/mymaven maven:3.6-jdk-8 mvn clean package`.
This volume maps the current directory and the local maven repository into a docker container
and performs a maven build there. Make sure your current directory is the same location as
`pom.xml` for this command to work. Note that this may make your `target` directory
root-owned and prevent local executions from saving config files. You can run
`sudo chown -R 1000:1000 target` to return ownership to your user. You can also
delete the target folder, which should prompt for elevated permissions before
allowing the folder to be deleted. The folder is rebuilt during `mvn compile`.

This is subject to change. Make sure you're paying attention to which version of Java
(currently this project is set to use Java 8) and Maven (3.6) you're using.

# Contacting Me

Please only contact me with the following

## Bug Reports

A bug report must contain the following

- Game version used
- Options selected
- Expected outcome
- Erroneous outcome

## Feature Requests

A feature request must contain the following

- What game version you're planning to use it on
- What you want in this feature
- If it's similar to any existing feature in the program

## Note

I will try to reply to messages, but do not guarantee a response time.
Additionally, I will decline bugs or features which I feel take too
much time to finish.

# Forks

Fork as you like, as long as you obey the terms of the included license.

# Pull Requests

If you have fixed a bug, feel free to send in a pull request for me to
review. Pull requests will be accepted/denied completely at my own
discretion and may not be responded to in a timely manner if I'm busy.

## Pull Requests that will probably be denied:

- Code that is blatantly stolen from somewhere else without appropriate credit.

PRs will likely be given feedback to address concerns like unit tests,
functionality, and code consistency. Approval is required prior to
a merge being granted so follow the feedback!

# Purpose

This fork was made to compile the changes from other forks into a single fork. This
repository includes a number of changes, and the resulting fixes that were implemented
for conflicts and oversights.
