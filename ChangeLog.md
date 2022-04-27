# Differences between this Fork and Dabomstew's Original Version

## 1.8.0

- Converted project into Maven and brought minimum java version to 8.
- Added an option to randomize the stats and types before or after any evolution randomization is applied. With this, you can get more natural evolutionary lines, or maintain the old method of randomization.
- Added an option to prevent random evolutionary lines from converging (2 pokemon evolve into the same thing).
- Added an option to prevent pokémon from evolving into another with a lower BST.
- Added an option to forbid lines with split evolutions from being used when randomizing starters.
- Added an option for randomizing outside of any BST cap.
- Added an option for fast text.
- Refactored "Guaranteed Four Moves" to work on a slider of 2-4.
- Eevee now evolves into different typed pokemon instead of sharing types (when evolutions are randomized).
- Tweaked moveset randomization to better balance types in movesets.
- Added an option to shuffle base stats between pokemon.
- Enabled "No Broken Moves" to be used in non-random builds.
- Refactored "Modify Levels" to work on non-random trainers.
- Refactored "Force Fully Evolved" to work on non-random trainers.
- Added an option to randomize starters with 1 or 2 evolutions.
- Added an option to balance wild type frequency.
- Added an option to have unique starter types when randomized.
- Added an option for evolved pokemon to appear at lower levels.
- Logs are now in HTML format and contain CSS formatting to enhance readability.

## 1.8.1

- Gen 3 - Wally is now a rival which can carry a starter or team.
- Rivals and Friends will no longer have duplicate team members. (the only exception is when a team is carried and a pre-evo is used)
- Rivals and Friends can carry their team in addition to their starter.
- Pokemon with stats above 190 or BST above 590 are now tagged as `BIG` in logs.
- Moves with power above 95 are now tagged as `BIG` in the logs.
- Adjusted how HP is randomized to avoid excessive skewing, leading to more stats in other areas (minimum of 35 instead of 20 and reduced HP growth by 10%).
- Pokemon types can now be shuffled on a 1-for-1 basis. Does not affect moves.
- Pokemon types can be randomized for only 1 type (2nd type is preserved).
- Split evolutions are no longer excluded from inheriting base form types.
- Eeveelutions are guaranteed to have different types when types are randomized.
- Base stats can now be shuffled between pokemon. Only applies to similar power levels.
- Base stats can also be shuffled in order and then between pokemon. Same as above.
- Starter pokemon can now be limited based on their BST.
- Starter pokemon can now be limited to just base forms.
- Updated logs to show old and new trainer teams for comparison.
- Updated trade logging to increase readability.
- Added section to show new evolution chains through the 3rd form.
- Added seed and settings to log (as opposed to just at the end of randomization).

## 1.9.0

- When random retaining types with follow evolutions checked, Pokemon with two types evolving into Pokemon with one type now passes the new type instead of retaining the shared type. Fixes issue where Weepinbell could evolve into Snorlax and not share any types.
- Added code tests to ensure new features do not break existing features
  - Settings are now tested using a robot framework from AssertJ Swing
  - Functions in RomHandler are still conducted with JUnit
- Pokemon with BST above 490 are now tagged as BIG in the logs when randomizing a Gen 1 game due to there only being 5 stats instead of the usual 6 for Gen 2 onwards.
- Log generation has been consolidated into one area of the code. This should make future log enhancements easier, as well as improve code readability.
- Table of Contents added to top of log to jump to section of interest
- Gen 3 and Gen 4 Feebas can now evolve by happiness when removing impossible evolutions
- Gen 5 and Gen 6 move updates can be applied independently of one another
- Black/White Cilan, Chili, Cress now have types that are superior to the player's chosen starter
  - As an additional option to the previous change, players can opt for a defensively resistant opponent. For instance, Magnemite would normally face a Ground type, but this option makes the Gym use an Electric type as Electric resists both Electric and Steel.
