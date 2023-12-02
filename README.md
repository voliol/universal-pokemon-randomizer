FORK README (do not merge)

A fork of the Universal Pokemon Randomizer intending to add the following features:

 * "Preserve Primary Type"  
     mainly for wild pokemon (but could also be an option for trainers), ensures the original pokemon's primary type is present in the randomized pokemon. This preserves most type theming, including soft theming, while still allowing variety.  
     usable with area and global 1-to-1 only.  ...well, actually, i guess it could maybe work with random?  might be hard to implement though.  
     compatible with similar strength with fudging, like type themed trainers, so should be a checkbox i guess.  
     (or move similar strength to a checkbox like it is on trainers, but that's a lot to justify...)
    * Subfeature: "Bird override"  
         makes Flying be considered the primary type for Normal-Flying pokemon.  
         (or, actually, should it be that Normal is never considered the primary type in dual-type pokemon?)  
         probably would not be called this.

 * "Preserve Type Themed Areas"  
     again mainly for wild pokemon. this is a fairly simple check:  
     if *all* wild pokemon in an area share a single type (whether primary, secondary, or mixed), then randomized pokemon will all have that type.  
     (for this purpose, "an area" means only a single type of encounter, so fishing, surfing, and tall grass would all be different areas.)  
     this would override "preserve primary type" if they are used together.  
     usable with area 1-to-1, and maybe with random.
     this is a checkbox.

 * "Superglobal 1-to-1"  
     replaces all instances of a certain pokemon, among wild pokemon OR trainers, with the same pokemon.  
     this would be a setting for trainers  
     (this means that trainer battles will typically show you pokemon that exist in the wild, as in vanilla.)  
     can be overridden for type-themed areas (gyms, pokemon league)
