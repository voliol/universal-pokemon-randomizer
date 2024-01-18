package test.romhandlers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.ItemList;
import com.dabomstew.pkrandom.pokemon.Shop;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RomHandlerShopTest extends RomHandlerTest {

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void shopItemsAreNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getShopItems().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void shopItemsDoNotChangeWithGetAndSet(String romName) {
        loadROM(romName);
        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        System.out.println(shopItems);
        Map<Integer, Shop> before = new HashMap<>(shopItems);
        romHandler.setShopItems(shopItems);
        System.out.println(romHandler.getShopItems());
        assertEquals(before, romHandler.getShopItems());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void shopItemsCanBeRandomizedAndGetAndSet(String romName) {
        loadROM(romName);
        romHandler.randomizeShopItems(new Settings());
        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        Map<Integer, Shop> before = new HashMap<>(shopItems);
        romHandler.setShopItems(shopItems);
        assertEquals(before, romHandler.getShopItems());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void shopsHaveNames(String romName) {
        loadROM(romName);
        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        for (Shop shop : shopItems.values()) {
            assertNotNull(shop.name);
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void mainGameShopsExist(String romName) {
        loadROM(romName);
        boolean hasMainGameShops = false;
        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        for (Shop shop : shopItems.values()) {
            if (shop.isMainGame) {
                hasMainGameShops = true;
                break;
            }
        }
        System.out.println(shopItems);
        assertTrue(hasMainGameShops);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void regularShopItemsIsNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getRegularShopItems().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void opShopItemsIsNotEmpty(String romName) {
        loadROM(romName);
        assertFalse(romHandler.getOPShopItems().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canBadBadItems(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setBanBadRandomShopItems(true);
        romHandler.randomizeShopItems(s);

        ItemList nonBad = romHandler.getNonBadItems();
        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        for (Shop shop : shopItems.values()) {
            System.out.println(shop);
            for (int itemID : shop.items) {
                assertTrue(nonBad.isAllowed(itemID));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canBadRegularShopItems(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setBanRegularShopItems(true);
        romHandler.randomizeShopItems(s);

        List<Integer> regularShop = romHandler.getRegularShopItems();
        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        for (Shop shop : shopItems.values()) {
            System.out.println(shop);
            for (int itemID : shop.items) {
                assertFalse(regularShop.contains(itemID));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canBanOverpoweredShopItems(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setBanOPShopItems(true);
        romHandler.randomizeShopItems(s);

        List<Integer> opShop = romHandler.getOPShopItems();
        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        for (Shop shop : shopItems.values()) {
            System.out.println(shop);
            for (int itemID : shop.items) {
                assertFalse(opShop.contains(itemID));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canGuaranteeEvolutionItems(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setGuaranteeEvolutionItems(true);
        romHandler.randomizeShopItems(s);

        List<Integer> evoItems = romHandler.getEvolutionItems();
        Map<Integer, Boolean> placed = new HashMap<>();
        for (int evoItem : evoItems) {
            placed.put(evoItem, false);
        }

        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        for (Shop shop : shopItems.values()) {
            System.out.println(shop);
            for (int itemID : shop.items) {
                if (evoItems.contains(itemID)) {
                    placed.put(itemID, true);
                }
            }
        }

        System.out.println(placed);
        int placedCount = (int) placed.values().stream().filter(b -> b).count();
        assertEquals(evoItems.size(), placedCount);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canGuaranteeXItems(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setGuaranteeXItems(true);
        romHandler.randomizeShopItems(s);

        List<Integer> xItems = romHandler.getXItems();
        Map<Integer, Boolean> placed = new HashMap<>();
        for (int xItem : xItems) {
            placed.put(xItem, false);
        }

        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        for (Shop shop : shopItems.values()) {
            System.out.println(shop);
            for (int itemID : shop.items) {
                if (xItems.contains(itemID)) {
                    placed.put(itemID, true);
                }
            }
        }

        System.out.println(placed);
        int placedCount = (int) placed.values().stream().filter(b -> b).count();
        assertEquals(xItems.size(), placedCount);
    }

    @ParameterizedTest
    @MethodSource("getRomNames")
    public void canGuaranteeEvolutionAndXItems(String romName) {
        loadROM(romName);

        Settings s = new Settings();
        s.setGuaranteeEvolutionItems(true);
        s.setGuaranteeXItems(true);
        romHandler.randomizeShopItems(s);

        List<Integer> evoItems = romHandler.getEvolutionItems();
        Map<Integer, Boolean> placedEvo = new HashMap<>();
        for (int evoItem : evoItems) {
            placedEvo.put(evoItem, false);
        }
        List<Integer> xItems = romHandler.getXItems();
        Map<Integer, Boolean> placedX = new HashMap<>();
        for (int xItem : xItems) {
            placedX.put(xItem, false);
        }

        Map<Integer, Shop> shopItems = romHandler.getShopItems();
        for (Shop shop : shopItems.values()) {
            System.out.println(shop);
            for (int itemID : shop.items) {
                if (evoItems.contains(itemID)) {
                    placedEvo.put(itemID, true);
                }
                if (xItems.contains(itemID)) {
                    placedX.put(itemID, true);
                }
            }
        }

        System.out.println("Evo: " + placedEvo);
        System.out.println("X: " + placedX);
        int placedEvoCount = (int) placedEvo.values().stream().filter(b -> b).count();
        assertEquals(evoItems.size(), placedEvoCount);
        int placedXCount = (int) placedX.values().stream().filter(b -> b).count();
        assertEquals(xItems.size(), placedXCount);
    }

}