- Modified the following sections of Gen 5 Black/White in-game text to match choices
  - Striaton City Gym leaders will use their true types when using type-theming on Gym Leaders, or use generic terms when completely random
  - Nacrene City Gym Guy will use true type and appropriate weakness when using type-theming on Gym Leaders, or use generic terms when completely random
  - Castelia City item guy will use correct starter names when giving type-boosting items. Items will not match the types of the new starters
  - Castelia City cafe guy will use correct types when referring to Striaton City leaders, or use generic terms when completely random
  - Castelia City Gym Trainers and Burgh will use the true type of the gym when type-theming, or use generic terms when completely random
  - Cheren will use Burgh's true type when referring to him, or use generic terms when completely random
  - Nimbasa City Gym Guy will use true type and appropriate weakness when using type-theming on Gym Leaders, or use generic terms when completely random
  - Professor Juniper will use Elesa's true type when speaking, or use generic terms when completely random
  - Driftveil City Gym Guy will use Clay's true type and appropriate weakness when using type-theming on Gym Leaders, or use generic terms when completely random
  - Mistralton City Gym Guy will use Skyla's true type and appropriate weakness when using type-theming on Gym Leaders, or use generic terms when completely random
  - Monkey giver in Striaton City will use correct types of Pokemon and a relevant strength when randomizing static Pokemon
  - Gorm in Pinwheel Forest will use correct type for Burgh and Lenora when type-theming on Gym Leaders, or generic terms when completely random
  - Skyla in Celestial Tower will use her true type when using type-theming on Gym Leaders, or use generic terms when completely random
- Added `Normalize`, `Klutz`, `Multitype` and `Stall` to list of negative abilities that can be banned
- Static Pokemon are randomized before trainers. Should have no noticable impact
- Pansear, Pansage, and Panpour (elemental monkies) now are replaced with a type that trumps Striaton gym if type-theming on Gym Leaders, or covers a random weakness of the selected starter
- Evolution Methods can now be randomized
  - In order to make this compatible with Metronome Only mode, all pokemon that evolve due to knowing a move (e.g. Bonsly) now require Metronome regardless of whether evolution methods are randomized
  - Fixing impossible evos occurs after movesets are randomized, and selects a new move from the randomzied moveset to replace the current one used when evolving while knowing a move regardless of whether evolution methods are randomized
  - Evolutions like Nincada and Karrablast are excluded due to lack of support in game for randomization of these methods
- Starter Pokemon selection has been condensed to use a slider to select whether the starters have at least 1, 2, or any amount of evolutions
  - Added a checkbox to make starters have that exact count.
  - This removes the ability to have 1 or 2 evo evolutions, but can be achieved by selecting "Limit Evolutions to Three Stages" when randomizing evolutions and selecting option 1 on the slider.
- Fix Starters No Split to only activate when Random Starters is selected
  - There was a bug discovered in version 1.81 where the values of Starter No Split and Starter Unique Types were restored in the wrong order. This has been corrected and should allow the settings file to be used as expected.
- Trainer pokemon held items can be randomized for Generation 3 or higher. Items are restricted to battle tower viable.
- Gyms and Elite 4 can be given a type theme when chosing Random for trainer pokemon
- A new `Randomize Settings` button has been added. This will randomly select settings available for the game (randomize the randomizer). You will still be required to click `Randomize (Save)` to confirm the settings and generate the ROM.
- Fix Minimum Catch Rate checkbox to be selectable regardless of what the Wild Pokemon radio button is set to
- Starters can now be required to be a Super Effective triangle
  - For instance, the traditional triangle is grass-fire-water, but this could make it electric-ground-water or flying-rock-fighting
  - While this generally creates a perfect triangle, there are edge cases that were deliberately skipped to save runtime. Better chances can be obtained by selecting "Unique Types" in addition to "SE Triangle"
- Trainers can now have pokemon swapped in a global 1 for 1, similar to the global mapping option of Wild Pokemon
  - Gym type theming is respected by prioritizing type consistency over global swap mapping. This should create a fresher game experience by making route trainers adhere to global swap rules while making gyms, elite 4, champion, and other themed teams stick to one type.
