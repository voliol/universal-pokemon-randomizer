FORK README (do not merge)

A fork of the Universal Pokemon Randomizer intending to add the following features:  

 * "Preserve Primary Type" (wild) (Fully implemented)  
     for wild pokemon, ensures the original pokemon's primary type is present in the randomized pokemon. This preserves most type theming, including "soft" theming like forests have Bug and Grass types, while still allowing variety.  

 * "Preserve Type Themed Areas" (wild) (Fully implemented)  
     for wild pokemon. this is a fairly simple check:  
     if *all* wild pokemon in an area share a single type (whether primary, secondary, or mixed), then randomized pokemon will all have that type.  
     (for this purpose, "an area" means only a single type of encounter, so fishing, surfing, and tall grass would all be different areas.)  
     this would override "preserve primary type" if they are used together.  
     if they share a pair of types, the primary of the first pokemon examined will be used, unless it's Normal.  
     *will* trigger if there is only one pokemon in the area.  
     usable with area 1-to-1 and random, but not global 1-to-1.

 * "Preserve Type Themed Trainers" (trainer) (Fully implemented)  
     functions the same as "Preserve Type Themed Areas", except the "area" is a single trainer.  
     also, if all the pokemon share a pair of types, it will choose the primary type of the trainer's first pokemon - unless it's Normal, in which case it chooses the secondary.
     always triggers if the trainer has only one pokemon.  
     if the trainer is a gym or league trainer, they will be forced to their original theme even if they did not originally strictly follow it (e.g. Pryce using a Seel (Water-type) despite being an Ice trainer)  

* "Preserve Theme Or Primary" (trainer) (not implemented)  
     functions the same as "Preserve Type Themed Trainers", unless the trainer has no theme.  
     In this case, replaces each pokemon with a new pokemon sharing its primary type, just as the wild pokemons' "Preserve Primary Type".  
     (I figured no one would be interested in *only* preserve primary.)

 * "Local pokemon only" (fully implemented)  
     restricts trainers to only using pokemon that can be caught in the wild, or their evolutionary relatives  
     can be used together with type restrictions  
          * Subfeature (not implemented): Allow post-game trainers to use post-game pokemon  
          * sub-subfeature (not implemented): A special case for the Johto games, allowing Kanto trainers to use *only* Kanto pokemon  

 * Starter type restrictions: (fully implemented)  
     a new radio button set including the following restrictions:  
          * None  
          * Fire, Water, Grass  
          * Any type triangle  
          * All one type: related dropdown, default "Random", contains each type  
     also, checkbox: No dual types
     
 * Other starter restriction: (fully implemented)  
     Random (any basic Pokemon)
