FORK README (do not merge)

A fork of the Universal Pokemon Randomizer intending to add the following features:

 * "Preserve Primary Type"
     mainly for wild pokemon (but an option for trainers), ensures the original pokemon's primary type is present in the randomized pokemon. This preserves most type theming while still allowing variety.
    * Subfeature: "Bird override"
         makes Flying be considered the primary type, even when it's the second type listed.
         options to do override either on all Flying, or only Normal-Flying.

 * "Superglobal 1-to-1"
     replaces all instances of a certain pokemon, among wild pokemon OR trainers, with the same pokemon.
     (this means that trainer battles will typically show you pokemon that exist in the wild, as in vanilla.)
     can be overridden for type-themed areas (gyms, pokemon league)