- Fix Wild Global 1-to-1 replacement erroring when restricting generation pokemon to correctly use all pokemon as replacable rather than just those that are allowed
- Starter Pokemon can now be filtered to only allow certain types (particularly useful for monoruns)
- Elite 4, Champion, and Uber class trainers can be buffed to only use Legendary and BIG Pokemon (BIG is 590+ BST (490+ Gen1) or 190+ in one stat)
  - Generally not advised until Gen 3 since the pool of Pokemon will be really small (7 in Gen 1, 16 Gen 2, 30+ Gen 3 and higher)
  - This also works with type theming (global or gym) and global swap

## 1.9.1

- Fix Randomize Movesets being stuck on UNCHANGED
- Hidden Hallows/Hollows have been renamed to Hidden Grottos
- Fix SE Triangle not restoring state to GUI on settings load
  - Also fix UI unit tests to verify that the UI state is also updated on reload instead of just the settings being valid
- Fix Gen 3 Hacks to be more or less supported
  - When new types are added (like Fairy) it will be given the HACK type. If more than 1 type is added, then all new types will be set to HACK, and in-game will be treated as 1 of the new types
  - When new abilities are added, log generation will state the ability as `-----`. Randomizing abilities will only use abilities from the vanilla game
  - Randomizing types with "follow evolutions" will do whatever it can, but some data is not aligned so types may just be random instead
  - Randomizing evolutions may fail due to data not being aligned
  - Starter pokemon options will do whatever it can, but some data is not aligned and options may appear to be non-functional
  - Randomizing moves will probably work, but log generation will not provide any move name details beyond those from Gen 5. Hack types are shown as `???`
  - Similarly, randomizing movesets will probably work, but log generation will not provide any move name details beyond those from Gen 5
  - Trainer randomization will don whatever it can, but some data is not aligned and options may appear to be non-functional
  - Wild pokemon appears to be supported (please report bugs if found)
  - Static pokemon are generally NOT supported
  - Similar to move randomization, TM and Move Tutor randomization will probably work, but log generation will not provide any move name details beyond those from Gen 5.
- Game breaking moves are no longer shown as being removed in the log if that option was not set
- Fix rival selecting weaker starter when using SE Triangle
  - A side-effect of this is that the valid pool can be drained a lot quicker. This will throw "Exhausted starter pool" errors a lot more frequently.
- Fix deprecated DatatypeConverter for Base64 to allow compatability with Java 8 and newer Java versions

## 1.10.0

- UI layout has been moved around for easier development
- All pokemon can evolve at level 1
  - Gen 2 will likely fail due to restrictions on data block size.
  - If `Change Methods` is chosen, all pokemon will evolve with a random method and level evolutions will NOT be at 1
  - If `Random Starters` is chosen, slider will be set to 0 and `Exact Evos` will be false as there is no end to an evolution chain
  - Evolution paths in the log will only display pokemon where nothing evolves into them
  - If `Follow Evolutions` is chosen, no guarantee is made to linear behavior. This will result in stat drops, type disparity, and moveset oddities.
  - Selecting `Standardize EXP Curves` will result it better selections. However, combining this with `Filter Legendaries` will result in failure as legendaries will have no valid selections to evolve into due to mandatory EXP curve requirements.
- `Force Fully Evolved` and `Modify Trainer Level` are available to use regardless of trainer option selected
- Rotom has been removed from the legendary pool list
- Platinum rival at Battle Tower now correctly retains starter or team if that option is selected
- `Allow Low Level Evolution` wild pokemon setting now correctly toggles behavior in the randomizer
- `LEVEL_ITEM_DAY`, `LEVEL_ITEM_NIGHT`, and `TRADE_ITEM` should correctly avoid duplicates. _should_
- Subsets of pokemon can now be randomized instead of everything in a ROM. Details can be found in "UserGuide.txt".
  - `LimitPokemon` now restricts the pool of pokemon eligible for randomization to align with this new subset feature.
  - Logs will still show all pokemon, even if they weren't randomized, to give a complete game guide.
