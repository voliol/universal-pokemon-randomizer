FORK README (do not merge)

A fork of the Universal Pokemon Randomizer intending to add the following features:  

 * "Preserve Primary Type" (wild) (Implemented with some forbidden combinations)  
     for wild pokemon, ensures the original pokemon's primary type is present in the randomized pokemon. This preserves most type theming, including "soft" theming like forests have Bug and Grass types, while still allowing variety.  

 * "Preserve Type Themed Areas" (wild) (not implemented)  
     for wild pokemon. this is a fairly simple check:  
     if *all* wild pokemon in an area share a single type (whether primary, secondary, or mixed), then randomized pokemon will all have that type.  
     (for this purpose, "an area" means only a single type of encounter, so fishing, surfing, and tall grass would all be different areas.)  
     this would override "preserve primary type" if they are used together.  
     if they share a pair of types, randomized pokemon will have both.  
     *will* trigger if there is only one pokemon in the area. (negotiable, but old rod fishing indicates it should)  
     usable with area 1-to-1, and maybe with random.  
     this is a checkbox.

 * "Preserve Primary Type" (trainer) (Fully Implemented)  
     functions exactly the same as for wild pokemon

 * "Preserve Type Themed Trainers" (trainer) (not implemented)  
     functions the same as "Preserve Type Themed Areas", except the "area" is a single trainer.  
     always triggers if the trainer has only one pokemon. (negotiable, but i think it's more positive than negative.)

 * "Local pokemon only" (implemented for english-language ROMs only)  
     restricts trainers to only using pokemon that can be caught in the wild, or their evolutionary relatives  
     can be used together with type restrictions  

 * Starter type restrictions: (fully implemented)  
     a new radio button set including the following restrictions:  
          * None  
          * Fire, Water, Grass  
          * Any type triangle  
          * All one type: related dropdown, default "Random", contains each type  
     also, checkbox: No dual types
     
 * Other starter restriction: (fully implemented)  
     Random (any basic Pokemon)
