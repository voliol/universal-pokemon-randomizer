package com.dabomstew.pkrandom.pokemon;

import java.util.*;

import static com.dabomstew.pkrandom.pokemon.Effectiveness.*;

/**
 * Keeps track of all relevant {@link Type}s and the {@link Effectiveness}-es between them.
 */
public class TypeTable {

    private static final List<Effectiveness> VALID_EFFECTIVENESSES = List.of(Effectiveness.ZERO, HALF,
            NEUTRAL, DOUBLE);

    // Each TypeTable has its own mapping of Types to indexes in the map.
    // This is both so the actual table only has to be types.length^2, instead of [AllTypes]^2,
    // but also to make this class less dependent on the internal structure of the Type class.
    private final List<Type> types;
    private final Effectiveness[][] effectivenesses;
    private final Map<Type, Integer> typeIndexMap;

    /**
     * Creates a new TypeTable.
     * @param types A list of all relevant {@link Type}s.
     */
    public TypeTable(List<Type> types) {
        if (types.stream().distinct().toList().size() != types.size()) {
            throw new IllegalArgumentException("Types must be unique");
        }
        this.types = Collections.unmodifiableList(types);
        this.effectivenesses = new Effectiveness[types.size()][types.size()];
        this.typeIndexMap = new EnumMap<>(Type.class);
        for (int i = 0; i < types.size(); i++) {
            typeIndexMap.put(types.get(i), i);
        }
        for (Effectiveness[] row : effectivenesses) {
            Arrays.fill(row, NEUTRAL);
        }
    }

    public TypeTable(TypeTable original) {
        this.types = new ArrayList<>(original.types);
        this.typeIndexMap = new HashMap<>(original.typeIndexMap);
        this.effectivenesses = new Effectiveness[types.size()][types.size()];
        for (int row = 0; row < types.size(); row++) {
            effectivenesses[row] = Arrays.copyOf(original.effectivenesses[row], types.size());
        }
    }

    /**
     * Creates a new TypeTable using a pre-made Effectiveness[][] table.
     *
     * @param types A list of all relevant {@link Type}s.
     * @param effectivenesses Must have the dimensions ({@code types.length})x({@code types.length}).<br>
     *                        The first index/rows of the table represents the attacker type, and the second
     *                        index/columns represents the defender type. Each row/column follows the same order
     *                        of the types as given in {@code types}. I.e. {@code effectivenesses[i][j]} represents
     *                        the {@link Effectiveness} when a move of the type at index {@code i} is used against a
     *                        Pokemon of the type at index {@code j}.
     */
    private TypeTable(List<Type> types, Effectiveness[][] effectivenesses) {
        this(types);
        checkDimensions(types, effectivenesses);
        for (int attIndex = 0; attIndex < types.size(); attIndex++) {
            for (int defIndex = 0; defIndex < types.size(); defIndex++) {
                Effectiveness eff = effectivenesses[attIndex][defIndex];
                setEffectiveness(types.get(attIndex), types.get(defIndex), eff);
            }
        }
    }

    private static void checkDimensions(List<Type> types, Effectiveness[][] effectivenesses) {
        if (effectivenesses.length != types.size()) {
            throw new IllegalArgumentException("Wrong dimensions of effectivenesses table, should be " +
                    types.size() + "x" + types.size());
        }
        for (Effectiveness[] row : effectivenesses) {
            if (row.length != types.size()) {
                throw new IllegalArgumentException("Wrong dimensions of effectivenesses table, should be " +
                        types.size() + "x" + types.size());
            }
        }
    }

    /**
     * Returns an unmodifiable {@link List} of the types used by the TypeTable.
     */
    public List<Type> getTypes() {
        return types;
    }

    public void setEffectiveness(Type attacker, Type defender, Effectiveness effectiveness) {
        if (!typeIndexMap.containsKey(attacker)) {
            throw new IllegalArgumentException("Type " + attacker + " not supported by this TypeTable.");
        }
        if (!typeIndexMap.containsKey(defender)) {
            throw new IllegalArgumentException("Type " + defender + " not supported by this TypeTable.");
        }
        if (!VALID_EFFECTIVENESSES.contains(effectiveness)) {
            throw new IllegalArgumentException("Invalid Effectiveness: " + effectiveness
                    + " (must be ZERO, HALF, NEUTRAL, or DOUBLE)");
        }
        effectivenesses[typeIndexMap.get(attacker)][typeIndexMap.get(defender)] = effectiveness;
    }