- When randomizing evolutions, you can restrict replacements to the same stage that the evolution is.
  - The main filter for evolution replacement in general is experience curve. Attempting to filter by same stage and same type can result in no change (such as the Abra line). Choosing `Force change` on top of this will result in type or stage restrictions potentially being skipped, leading to odd selections for evolutions (Kadabra can only evolve into Celebi or Mew). Selecting `Standardize exp curves` will help in widening the available pool.
- When randomizing evolutions, you can ban legendaries as an option
- HGSS can now randomize headbutt trees
  - Credit to [dee-ee](https://www.reddit.com/user/dee-ee/) for pointing me [to the code](https://www.reddit.com/r/PokemonROMhacks/comments/n0xsrl/comment/gz8evmb/?utm_source=share&utm_medium=web2x&context=3)
- Wild encounters are now sorted alphabetically
- HGSS wild encounter logs now have background colors for Swarms and Radio encounters
  - If any other logs are missing colors, please let me know!
- Logs now have Dark Mode
  - Will start in Light Mode unless browser/system settings are configured for dark mode in a way "matchMedia" can read from
- Fixes potential error with `LimitPokemon` where only 1 related pokemon was included in the batch, rather than all in the evolution line that met the criteria
- Adds more specific headers in logs for Starter randomization
- Type effectiveness can now be used in all generations
  - This only removes Steel resistance to Ghost and Dark for Gen 2+, and does not add the Fairy type
  - Credit to [dee-ee](https://www.reddit.com/user/dee-ee/) for pointing me [to the code](https://www.reddit.com/r/PokemonROMhacks/comments/n0xsrl/version_190_of_universal_pokemon_randomizer/h54tvei?utm_source=share&utm_medium=web2x&context=3)
- Type effectiveness is now based on the data in the ROM
  - A Gen 8 effectiveness table (minus FAIRY) is provided if game data is not found.
  - The type chart in game is printed as part of the logs if game data is found.
  - This has not been tested thoroughly, and it is reasonable to assume some hacks may have types improperly read. Please let me know if you find an example!
- Adds more support for Randomizable 809
  - Adds unique entry to allow FAIRY type to be properly recognized
  - This also enables other rom hacks to be easily supported, so long as the offsets are known and a unique identifier exists
  - Added support for evolutions. Credit to [ajarmar](https://github.com/Ajarmar) for [evolution type code](https://github.com/Ajarmar/universal-pokemon-randomizer-zx/blob/master/src/com/dabomstew/pkrandom/pokemon/EvolutionType.java)
- Logs are now handled in a separate class. There should be no impact to users.
- SE Triangle algorithm has been redone. It should be more consistent and throw fewer errors.
- Fixed bug where `Random (retain one)` type randomization would hit an infinite loop
- Fixed bug in Platinum where Giratina could not be replaced when limiting generations

## 1.10.1

- Add support for Renegade Platinum and Following Platinum
  - Static pokemon are not supported
  - Following Renegade Platinum is not supported due to misalignment between expected offsets
- Fix bug for Randomizable 809 where FAIRY type was incorrectly mapped as HACK type for type shuffling
- Added Rom Hack checking for Platinum, Soul Silver, Heart Gold, Black, White, Black 2, White 2
- Added basic Fire Red hack support structures for Gen 4 and Gen 5
  - Unfortunately, due to bad NDS ROM writing, Gen 4 and Gen 5 hacks are largely unsupported. Feel free to try though!

## 1.10.2

- Fix Soul Silver checksum to match a more common version
- Update type theming for trainers to use new PokemonCollection from 1.10.0
  - This should now properly support new move types that have no corresponding Pokemon (like Curse)

## 1.10.3

- Add "Disable ROM Hack" checkbox to treat a ROM as a base game
  - No guarantee is made that the ROM will be functional. This is meant to prevent normal ROMs from being feature blocked