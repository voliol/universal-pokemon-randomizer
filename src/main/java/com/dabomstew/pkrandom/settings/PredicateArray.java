package com.dabomstew.pkrandom.settings;

/*----------------------------------------------------------------------------*/
/*--  PredicateArray.java - represents an array of PredicatePairs that form --*/
/*--                        a logical AND situation between two or more     --*/
/*--                        PredicatePair objects.                          --*/
/*--                        Enables a child SettingsOption object to define --*/
/*--                        when its state is allowed to be changed         --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

public class PredicateArray {
    private ArrayList<PredicatePair> predicateArray;
    private HashMap<SettingsOption, Boolean> predicatePairResults;

    public PredicateArray(PredicatePair[] predicates) {
        this.predicateArray = new ArrayList<PredicatePair>(Arrays.asList(predicates));
        predicatePairResults = new HashMap<SettingsOption, Boolean>();
    }

    public boolean isValueRandomizable(SettingsOption item) {
        // See if item is in the predicateArray and then set the result
        Optional<PredicatePair> match =
                predicateArray.stream().filter(p -> p.getParent() == item).findFirst();

        if (match.isPresent()) {
            predicatePairResults.put(item, match.get().test(item));
        }

        // Cannot randomize until all parents have run and all results are true
        if (predicatePairResults.size() == predicateArray.size()) {
            return predicatePairResults.values().stream().allMatch(v -> v);
        }

        // Return false if all conditions fail
        return false;
    }

    public void setChildren(SettingsOption item) {
        predicateArray.forEach(p -> p.getParent().add(item));
    }
}