    public Effectiveness getEffectiveness(Type attacker, Type defender) {
        return effectivenesses[typeIndexMap.get(attacker)][typeIndexMap.get(defender)];
    }

    public Map<Type, Effectiveness> against(Type defenderPrimary, Type defenderSecondary) {
        Map<Type, Effectiveness> results = new HashMap<>();
        for (int i = 0; i < types.size(); i++) {
            Effectiveness eff = effectivenesses[i][typeIndexMap.get(defenderPrimary)];
            if (defenderSecondary != null) {
                eff = eff.combine(effectivenesses[i][typeIndexMap.get(defenderSecondary)]);
            }
            results.put(types.get(i), eff);
        }
        return results;
    }

    public List<Type> notVeryEffective(Type attacker) {
        List<Type> results = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            Effectiveness eff = effectivenesses[typeIndexMap.get(attacker)][i];
            if (eff == Effectiveness.ZERO || eff == HALF) {
                results.add(types.get(i));
            }
        }
        return results;
    }

    public List<Type> superEffective(Type attacker) {
        List<Type> results = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            Effectiveness eff = effectivenesses[typeIndexMap.get(attacker)][i];
            if (eff == DOUBLE) {
                results.add(types.get(i));
            }
        }
        return results;
    }

    /**
     * Returns the number of {@link Effectiveness}es in the table, that are not {@link Effectiveness#NEUTRAL}.
     */
    public int nonNeutralEffectivenessCount() {
        int count = 0;
        for (Effectiveness[] row : effectivenesses) {
            for (Effectiveness eff : row) {
                if (eff != NEUTRAL) count++;
            }
        }
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TypeTable other) {
            return equalTypes(other) && equalEffectivenesses(other);
        }
        return false;
    }

    private boolean equalTypes(TypeTable other) {
        return Set.copyOf(other.types).equals(Set.copyOf(types));
    }

    private boolean equalEffectivenesses(TypeTable other) {
        for (Type attacker : types) {
            for (Type defender : types) {
                Effectiveness eff = getEffectiveness(attacker, defender);
                Effectiveness otherEff = other.getEffectiveness(attacker, defender);
                if (eff != otherEff) return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(types);
    }

    @Override
    public String toString() {
        return "TypeTable(#types=" + types.size() + " , #nonNeutral=" + nonNeutralEffectivenessCount() + ", types="
                + types + ")";
    }

    public static void main(String[] args) {
        System.out.println(getVanillaGen6PlusTable().toBigString());
    }

    private static final String[] effectivenessSymbols = new String[] {"Imm", "nve", "---", "SE!"};

    public String toBigString() {
        StringBuilder sb = new StringBuilder();
        int maxNameLength = 0;
        for (Type t : getTypes()) {
            maxNameLength = Math.max(maxNameLength, t.name().length());
        }

        for (int i = 0; i < 3; i+= 3) {
            sb.append(" ".repeat(maxNameLength + 1));
            for (Type defender : types) {
                sb.append(defender.name().length() > i ? defender.name().charAt(i) : " ");
                sb.append(defender.name().length() > i + 1 ? defender.name().charAt(i + 1) : " ");
                sb.append(defender.name().length() > i + 2 ? defender.name().charAt(i + 2) : " ");
                sb.append(" ");
            }
            sb.append("\n");
        }

        for (Type attacker : getTypes()) {
            sb.append(attacker.name());
            sb.append(" ".repeat(maxNameLength - attacker.name().length()));
            for (Type defender : getTypes()) {
                sb.append("|");
                Effectiveness eff = getEffectiveness(attacker, defender);
                sb.append(effectivenessSymbols[eff.ordinal()]);
            }
            sb.append("|");
            sb.append("\n");
        }

        sb.append(effectivenessSymbols[NEUTRAL.ordinal()]);
        sb.append(" = Neutral\n");
        sb.append(effectivenessSymbols[DOUBLE.ordinal()]);
        sb.append(" = Super Effective\n");
        sb.append(effectivenessSymbols[HALF.ordinal()]);
        sb.append(" = Not Very Effective\n");
        sb.append(effectivenessSymbols[ZERO.ordinal()]);
        sb.append(" = Immune\n");
        return sb.toString();
    }

    public static TypeTable getVanillaGen1Table() {
        return new TypeTable(gen1Types, gen1Table);
    }

    public static TypeTable getVanillaGen2To5Table() {
        return new TypeTable(gen2To5Types, gen2To5Table);
    }

    public static TypeTable getVanillaGen6PlusTable() {
        return new TypeTable(gen6PlusTypes, gen6PlusTable);
    }

    private static final List<Type> gen1Types = List.of(
            Type.NORMAL, Type.FIGHTING, Type.FLYING, Type.GRASS, Type.WATER, Type.FIRE, Type.ROCK, Type.GROUND,
            Type.PSYCHIC, Type.BUG, Type.DRAGON, Type.ELECTRIC, Type.GHOST, Type.POISON, Type.ICE
    );
    private static final Effectiveness[][] gen1Table = {
            /*            NORMAL,FIGHTING, FLYING,   GRASS ,   WATER,   FIRE ,   ROCK , GROUND,  PSYCHIC,   BUG  ,  DRAGON,ELECTRIC,   GHOST , POISON,   ICE  */
            /*NORMAL */ {NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    ZERO, NEUTRAL, NEUTRAL},
            /*FIGHTING*/{ DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL,    ZERO,    HALF,  DOUBLE},
            /*FLYING */ {NEUTRAL,  DOUBLE, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL},
            /*GRASS  */ {NEUTRAL, NEUTRAL,    HALF,    HALF,  DOUBLE,    HALF,  DOUBLE,  DOUBLE, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL,    HALF, NEUTRAL},
            /*WATER  */ {NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF,  DOUBLE,  DOUBLE,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL},
            /*FIRE   */ {NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE,    HALF,    HALF,    HALF, NEUTRAL, NEUTRAL,  DOUBLE,    HALF, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE},
            /*ROCK   */ {NEUTRAL,    HALF,  DOUBLE, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE},
            /*GROUND */ {NEUTRAL, NEUTRAL,    ZERO,    HALF, NEUTRAL,  DOUBLE,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL,  DOUBLE, NEUTRAL,  DOUBLE, NEUTRAL},
            /*PSYCHIC*/ {NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL},
            /*BUG    */ {NEUTRAL,    HALF,    HALF,  DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,  DOUBLE, NEUTRAL},
            /*DRAGON */ {NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL},
            /*ELECTRIC*/{NEUTRAL, NEUTRAL,  DOUBLE,    HALF,  DOUBLE, NEUTRAL, NEUTRAL,    ZERO, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL, NEUTRAL},
            /*GHOST  */ {   ZERO, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    ZERO, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL},
            /*POISON */ {NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL},
            /*ICE    */ {NEUTRAL, NEUTRAL,  DOUBLE,  DOUBLE,    HALF, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF},
    };

    private static final List<Type> gen2To5Types = List.of(
            Type.NORMAL, Type.FIGHTING, Type.FLYING, Type.GRASS, Type.WATER, Type.FIRE, Type.ROCK, Type.GROUND,
            Type.PSYCHIC, Type.BUG, Type.DRAGON, Type.ELECTRIC, Type.GHOST, Type.POISON, Type.ICE, Type.STEEL, Type.DARK
    );
    private static final Effectiveness[][] gen2To5Table = {
        /*            NORMAL,FIGHTING, FLYING,   GRASS ,   WATER,   FIRE ,   ROCK , GROUND,  PSYCHIC,   BUG  ,  DRAGON,ELECTRIC,   GHOST , POISON,   ICE  ,  STEEL ,  DARK  */
        /*NORMAL */ {NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    ZERO, NEUTRAL, NEUTRAL,    HALF, NEUTRAL},
        /*FIGHTING*/{ DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL,    ZERO,    HALF,  DOUBLE,  DOUBLE,  DOUBLE},
        /*FLYING */ {NEUTRAL,  DOUBLE, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL},
        /*GRASS  */ {NEUTRAL, NEUTRAL,    HALF,    HALF,  DOUBLE,    HALF,  DOUBLE,  DOUBLE, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL,    HALF, NEUTRAL,    HALF, NEUTRAL},
        /*WATER  */ {NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF,  DOUBLE,  DOUBLE,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL},
        /*FIRE   */ {NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE,    HALF,    HALF,    HALF, NEUTRAL, NEUTRAL,  DOUBLE,    HALF, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE,  DOUBLE, NEUTRAL},
        /*ROCK   */ {NEUTRAL,    HALF,  DOUBLE, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE,    HALF, NEUTRAL},
        /*GROUND */ {NEUTRAL, NEUTRAL,    ZERO,    HALF, NEUTRAL,  DOUBLE,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL,  DOUBLE, NEUTRAL,  DOUBLE, NEUTRAL,  DOUBLE, NEUTRAL},
        /*PSYCHIC*/ {NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF,    ZERO},
        /*BUG    */ {NEUTRAL,    HALF,    HALF,  DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL,    HALF,  DOUBLE},
        /*DRAGON */ {NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL},
        /*ELECTRIC*/{NEUTRAL, NEUTRAL,  DOUBLE,    HALF,  DOUBLE, NEUTRAL, NEUTRAL,    ZERO, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL},
        /*GHOST  */ {   ZERO, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF,    HALF},
        /*POISON */ {NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL,    ZERO, NEUTRAL},
        /*ICE    */ {NEUTRAL, NEUTRAL,  DOUBLE,  DOUBLE,    HALF,    HALF, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL},
        /*STEEL  */ {NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL,  DOUBLE,    HALF, NEUTRAL},
        /*DARK   */ {NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF,    HALF},
    };

    private static final List<Type> gen6PlusTypes = List.of(
            Type.NORMAL, Type.FIGHTING, Type.FLYING, Type.GRASS, Type.WATER, Type.FIRE, Type.ROCK, Type.GROUND,
            Type.PSYCHIC, Type.BUG, Type.DRAGON, Type.ELECTRIC, Type.GHOST, Type.POISON, Type.ICE, Type.STEEL,
            Type.DARK, Type.FAIRY
    );
    private static final Effectiveness[][] gen6PlusTable = {
            /*            NORMAL,FIGHTING, FLYING,   GRASS ,   WATER,   FIRE ,   ROCK , GROUND,  PSYCHIC,   BUG  ,  DRAGON,ELECTRIC,   GHOST , POISON,   ICE  ,  STEEL ,  DARK  , FAIRY */
            /*NORMAL */ {NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    ZERO, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL},
            /*FIGHTING*/{ DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL,    ZERO,    HALF,  DOUBLE,  DOUBLE,  DOUBLE,    HALF},
            /*FLYING */ {NEUTRAL,  DOUBLE, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL},
            /*GRASS  */ {NEUTRAL, NEUTRAL,    HALF,    HALF,  DOUBLE,    HALF,  DOUBLE,  DOUBLE, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL,    HALF, NEUTRAL,    HALF, NEUTRAL, NEUTRAL},
            /*WATER  */ {NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF,  DOUBLE,  DOUBLE,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL},
            /*FIRE   */ {NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE,    HALF,    HALF,    HALF, NEUTRAL, NEUTRAL,  DOUBLE,    HALF, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE,  DOUBLE, NEUTRAL, NEUTRAL},
            /*ROCK   */ {NEUTRAL,    HALF,  DOUBLE, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE,    HALF, NEUTRAL, NEUTRAL},
            /*GROUND */ {NEUTRAL, NEUTRAL,    ZERO,    HALF, NEUTRAL,  DOUBLE,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL,  DOUBLE, NEUTRAL,  DOUBLE, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL},
            /*PSYCHIC*/ {NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL,    HALF,    ZERO, NEUTRAL},
            /*BUG    */ {NEUTRAL,    HALF,    HALF,  DOUBLE, NEUTRAL,    HALF, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL,    HALF,  DOUBLE,    HALF},
            /*DRAGON */ {NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL,    ZERO},
            /*ELECTRIC*/{NEUTRAL, NEUTRAL,  DOUBLE,    HALF,  DOUBLE, NEUTRAL, NEUTRAL,    ZERO, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL},
            /*GHOST  */ {   ZERO, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL},
            /*POISON */ {NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL,    ZERO, NEUTRAL,  DOUBLE},
            /*ICE    */ {NEUTRAL, NEUTRAL,  DOUBLE,  DOUBLE,    HALF,    HALF, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF, NEUTRAL, NEUTRAL},
            /*STEEL  */ {NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL,  DOUBLE,    HALF, NEUTRAL,  DOUBLE},
            /*DARK   */ {NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF,    HALF},
            /*FAIRY  */ {NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL, NEUTRAL,    HALF, NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL,  DOUBLE, NEUTRAL, NEUTRAL,    HALF, NEUTRAL,    HALF,  DOUBLE, NEUTRAL},
    };


}
